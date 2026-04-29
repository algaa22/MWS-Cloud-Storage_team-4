package com.mipt.team4.cloud_storage_backend.netty.mapping;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mipt.team4.cloud_storage_backend.config.constants.netty.NettyAttributes;
import com.mipt.team4.cloud_storage_backend.exception.netty.mapping.CreateDtoInstanceException;
import com.mipt.team4.cloud_storage_backend.exception.netty.mapping.NotFullRequestException;
import com.mipt.team4.cloud_storage_backend.exception.netty.mapping.ParseJsonParamException;
import com.mipt.team4.cloud_storage_backend.exception.netty.mapping.ReadJsonBodyException;
import com.mipt.team4.cloud_storage_backend.exception.netty.mapping.UnknownRequestSourceTypeException;
import com.mipt.team4.cloud_storage_backend.exception.netty.mapping.WrongParameterTypeException;
import com.mipt.team4.cloud_storage_backend.exception.user.auth.MissingAuthTokenException;
import com.mipt.team4.cloud_storage_backend.exception.utils.MissingRequiredParamException;
import com.mipt.team4.cloud_storage_backend.netty.mapping.MappedParameter.SourceType;
import com.mipt.team4.cloud_storage_backend.utils.parser.SafeParser;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.Arrays;
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
    JsonNode rootNode = dtoHasJson(parameters) ? readJson(request) : null;
    Object[] args = new Object[parameters.length];

    for (int i = 0; i < parameters.length; i++) {
      MappedParameter param = parameters[i];

      args[i] =
          switch (param.source()) {
            case QUERY -> parseQueryParam(queryDecoder, param);
            case HEADER -> parseHeader(request, param);
            case AUTH -> getAuthAttribute(ctx, param);
            case BODY_PARAM -> parseBodyParam(rootNode, param);
            case BODY -> parseBody(request, param);
            case NESTED_DTO -> assemble(ctx, param.type(), request);
            default ->
                throw new UnknownRequestSourceTypeException(param.mappedName(), param.source());
          };
    }

    return args;
  }

  private boolean dtoHasJson(MappedParameter[] parameters) {
    return Arrays.stream(parameters).anyMatch(param -> param.source() == SourceType.BODY_PARAM);
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

  private Object parseQueryParam(QueryStringDecoder queryDecoder, MappedParameter param) {
    List<String> values = queryDecoder.parameters().get(param.mappedName());
    String value = (values != null && !values.isEmpty()) ? values.getFirst() : null;
    return SafeParser.parse(
        value, param.type(), param.defaultValue(), param.required(), param.mappedName());
  }

  private Object parseHeader(HttpRequest request, MappedParameter param) {
    String value = request.headers().get(param.mappedName());
    return SafeParser.parse(
        value, param.type(), param.defaultValue(), param.required(), param.mappedName());
  }

  private Object getAuthAttribute(ChannelHandlerContext ctx, MappedParameter param) {
    if (param.type() != UUID.class) {
      throw new WrongParameterTypeException(param.name(), UUID.class, param.type());
    }

    UUID userId = ctx.channel().attr(NettyAttributes.USER_ID).get();
    if (userId == null) {
      throw new MissingAuthTokenException();
    }

    return userId;
  }

  private Object parseBodyParam(JsonNode rootNode, MappedParameter param) {
    if (rootNode == null || rootNode.isNull()) {
      if (param.required()) throw new MissingRequiredParamException(param.mappedName());
      return null;
    }

    JsonNode fieldNode = param.mappedName().isBlank() ? rootNode : rootNode.get(param.mappedName());

    if (fieldNode == null || fieldNode.isMissingNode()) {
      if (param.required()) throw new MissingRequiredParamException(param.mappedName());

      if (param.defaultValue() != null && !param.defaultValue().isBlank()) {
        return SafeParser.parse(
            param.defaultValue(), param.type(), null, false, param.mappedName());
      }

      return null;
    }

    try {
      return objectMapper.treeToValue(fieldNode, param.type());
    } catch (JsonProcessingException e) {
      throw new ParseJsonParamException(param.mappedName(), param.type(), e);
    }
  }

  private byte[] parseBody(HttpRequest request, MappedParameter param) {
    if (!(request instanceof FullHttpRequest fullRequest)) {
      if (param.required()) {
        throw new NotFullRequestException(param.name());
      } else {
        return null;
      }
    }

    if (param.type() != byte[].class) {
      throw new WrongParameterTypeException(param.name(), byte[].class, param.type());
    }

    ByteBuf data = fullRequest.content();
    if (data.readableBytes() == 0) {
      return null;
    }

    byte[] bytes = new byte[data.readableBytes()];
    data.readBytes(bytes);

    return bytes;
  }
}
