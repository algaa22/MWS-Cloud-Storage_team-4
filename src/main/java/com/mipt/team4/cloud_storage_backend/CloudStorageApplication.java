package com.mipt.team4.cloud_storage_backend;

import com.mipt.team4.cloud_storage_backend.config.StorageConfig;
import com.mipt.team4.cloud_storage_backend.controller.storage.FileController;
import com.mipt.team4.cloud_storage_backend.controller.user.UserController;
import com.mipt.team4.cloud_storage_backend.netty.server.NettyServer;
import com.mipt.team4.cloud_storage_backend.repository.database.PostgresConnection;
import com.mipt.team4.cloud_storage_backend.repository.storage.FileRepository;
import com.mipt.team4.cloud_storage_backend.repository.user.UserRepository;
import com.mipt.team4.cloud_storage_backend.service.storage.FileService;
import com.mipt.team4.cloud_storage_backend.service.user.SessionService;
import com.mipt.team4.cloud_storage_backend.service.user.UserService;
import com.mipt.team4.cloud_storage_backend.service.user.security.JwtService;

public class CloudStorageApplication {
  public static void main(String[] args) {
    PostgresConnection postgres = new PostgresConnection();
    postgres.connect();

    FileRepository fileRepository = new FileRepository(postgres);
    UserRepository userRepository = new UserRepository(postgres);

    SessionService sessionService =
        new SessionService(new JwtService(StorageConfig.INSTANCE.getJwtTokenExpirationSec()));
    FileService fileService = new FileService(fileRepository, sessionService);
    UserService userService = new UserService(userRepository, sessionService);

    FileController fileController = new FileController(fileService);
    UserController userController = new UserController(userService);

    NettyServer server = new NettyServer(fileController, userController);
    server.start();

    postgres.disconnect();
  }
}
