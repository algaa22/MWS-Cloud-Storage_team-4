package com.mipt.team4.cloud_storage_backend.netty.handlers.metrics;

import com.mipt.team4.cloud_storage_backend.netty.constants.NettyMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.util.AttributeKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@Sharable
@RequiredArgsConstructor
public class MetricsHandler extends ChannelDuplexHandler {
  private final MeterRegistry meterRegistry;

  private static final AttributeKey<Timer.Sample> TIMER_SAMPLE_KEY =
      AttributeKey.valueOf("metrics.timer_sample");
  private static final AttributeKey<String> URI_KEY = AttributeKey.valueOf("metrics.uri");
  private static final AttributeKey<String> METHOD_KEY = AttributeKey.valueOf("metrics.method");

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    if (msg instanceof HttpRequest request) {
      Timer.Sample sample = Timer.start(meterRegistry);
      ctx.channel().attr(TIMER_SAMPLE_KEY).set(sample);

      ctx.channel().attr(URI_KEY).set(request.uri());
      ctx.channel().attr(METHOD_KEY).set(request.method().name());
    }

    ctx.fireChannelRead(msg);
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
      throws Exception {
    if (msg instanceof HttpResponse response) {
      promise.addListener(
          future -> {
            if (future.isSuccess()) {
              recordMetrics(ctx, response.status().code());
            }
          });
    }

    super.write(ctx, msg, promise);
  }

  private void recordMetrics(ChannelHandlerContext ctx, int status) {
    Timer.Sample sample = ctx.channel().attr(TIMER_SAMPLE_KEY).getAndSet(null);

    if (sample != null) {
      String uri = ctx.channel().attr(URI_KEY).get();
      String method = ctx.channel().attr(METHOD_KEY).get();

      Timer.Builder builder =
          Timer.builder(NettyMetrics.HTTP_SERVER_REQUESTS)
              .tag("method", method)
              .tag("uri", uri)
              .tag("status", String.valueOf(status))
              .description("HTTP Request Latency");

      sample.stop(builder.register(meterRegistry));
    }
  }
}
