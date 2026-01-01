package com.mipt.team4.cloud_storage_backend.netty.handlers.chunked;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.stream.ChunkedInput;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: rename
class CustomChunkedInput implements ChunkedInput<HttpContent> {

  final Logger logger = LoggerFactory.getLogger(CustomChunkedInput.class);

  private final InputStream stream;
  private final int chunkSize;
  private final long totalSize;
  private boolean ended = false;
  private long bytesSent = 0;

  public CustomChunkedInput(InputStream stream, int chunkSize, long totalSize) {
    this.stream = stream;
    this.chunkSize = chunkSize;
    this.totalSize = totalSize;
  }

  @Override
  public boolean isEndOfInput() {
    return ended;
  }

  @Override
  public void close() throws Exception {
    if (logger.isDebugEnabled()) {
      logger.debug("Closing input stream.");
    }

    stream.close();
  }

  // TODO: refactor

  @Override
  public HttpContent readChunk(ByteBufAllocator allocator) throws Exception {
    if (ended) {
      return null;
    }

    byte[] buffer = new byte[chunkSize];
    int bytesRead = 0;
    int currentRead;

    while (bytesRead < chunkSize
        && (currentRead = stream.read(buffer, bytesRead, chunkSize - bytesRead)) != -1) {
      bytesRead += currentRead;
    }

    if (bytesRead == 0) {
      ended = true;

      if (logger.isDebugEnabled()) {
        logger.debug(
            "End of stream reached. Total bytes sent: {}. Sending LastHttpContent.", bytesSent);
      }

      return LastHttpContent.EMPTY_LAST_CONTENT;
    }

    bytesSent += bytesRead;

    if (logger.isDebugEnabled()) {
      logger.debug("Read from stream: {} bytes (total: {}/{})", bytesRead, bytesSent, totalSize);
    }

    ByteBuf chunkBuf = allocator.buffer(bytesRead);
    chunkBuf.writeBytes(buffer, 0, bytesRead);

    return new DefaultHttpContent(chunkBuf);
  }

  @Deprecated
  @Override
  public HttpContent readChunk(ChannelHandlerContext ctx) throws Exception {
    return readChunk(ctx.alloc());
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
