package com.mipt.team4.cloud_storage_backend;

import com.mipt.team4.cloud_storage_backend.config.DatabaseConfig;
import com.mipt.team4.cloud_storage_backend.config.MinioConfig;
import com.mipt.team4.cloud_storage_backend.config.NettyConfig;
import com.mipt.team4.cloud_storage_backend.config.StorageConfig;
import com.mipt.team4.cloud_storage_backend.controller.storage.FileController;
import com.mipt.team4.cloud_storage_backend.controller.user.UserController;
import com.mipt.team4.cloud_storage_backend.exception.netty.ServerStartException;
import com.mipt.team4.cloud_storage_backend.netty.server.NettyServer;
import com.mipt.team4.cloud_storage_backend.repository.database.PostgresConnection;
import com.mipt.team4.cloud_storage_backend.repository.storage.FileRepository;
import com.mipt.team4.cloud_storage_backend.repository.user.UserRepository;
import com.mipt.team4.cloud_storage_backend.service.storage.FileService;
import com.mipt.team4.cloud_storage_backend.service.user.UserSessionService;
import com.mipt.team4.cloud_storage_backend.service.user.UserService;
import com.mipt.team4.cloud_storage_backend.service.user.security.JwtService;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class CloudStorageApplication {
  private static NettyServer server;

  public static void main(String[] args) {
    start(DatabaseConfig.INSTANCE.getUrl(), MinioConfig.INSTANCE.getUrl());
  }

  public static void start(String postgresUrl, String minioUrl) {
    startAsync(postgresUrl, minioUrl);
    waitUntilStarted();
  }

  public static void stop() {
    if (server != null) server.stop();
  }

  private static void startAsync(String postgresUrl, String minioUrl) {
    PostgresConnection postgresConnection = new PostgresConnection(postgresUrl);
    postgresConnection.connect();

    FileRepository fileRepository = new FileRepository(postgresConnection, minioUrl);
    UserRepository userRepository = new UserRepository(postgresConnection);

    UserSessionService userSessionService =
        new UserSessionService(new JwtService(StorageConfig.INSTANCE.getJwtTokenExpirationSec()));
    FileService fileService = new FileService(fileRepository, userSessionService);
    UserService userService = new UserService(userRepository, userSessionService);

    FileController fileController = new FileController(fileService);
    UserController userController = new UserController(userService);

    server = new NettyServer(fileController, userController);

    Thread serverThread =
        new Thread(
            () -> {
              server.start();

              postgresConnection.disconnect();
            });

    serverThread.start();
  }

  private static void waitUntilStarted() {
    CountDownLatch startupLatch = server.getStartupLatch();

    try {
      if (!startupLatch.await(NettyConfig.INSTANCE.getStartTimeoutSec(), TimeUnit.SECONDS))
        throw new ServerStartException(
            "Server start timeout after " + NettyConfig.INSTANCE.getStartTimeoutSec());
    } catch (InterruptedException e) {
      throw new ServerStartException(e);
    }
  }
}
