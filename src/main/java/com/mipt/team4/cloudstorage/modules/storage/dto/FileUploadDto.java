package com.mipt.team4.cloudstorage.modules.storage.dto;

import java.util.List;

// TODO: Как я понимаю, здесь еще будет храниться сам файл, который контроллер берёт из Netty
public record FileUploadDto(
    String name, String path, String bucketName, List<String> tags, String type) {}
