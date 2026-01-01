package com.mipt.team4.cloud_storage_backend.model.storage.dto;

import java.util.UUID;

public record FileListFilter(UUID userId, boolean includeDirectories, boolean recursive,
                             String searchDirectory) {

}
