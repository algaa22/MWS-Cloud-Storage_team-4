package com.mipt.team4.cloud_storage_backend.netty.handlers.rest;

import com.mipt.team4.cloud_storage_backend.config.props.StorageConfig;
import com.mipt.team4.cloud_storage_backend.exception.netty.NotHttpRequestException;
import com.mipt.team4.cloud_storage_backend.exception.validation.ParseException;
import com.mipt.team4.cloud_storage_backend.netty.constants.ApiEndpoints;
import com.mipt.team4.cloud_storage_backend.netty.constants.PipelineHandlerNames;
import com.mipt.team4.cloud_storage_backend.netty.handlers.validation.GlobalValidationHandler;
import com.mipt.team4.cloud_storage_backend.netty.mapping.codec.RequestToDtoDecoder;
import com.mipt.team4.cloud_storage_backend.netty.utils.RequestUtils;
import com.mipt.team4.cloud_storage_backend.utils.SafeParser;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.ReferenceCountUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Динамический селектор стратегии обработки трафика.
 *
 * <p>Класс анализирует метаданные входящего HTTP-запроса и на лету перестраивает {@link
 * ChannelPipeline}, переключаясь между агрегированной (в памяти) и потоковой (chunked) обработкой
 * данных. <b>Логика выбора:</b>
 *
 * <ul>
 *   <li>{@link PipelineType#CHUNKED}: Используется для загрузки файлов, превышающих лимит {@code
 *       maxAggregatedContentLength}, и для скачивания файлов. Позволяет работать с большими данными
 *       без риска OutOfMemory.
 *   <li>{@link PipelineType#AGGREGATED}: Используется для стандартных API-запросов. Весь запрос
 *       собирается в один FullHttpRequest для удобства обработки.
 * </ul>
 *
 * <b>Важные нюансы реализации:</b>
 *
 * <ul>
 *   <li>Имеет состояние (поле {@code previousPipeline}), поэтому помечен как
 *       {@code @Scope("prototype")}. Для каждого нового соединения создается свой экземпляр.
 *   <li>При переключении стратегий корректно удаляет старые хендлеры, предотвращая дублирование или
 *       конфликты в пайплайне.
 *   <li>Обеспечивает ручное управление счетчиком ссылок (Reference Counting) при возникновении
 *       исключений, чтобы избежать утечек в Direct Memory.
 * </ul>
 */
@Component
@Scope("prototype")
@Slf4j
@RequiredArgsConstructor
public class HttpTrafficStrategySelector extends ChannelInboundHandlerAdapter {
  private final ObjectProvider<ChunkedHttpHandler> chunkedHttpHandlers;
  private final AggregatedHttpHandler aggregatedHttpHandlerProvider;
  private final GlobalValidationHandler globalValidationHandler;
  private final RequestToDtoDecoder requestToDtoDecoder;
  private final StorageConfig storageConfig;

  private PipelineType previousPipeline = null;

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (!(msg instanceof HttpObject)) {
      ReferenceCountUtil.release(msg);
      throw new NotHttpRequestException();
    }

    if (msg instanceof HttpRequest request) {
      PipelineType currentPipeline;

      try {
        currentPipeline =
            PipelineType.from(request, storageConfig.rest().maxAggregatedContentLength());
      } catch (ParseException e) {
        ReferenceCountUtil.release(msg);
        throw e;
      }

      if (previousPipeline != currentPipeline) {
        cleanupPipeline(ctx);
        configurePipeline(currentPipeline, ctx);

        previousPipeline = currentPipeline;
      }
    }

    super.channelRead(ctx, msg);
  }

  private void cleanupPipeline(ChannelHandlerContext ctx) {
    ChannelPipeline pipeline = ctx.pipeline();

    if (previousPipeline == PipelineType.CHUNKED) {
      safeRemoveFromPipeline(pipeline, ChunkedWriteHandler.class);
      safeRemoveFromPipeline(pipeline, ChunkedHttpHandler.class);
    }

    if (previousPipeline == PipelineType.AGGREGATED) {
      safeRemoveFromPipeline(pipeline, HttpObjectAggregator.class);
      safeRemoveFromPipeline(pipeline, AggregatedHttpHandler.class);
    }
  }

  private void configurePipeline(PipelineType currentPipeline, ChannelHandlerContext ctx) {
    ChannelPipeline pipeline = ctx.pipeline();

    if (currentPipeline == PipelineType.CHUNKED) {
      addHandlerToPipeline(pipeline, PipelineHandlerNames.CHUNKED_WRITE, new ChunkedWriteHandler());
      addInitialRestHandlers(pipeline);
      addHandlerToPipeline(
          pipeline, PipelineHandlerNames.CHUNKED_HTTP, chunkedHttpHandlers.getObject());
    } else {
      addHandlerToPipeline(
          pipeline,
          PipelineHandlerNames.HTTP_OBJECT_AGGREGATOR,
          new HttpObjectAggregator(storageConfig.rest().maxAggregatedContentLength()));
      addInitialRestHandlers(pipeline);
      addHandlerToPipeline(
          pipeline, PipelineHandlerNames.AGGREGATED_HTTP, aggregatedHttpHandlerProvider);
    }
  }

  private void addInitialRestHandlers(ChannelPipeline pipeline) {
    addHandlerToPipeline(pipeline, PipelineHandlerNames.REQUEST_TO_DTO, requestToDtoDecoder);
    addHandlerToPipeline(pipeline, PipelineHandlerNames.GLOBAL_VALIDATION, globalValidationHandler);
  }

  private void addHandlerToPipeline(ChannelPipeline pipeline, String name, ChannelHandler handler) {
    pipeline.addBefore(PipelineHandlerNames.STORAGE_EXCEPTION, name, handler);
  }

  private void safeRemoveFromPipeline(
      ChannelPipeline pipeline, Class<? extends ChannelHandler> handlerClass) {
    if (pipeline.get(handlerClass) != null) {
      pipeline.remove(handlerClass);
    }
  }

  public enum PipelineType {
    CHUNKED,
    AGGREGATED;

    public static PipelineType from(HttpRequest request, int maxAggregatedContentLength)
        throws ParseException {
      if (request.method() == HttpMethod.POST) {
        int fileSize =
            SafeParser.parseInt("File size", RequestUtils.getHeader(request, "X-File-Size", "0"));

        if (fileSize > maxAggregatedContentLength) {
          return CHUNKED;
        }
      }

      if (request.uri().startsWith(ApiEndpoints.FILES_CHUNKED_UPLOAD)
          || request.uri().startsWith(ApiEndpoints.FILES_CHUNKED_UPLOAD_RESUME)
          || request.uri().startsWith(ApiEndpoints.FILES_DOWNLOAD)) {
        return CHUNKED;
      }

      return AGGREGATED;
    }
  }
}
