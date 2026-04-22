package com.mipt.team4.cloud_storage_backend.antivirus.model.dto;

import java.util.UUID;

public record ScanTaskDto(
    UUID fileId, String name, String hash, String mimeType, String s3Key, long size) {}
