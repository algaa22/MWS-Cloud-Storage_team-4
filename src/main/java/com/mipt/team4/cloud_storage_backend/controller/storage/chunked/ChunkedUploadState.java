package com.mipt.team4.cloud_storage_backend.controller.storage.chunked;

import com.mipt.team4.cloud_storage_backend.model.storage.dto.ChunkedUploadInfoDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ChunkedUploadState {
  private ChunkedUploadInfoDto info;
  private Status status;

  public enum Status {
    IDLE,
    PROCESSING,
    STOPPED,
    COMPLETED
  }
}
