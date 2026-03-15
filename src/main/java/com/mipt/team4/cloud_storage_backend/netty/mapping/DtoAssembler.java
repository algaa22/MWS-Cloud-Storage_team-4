package com.mipt.team4.cloud_storage_backend.netty.mapping;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mipt.team4.cloud_storage_backend.exception.netty.mapping.CreateDtoInstanceException;
import com.mipt.team4.cloud_storage_backend.exception.netty.mapping.ParseJsonParamException;
import com.mipt.team4.cloud_storage_backend.exception.netty.mapping.ReadJsonBodyException;
import com.mipt.team4.cloud_storage_backend.exception.user.auth.MissingAuthTokenException;
import com.mipt.team4.cloud_storage_backend.exception.utils.MissingRequiredParamException;
import com.mipt.team4.cloud_storage_backend.netty.constants.SecurityAttributes;
import com.mipt.team4.cloud_storage_backend.utils.SafeParser;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DtoAssembler {
  private final DtoMetadataCache metadataCache;
  private final ObjectMapper objectMapper;

  public Object assemble(ChannelHandlerContext ctx, Class<?> dtoClass, HttpRequest request) {
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
      ChannelHandlerContext ctx, MappedParameter[] parameters, HttpRequest request) {
    QueryStringDecoder queryDecoder = new QueryStringDecoder(request.uri());
    JsonNode rootNode = readJson(request);
    Object[] args = new Object[parameters.length];

    for (int i = 0; i < parameters.length; i++) {
      MappedParameter param = parameters[i];

      args[i] =
          switch (param.source()) {
            case QUERY -> parseQuery(queryDecoder, param);
            case HEADER -> parseHeader(request, param);
            case AUTH -> getAuthAttribute(ctx);
            case BODY -> parseBodyParam(rootNode, param);
          };
    }

    return args;
  }

  private JsonNode readJson(HttpRequest request) {
    if (!(request instanceof FullHttpRequest)) {
      return null;
    }

    ByteBuf content = ((FullHttpRequest) request).content();

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

  private Object parseHeader(HttpRequest request, MappedParameter param) {
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

    JsonNode fieldNode = param.name().isBlank() ? rootNode : rootNode.get(param.name());

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
