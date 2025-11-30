package com.mipt.team4.cloud_storage_backend.utils;

import com.mipt.team4.cloud_storage_backend.exception.transfer.CombineChunksToPartException;
import com.mipt.team4.cloud_storage_backend.service.storage.ChunkedUploadState;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ChunkCombiner {
  public static byte[] combineChunksToPart(ChunkedUploadState uploadState)
          throws CombineChunksToPartException {
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      for (byte[] chunk : uploadState.chunks) {
        outputStream.write(chunk);
      }

      uploadState.chunks.clear();
      uploadState.partSize = 0;
      uploadState.partNum++;

      return outputStream.toByteArray();
    } catch (IOException e) {
      throw new CombineChunksToPartException();
    }
  }
}
