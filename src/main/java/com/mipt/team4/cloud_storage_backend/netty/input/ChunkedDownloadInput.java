package com.mipt.team4.cloud_storage_backend.netty.input;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.stream.ChunkedInput;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.ScatteringByteChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
@RequiredArgsConstructor
@Slf4j
public class ChunkedDownloadInput implements ChunkedInput<HttpContent> {

  private static final int EOF = -1;

  private final ReadableByteChannel channel;
  private final int chunkSize;
  private final long totalSize;

  private boolean ended = false;
  private long bytesSent = 0;

  @Override
  public HttpContent readChunk(ChannelHandlerContext ctx) throws Exception {
    return readChunk(ctx.alloc());
  }

  @Override
  public HttpContent readChunk(ByteBufAllocator allocator) throws Exception {
    if (isEndOfInput()) {
      return null;
    }

    ByteBuf buffer = allocator.buffer(chunkSize);
    int localRead = readChannel(buffer);

    if (localRead == 0) {
      buffer.release();

      if (ended) {
        return LastHttpContent.EMPTY_LAST_CONTENT;
      }

      return null;
    }

    bytesSent += localRead;

    return new DefaultHttpContent(buffer);
  }

  @Override
  public void close() throws Exception {
    channel.close();
  }

  @Override
  public boolean isEndOfInput() {
    return ended || bytesSent >= totalSize;
  }

  @Override
  public long length() {
    return totalSize;
  }

  @Override
  public long progress() {
    return bytesSent;
  }

  private int readChannel(ByteBuf buffer) throws IOException {
    int localRead = 0;

    try {
      while (localRead < chunkSize) {
        int read = buffer.writeBytes((ScatteringByteChannel) channel, chunkSize - localRead);

        if (read == EOF) {
          ended = true;
          break;
        }

        localRead += read;
      }
    } catch (Exception e) {
      buffer.release();
      throw e;
    }

    return localRead;
  }
}
