package com.mipt.team4.cloud_storage_backend.netty.mapping;

public record RoutedMessage(Object dto, String method, String path) {}
