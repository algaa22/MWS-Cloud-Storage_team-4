package com.mipt.team4.cloud_storage_backend.config;

import com.mipt.team4.cloud_storage_backend.netty.channel.Http2StreamInitializer;
import io.netty.handler.codec.http2.Http2MultiplexHandler;
import io.netty.handler.logging.LoggingHandler;
import java.security.SecureRandom;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {
  @Bean
  public SecureRandom secureRandom() {
    return new SecureRandom();
  }
}
