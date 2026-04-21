package com.mipt.team4.cloud_storage_backend.antivirus.model.dto;

import com.mipt.team4.cloud_storage_backend.antivirus.model.enums.ScanVerdict;
import java.util.UUID;
import lombok.Builder;

@Builder
public record ScanResultDto(UUID fileId, ScanVerdict verdict) {}
