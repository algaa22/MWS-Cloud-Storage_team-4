package com.mipt.team4.cloud_storage_backend;

import com.mipt.team4.cloud_storage_backend.config.DatabaseConfig;
import com.mipt.team4.cloud_storage_backend.config.NettyConfig;
import com.mipt.team4.cloud_storage_backend.config.sources.EnvironmentConfigSource;
import com.mipt.team4.cloud_storage_backend.config.sources.YamlConfigSource;
import com.mipt.team4.cloud_storage_backend.controller.storage.FileController;
import com.mipt.team4.cloud_storage_backend.controller.user.UserController;
import com.mipt.team4.cloud_storage_backend.netty.handler.HTTPRequestHandler;
import com.mipt.team4.cloud_storage_backend.netty.server.NettyServer;
import com.mipt.team4.cloud_storage_backend.repository.database.PostgresConnection;
import com.mipt.team4.cloud_storage_backend.repository.storage.FileRepository;
import com.mipt.team4.cloud_storage_backend.service.storage.FileService;
import com.mipt.team4.cloud_storage_backend.service.user.UserService;

public class CloudStorageApplication {
  public static void main(String[] args) {
    DatabaseConfig databaseConfig = DatabaseConfig.from(new EnvironmentConfigSource());
    NettyConfig nettyConfig = NettyConfig.from(new YamlConfigSource("config/netty.yml"));

    PostgresConnection postgres = new PostgresConnection(databaseConfig);
    FileRepository fileRepository = new FileRepository(postgres);

    FileService fileService = new FileService(fileRepository);
    UserService userService = new UserService();

    FileController fileController = new FileController(fileService);
    UserController userController = new UserController(userService);

    HTTPRequestHandler requestHandler = new HTTPRequestHandler(fileController, userController);
    NettyServer server = new NettyServer(nettyConfig, requestHandler);

    server.start();
  }
}
