package com.mipt.team4.cloud_storage_backend;

import com.mipt.team4.cloud_storage_backend.config.DatabaseConfig;
import com.mipt.team4.cloud_storage_backend.config.NettyConfig;
import com.mipt.team4.cloud_storage_backend.config.StorageConfig;
import com.mipt.team4.cloud_storage_backend.config.sources.EnvironmentConfigSource;
import com.mipt.team4.cloud_storage_backend.config.sources.YamlConfigSource;
import com.mipt.team4.cloud_storage_backend.controller.storage.FileController;
import com.mipt.team4.cloud_storage_backend.controller.user.UserController;
import com.mipt.team4.cloud_storage_backend.netty.handler.HttpRequestHandler;
import com.mipt.team4.cloud_storage_backend.netty.server.NettyServer;
import com.mipt.team4.cloud_storage_backend.repository.database.PostgresConnection;
import com.mipt.team4.cloud_storage_backend.repository.storage.FileRepository;
import com.mipt.team4.cloud_storage_backend.service.storage.FileService;
import com.mipt.team4.cloud_storage_backend.service.user.UserService;

public class CloudStorageApplication {
  public static void main(String[] args) {
    EnvironmentConfigSource envConfigSource = new EnvironmentConfigSource();
    YamlConfigSource yamlConfigSource = new YamlConfigSource("config.yml");

    DatabaseConfig databaseConfig = DatabaseConfig.from(envConfigSource);
    NettyConfig nettyConfig = NettyConfig.from(yamlConfigSource);
    StorageConfig storageConfig = StorageConfig.from(yamlConfigSource);

    PostgresConnection postgres = new PostgresConnection(databaseConfig);
    postgres.connect();

    FileRepository fileRepository = new FileRepository(postgres);

    FileService fileService = new FileService(fileRepository);
    UserService userService = new UserService();

    FileController fileController = new FileController(fileService, storageConfig);
    UserController userController = new UserController(userService);

    NettyServer server = new NettyServer(nettyConfig, fileController, userController);
    server.start();

    postgres.disconnect();
  }
}
