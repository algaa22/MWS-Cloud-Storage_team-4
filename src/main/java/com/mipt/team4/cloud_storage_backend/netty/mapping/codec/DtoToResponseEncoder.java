package com.mipt.team4.cloud_storage_backend.netty.mapping.codec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mipt.team4.cloud_storage_backend.exception.netty.mapping.ReadFieldValueException;
import com.mipt.team4.cloud_storage_backend.exception.netty.mapping.WriteJsonBodyException;
import com.mipt.team4.cloud_storage_backend.exception.netty.mapping.WrongResponseStatusTypeException;
import com.mipt.team4.cloud_storage_backend.netty.mapping.DtoMetadataCache;
import com.mipt.team4.cloud_storage_backend.netty.mapping.MappedParameter;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.response.ResponseStatus;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.ReferenceCountUtil;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@Sharable
@RequiredArgsConstructor
public class DtoToResponseEncoder extends MessageToMessageEncoder<Object> {
  private final DtoMetadataCache metadataCache;
  private final ObjectMapper objectMapper;

  @Override
  protected void encode(ChannelHandlerContext ctx, Object msg, List<Object> out) {
    if (!msg.getClass().isRecord()) {
      out.add(ReferenceCountUtil.retain(msg));
      return;
    }

    FullHttpResponse response = createInitialResponse(msg);
    processParameters(msg, response);
    out.add(response);
  }

  private FullHttpResponse createInitialResponse(Object msg) {
    FullHttpResponse response =
        new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.buffer());

    ResponseStatus statusAnn = msg.getClass().getAnnotation(ResponseStatus.class);
    if (statusAnn != null) {
      response.setStatus(HttpResponseStatus.valueOf(statusAnn.value()));
    }

    return response;
  }

  private void processParameters(Object msg, FullHttpResponse response) {
    MappedParameter[] parameters = metadataCache.getParameters(msg.getClass());
    Map<String, Object> bodyMap = new HashMap<>();

    for (MappedParameter param : parameters) {
      Object value = resolveValue(msg, param);
      if (value == null) continue;

      mapValueToResponse(param, value, response, bodyMap);
    }

    finalizeResponse(response, bodyMap);
  }

  private Object resolveValue(Object msg, MappedParameter param) {
    Object value = getFieldValue(msg, param.name());
    if (value == null && param.defaultValue() != null && !param.defaultValue().isBlank()) {
      return param.defaultValue();
    }

    return value;
  }

  private void mapValueToResponse(
      MappedParameter param, Object value, FullHttpResponse response, Map<String, Object> bodyMap) {
    if (value == null) {
      return;
    }

    switch (param.source()) {
      case HEADER -> response.headers().set(param.mappedName(), value.toString());
      case BODY_PARAM -> bodyMap.put(param.mappedName(), value);
      case STATUS -> response.setStatus(parseResponseStatus(value));
    }
  }

  private void finalizeResponse(FullHttpResponse response, Map<String, Object> bodyMap) {
    if (!bodyMap.isEmpty()) {
      writeBodyMap(bodyMap, response);
    } else {
      response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);
    }
  }

  private HttpResponseStatus parseResponseStatus(Object value) {
    if (value instanceof HttpResponseStatus status) {
      return status;
    } else if (value instanceof Integer code) {
      return HttpResponseStatus.valueOf(code);
    }

    throw new WrongResponseStatusTypeException(value.getClass());
  }

  private void writeBodyMap(Map<String, Object> bodyMap, FullHttpResponse response) {
    byte[] bytes;

    try {
      bytes = objectMapper.writeValueAsBytes(bodyMap);
    } catch (JsonProcessingException e) {
      throw new WriteJsonBodyException(e);
    }

    response.content().writeBytes(bytes);

    response.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
    response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
  }

  private Object getFieldValue(Object message, String fieldName) {
    Class<?> clazz = message.getClass();

    try {
      return getRecordFieldValue(message, fieldName, clazz);
    } catch (Exception ignored) {
      try {
        return getClassFieldValue(message, fieldName, clazz);
      } catch (Exception e) {
        throw new ReadFieldValueException(fieldName, e);
      }
    }
  }

  private Object getRecordFieldValue(Object message, String fieldName, Class<?> clazz)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    return clazz.getMethod(fieldName).invoke(message);
  }

  private Object getClassFieldValue(Object message, String fieldName, Class<?> clazz)
      throws NoSuchFieldException, IllegalAccessException {
    Field field = clazz.getDeclaredField(fieldName);
    field.setAccessible(true);
    return field.get(message);
  }
}
