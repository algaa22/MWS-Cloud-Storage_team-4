package com.mipt.team4.cloud_storage_backend.netty.input;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.stream.ChunkedInput;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
@RequiredArgsConstructor
@Slf4j
public class ChunkedDownloadInput implements ChunkedInput<HttpContent> {
  private final InputStream stream;
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
    if (ended) {
      return null;
    }

    long remaining = totalSize - bytesSent;
    if (remaining <= 0) {
      ended = true;
      return LastHttpContent.EMPTY_LAST_CONTENT;
    }

    int toRead = (int) Math.min(chunkSize, remaining);
    ByteBuf buffer = allocator.buffer(toRead);
    int localRead;

    try {
      localRead = buffer.writeBytes(stream, toRead);
    } catch (Exception e) {
      buffer.release();
      throw e;
    }

    log.debug("sent: {}, total: {}, local: {}", bytesSent, totalSize, localRead);

    if (localRead <= 0) {
      buffer.release();
      ended = true;
      return LastHttpContent.EMPTY_LAST_CONTENT;
    }

    bytesSent += localRead;

    return new DefaultHttpContent(buffer);
  }

  @Override
  public void close() throws Exception {
    stream.close();
  }

  @Override
  public boolean isEndOfInput() {
    return ended;
  }

  @Override
  public long length() {
    return totalSize;
  }

  @Override
  public long progress() {
    return bytesSent;
  }
}
