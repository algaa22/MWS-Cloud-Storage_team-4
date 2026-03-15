package com.mipt.team4.cloud_storage_backend.netty.mapping.codec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mipt.team4.cloud_storage_backend.exception.netty.mapping.ReadFieldValueException;
import com.mipt.team4.cloud_storage_backend.exception.netty.mapping.WriteJsonBodyException;
import com.mipt.team4.cloud_storage_backend.netty.mapping.DtoMetadataCache;
import com.mipt.team4.cloud_storage_backend.netty.mapping.MappedParameter;
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
  protected void encode(ChannelHandlerContext channelHandlerContext, Object msg, List<Object> out) {
    FullHttpResponse response =
        new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.buffer());

    Class<?> clazz = msg.getClass();
    MappedParameter[] parameters = metadataCache.getParameters(clazz);
    Map<String, Object> bodyMap = new HashMap<>();

    for (MappedParameter param : parameters) {
      Object value = getFieldValue(msg, param.name());

      if (value == null) {
        if (param.defaultValue() != null && !param.defaultValue().isBlank()) {
          value = param.defaultValue();
        } else {
          continue;
        }
      }

      switch (param.source()) {
        case HEADER -> response.headers().set(param.name(), value.toString());
        case BODY -> bodyMap.put(param.name(), value);
      }
    }

    if (!bodyMap.isEmpty()) {
      writeBodyMap(bodyMap, response);
    } else {
      response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);
    }

    out.add(response);
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
