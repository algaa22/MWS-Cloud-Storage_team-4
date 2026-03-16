package com.mipt.team4.cloud_storage_backend.model.storage.dto;

import com.mipt.team4.cloud_storage_backend.netty.constants.ApiEndpoints;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.RequestMapping;

@RequestMapping(method = "POST", path = ApiEndpoints.FILES_CHUNKED_UPLOAD_RESUME)
public record ResumeChunkedUploadRequest() {}
