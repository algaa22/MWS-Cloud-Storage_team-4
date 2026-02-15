package com.mipt.team4.cloud_storage_backend.exception.transfer;

public class CombineChunksToPartException extends Exception {

  public CombineChunksToPartException() {
    super("Failed to combine chunks to part");
  }
}
