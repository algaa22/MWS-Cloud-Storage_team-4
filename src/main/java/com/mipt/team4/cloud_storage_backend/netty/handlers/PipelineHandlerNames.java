package com.mipt.team4.cloud_storage_backend.netty.handlers;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PipelineHandlerNames {
  public static final String SSL = "sslHandler";
  public static final String LOGGING = "loggingHandler";
  public static final String IDLE_STATE = "idleStateHandler";
  public static final String HTTP2_MULTIPLEX = "http2MultiplexHandler";

  public static final String HTTP2_FRAME = "http2FrameCodec";
  public static final String HTTP_SERVER_CODEC = "httpServerCodec";
  public static final String HTTP2_STREAM_FRAME_TO_OBJECT = "http2StreamFrameToHttpObjectCodec";

  public static final String CORS = "corsHandler";
  public static final String PROTOCOL_NEGOTIATION = "protocolNegotiationHandler";
  public static final String TRAFFIC_STRATEGY_SELECTOR = "trafficStrategySelector";

  public static final String HTTP_OBJECT_AGGREGATOR = "httpObjectAggregator";
  public static final String CHUNKED_WRITE = "chunkedWriteHandler";

  public static final String AGGREGATED_HTTP = "aggregatedHttpHandler";
  public static final String CHUNKED_HTTP = "chunkedHttpHandler";
  public static final String CHUNKED_UPLOAD = "chunkedUploadHandler";
  public static final String CHUNKED_DOWNLOAD = "chunkedDownloadHandler";

  public static final String STORAGE_EXCEPTION = "storageExceptionHandler";
  public static final String GLOBAL_ERROR = "globalErrorHandler";
  public static final String IDLE_TIMEOUT = "idleTimeoutHandler";
}
