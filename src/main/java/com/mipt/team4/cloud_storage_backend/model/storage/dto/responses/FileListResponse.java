package com.mipt.team4.cloud_storage_backend.model.storage.dto.responses;

import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.response.ResponseBodyParam;
import java.util.List;

public record FileListResponse(@ResponseBodyParam("files") List<FileInfoResponse> files) {}
