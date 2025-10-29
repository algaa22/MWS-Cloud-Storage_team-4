package com.mipt.team4.cloudstorage.model.storage.dto;

import java.util.List;

public record FileDto(
    String name,
    String path,
    String bucketName,
    String url,
    String type,
    long size,
    List<String> tags) {}
