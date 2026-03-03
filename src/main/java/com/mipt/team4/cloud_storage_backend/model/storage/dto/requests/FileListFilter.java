package com.mipt.team4.cloud_storage_backend.model.storage.dto.requests;

import java.util.UUID;

public record FileListFilter(
    UUID userId, UUID parentId, boolean includeDirectories, boolean recursive) {}
