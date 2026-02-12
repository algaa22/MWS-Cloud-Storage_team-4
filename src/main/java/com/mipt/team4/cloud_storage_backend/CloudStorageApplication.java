package com.mipt.team4.cloud_storage_backend;

import com.mipt.team4.cloud_storage_backend.controller.storage.DirectoryController;
import com.mipt.team4.cloud_storage_backend.controller.storage.FileController;
import com.mipt.team4.cloud_storage_backend.controller.user.UserController;
import com.mipt.team4.cloud_storage_backend.exception.netty.ServerStartException;
import com.mipt.team4.cloud_storage_backend.netty.server.NettyServerManager;
import com.mipt.team4.cloud_storage_backend.repository.database.PostgresConnection;
import com.mipt.team4.cloud_storage_backend.repository.storage.StorageRepository;
import com.mipt.team4.cloud_storage_backend.repository.user.RefreshTokenRepository;
import com.mipt.team4.cloud_storage_backend.repository.user.UserRepository;
import com.mipt.team4.cloud_storage_backend.service.storage.DirectoryService;
import com.mipt.team4.cloud_storage_backend.service.storage.FileService;
import com.mipt.team4.cloud_storage_backend.service.user.UserService;
import com.mipt.team4.cloud_storage_backend.service.user.UserSessionService;
import com.mipt.team4.cloud_storage_backend.service.user.security.JwtService;
import com.mipt.team4.cloud_storage_backend.service.user.security.RefreshTokenService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class CloudStorageApplication {

  private static NettyServerManager server;

  static void main(String[] args) {
    start(DatabaseConfigTEMP.INSTANCE.getUrl(), MinioConfigTEMP.INSTANCE.getUrl());
  }

  public static void start(String postgresUrl, String minioUrl) {
    startAsync(postgresUrl, minioUrl);
    waitUntilStarted();
  }

  public static void stop() {
    if (server != null) {
      server.stop();
    }
  }

  public static void startAsync(String postgresUrl, String minioUrl) {
    PostgresConnection postgresConnection = new PostgresConnection(postgresUrl);
    postgresConnection.connect();

    StorageRepository storageRepository = new StorageRepository(postgresConnection, minioUrl);
    UserRepository userRepository = new UserRepository(postgresConnection);

    JwtService jwtService =
        new JwtService(
            StorageConfigTEMP.INSTANCE.getAccessTokenExpirationSec(),
            StorageConfigTEMP.INSTANCE.getRefreshTokenExpirationSec());
    UserSessionService userSessionService = new UserSessionService(jwtService);

    RefreshTokenRepository refreshTokenRepository = new RefreshTokenRepository(postgresConnection);
    RefreshTokenService refreshTokenService = new RefreshTokenService(refreshTokenRepository);

    FileService fileService =
        new FileService(storageRepository, userRepository, userSessionService);
    DirectoryService directoryService =
        new DirectoryService(storageRepository, userRepository, userSessionService);
    UserService userService =
        new UserService(userRepository, userSessionService, refreshTokenService);

    FileController fileController = new FileController(fileService);
    DirectoryController directoryController = new DirectoryController(directoryService);
    UserController userController = new UserController(userService);

    server = new NettyServerManager(fileController, directoryController, userController);

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
      if (!startupLatch.await(NettyConfigTEMP.INSTANCE.getStartTimeoutSec(), TimeUnit.SECONDS)) {
        throw new ServerStartException(
            "Server start timeout after " + NettyConfigTEMP.INSTANCE.getStartTimeoutSec());
      }
    } catch (InterruptedException e) {
      throw new ServerStartException(e);
    }
  }
}
