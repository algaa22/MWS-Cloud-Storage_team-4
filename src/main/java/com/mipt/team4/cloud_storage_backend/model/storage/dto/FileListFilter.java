package com.mipt.team4.cloud_storage_backend.model.storage.dto;

import java.util.Optional;
import java.util.UUID;

public record FileListFilter(
    UUID userId,
    UUID parentId,
    boolean includeDirectories,
    boolean recursive,
    Optional<String> tags) {}
