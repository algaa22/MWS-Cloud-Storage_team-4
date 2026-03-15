package com.mipt.team4.cloud_storage_backend.netty.mapping;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mipt.team4.cloud_storage_backend.exception.mapping.CreateDtoInstanceException;
import com.mipt.team4.cloud_storage_backend.exception.mapping.ParseJsonParamException;
import com.mipt.team4.cloud_storage_backend.exception.mapping.ReadJsonBodyException;
import com.mipt.team4.cloud_storage_backend.exception.user.auth.MissingAuthTokenException;
import com.mipt.team4.cloud_storage_backend.exception.utils.MissingRequiredParamException;
import com.mipt.team4.cloud_storage_backend.netty.constants.SecurityAttributes;
import com.mipt.team4.cloud_storage_backend.utils.SafeParser;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@Sharable
@RequiredArgsConstructor
public class RequestToDtoDecoder extends MessageToMessageDecoder<FullHttpRequest> {
  private final DtoMetadataCache metadataCache;
  private final RouteRegistry routeRegistry;
  private final ObjectMapper objectMapper;

  @Override
  protected void decode(ChannelHandlerContext ctx, FullHttpRequest request, List<Object> out) {
    String method = request.method().name();
    String path = request.uri().split("\\?")[0];

    Class<?> dtoClass = routeRegistry.getDto(method, path);

    if (dtoClass == null) {
      out.add(request.retain());
      return;
    }

    Object dto = assemble(ctx, dtoClass, request);
    out.add(dto);
  }

  private Object assemble(ChannelHandlerContext ctx, Class<?> dtoClass, FullHttpRequest request) {
    MappedParameter[] parameters = metadataCache.getParameters(dtoClass);
    Constructor<?> constructor = metadataCache.getConstructor(dtoClass);
    Object[] args = parseParameters(ctx, parameters, request);

    try {
      return constructor.newInstance(args);
    } catch (Exception e) {
      throw new CreateDtoInstanceException(dtoClass, e);
    }
  }

  private Object[] parseParameters(
      ChannelHandlerContext ctx, MappedParameter[] parameters, FullHttpRequest request) {
    QueryStringDecoder queryDecoder = new QueryStringDecoder(request.uri());
    JsonNode rootNode = readJson(request.content());
    Object[] args = new Object[parameters.length];

    for (int i = 0; i < parameters.length; i++) {
      MappedParameter param = parameters[i];

      args[i] =
          switch (param.source()) {
            case QUERY -> parseQuery(queryDecoder, param);
            case HEADER -> parseHeader(request, param);
            case BODY -> parseBodyParam(rootNode, param);
            case AUTH -> getAuthAttribute(ctx);
          };
    }

    return args;
  }

  private JsonNode readJson(ByteBuf content) {
    if (content.isReadable()) {
      try (InputStream inputStream = new ByteBufInputStream(content)) {
        return objectMapper.readTree(inputStream);
      } catch (IOException e) {
        throw new ReadJsonBodyException(e);
      }
    }

    return null;
  }

  private Object parseQuery(QueryStringDecoder queryDecoder, MappedParameter param) {
    List<String> values = queryDecoder.parameters().get(param.name());
    String value = (values != null && !values.isEmpty()) ? values.getFirst() : null;
    return SafeParser.parse(
        value, param.type(), param.defaultValue(), param.required(), param.name());
  }

  private Object parseHeader(FullHttpRequest request, MappedParameter param) {
    String value = request.headers().get(param.name());
    return SafeParser.parse(
        value, param.type(), param.defaultValue(), param.required(), param.name());
  }

  private Object getAuthAttribute(ChannelHandlerContext ctx) {
    UUID userId = ctx.channel().attr(SecurityAttributes.USER_ID).get();
    if (userId == null) {
      throw new MissingAuthTokenException();
    }

    return userId;
  }

  private Object parseBodyParam(JsonNode rootNode, MappedParameter param) {
    if (rootNode == null || rootNode.isNull()) {
      if (param.required()) throw new MissingRequiredParamException(param.name());
      return null;
    }

    JsonNode fieldNode = rootNode.get(param.name());
    if (fieldNode == null || fieldNode.isMissingNode()) {
      if (param.required()) throw new MissingRequiredParamException(param.name());

      if (param.defaultValue() != null && !param.defaultValue().isBlank()) {
        return SafeParser.parse(param.defaultValue(), param.type(), null, false, param.name());
      }

      return null;
    }

    try {
      return objectMapper.treeToValue(fieldNode, param.type());
    } catch (JsonProcessingException e) {
      throw new ParseJsonParamException(param.name(), param.type(), e);
    }
  }
}
