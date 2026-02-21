package com.mipt.team4.cloud_storage_backend;

import com.mipt.team4.cloud_storage_backend.netty.server.NettyServerManager;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@ConfigurationPropertiesScan
@SpringBootApplication
@EnableScheduling
public class CloudStorageApplication {
  static void main(String[] args) {
    SpringApplication.run(CloudStorageApplication.class, args);
  }

  @Bean
  public CommandLineRunner run(NettyServerManager server) {
    return args -> {
      server.start();
    };
  }
}
