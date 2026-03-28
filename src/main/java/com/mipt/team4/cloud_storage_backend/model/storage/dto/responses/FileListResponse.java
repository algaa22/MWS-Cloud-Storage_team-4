package com.mipt.team4.cloud_storage_backend.model.storage.dto.responses;

import com.mipt.team4.cloud_storage_backend.model.common.dto.responses.PageResponse;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.response.ResponseBodyParam;

public record FileListResponse(@ResponseBodyParam PageResponse<FileInfoResponse> files) {}
