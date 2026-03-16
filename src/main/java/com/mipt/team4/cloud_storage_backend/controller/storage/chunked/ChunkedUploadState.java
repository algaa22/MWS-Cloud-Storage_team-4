package com.mipt.team4.cloud_storage_backend.controller.storage.chunked;

public enum ChunkedUploadState {
  IDLE,
  PROCESSING,
  STOPPED,
  COMPLETED
}
