package com.mipt.team4.cloud_storage_backend.model.storage.dto;

import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.response.ResponseBodyParam;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.response.ResponseStatus;
import java.util.UUID;
import org.apache.hc.core5.http.HttpStatus;

@ResponseStatus(HttpStatus.SC_CREATED)
public record ChunkedUploadFileResponse(
    @ResponseBodyParam UUID id, @ResponseBodyParam long size, @ResponseBodyParam long totalParts) {}
