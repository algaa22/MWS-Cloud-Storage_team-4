package com.mipt.team4.cloud_storage_backend.netty.constants;

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
  public static final String REQUEST_TO_DTO = "requestToDtoDecoder";
  public static final String DTO_TO_RESPONSE = "dtoToResponseEncoder";

  public static final String CORS = "corsHandler";
  public static final String JWT_AUTH = "jwtAuthHandler";
  public static final String GLOBAL_VALIDATION = "globalValidationHandler";
  public static final String PROTOCOL_NEGOTIATION = "protocolNegotiationHandler";
  public static final String TRAFFIC_STRATEGY_SELECTOR = "trafficStrategySelector";

  public static final String HTTP_OBJECT_AGGREGATOR = "httpObjectAggregator";
  public static final String CHUNKED_WRITE = "chunkedWriteHandler";

  public static final String AGGREGATED_HTTP = "aggregatedHttpHandler";
  public static final String CHUNKED_HTTP = "chunkedHttpHandler";

  public static final String STORAGE_EXCEPTION = "storageExceptionHandler";
  public static final String HEAD_GLOBAL_ERROR = "headGlobalErrorHandler";
  public static final String TAIL_GLOBAL_ERROR = "tailGlobalErrorHandler";
  public static final String HTTPS_REDIRECT = "httpsRedirectHandler";
  public static final String IDLE_TIMEOUT = "idleTimeoutHandler";
}
