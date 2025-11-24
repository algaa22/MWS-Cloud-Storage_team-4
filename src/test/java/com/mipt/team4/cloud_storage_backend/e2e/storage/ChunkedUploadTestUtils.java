package com.mipt.team4.cloud_storage_backend.e2e.storage;

import com.mipt.team4.cloud_storage_backend.utils.FileLoader;
import com.mipt.team4.cloud_storage_backend.utils.TestUtils;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.List;

public class ChunkedUploadTestUtils {
  private static List<byte[]> splitFileIntoChunks(byte[] fileData, int maxChunkSize) {
    // TODO: nyzhen?
    List<byte[]> chunks = new ArrayList<>(maxChunkSize);
    int offset = 0;

    while (offset < fileData.length) {
      int chunkSize = Math.min(maxChunkSize, fileData.length - offset);
      byte[] chunk = new byte[chunkSize];

      System.arraycopy(fileData, offset, chunk, 0, chunkSize);
      chunks.add(chunk);

      offset += chunkSize;
    }

    return chunks;
  }

  public static HttpRequest sendUploadRequest(
      String userToken, String filePath, String fileTags) throws IOException {
    byte[] fileData = FileLoader.getInputStream(filePath).readAllBytes();

    return TestUtils.createRequest("/api/files/upload?path=" + filePath)
        .header("Transfer-Encoding", "chunked")
        .header("X-Auth-Token", userToken)
        .header("X-File-Tags", fileTags)
        .POST(HttpRequest.BodyPublishers.ofByteArray(fileData))
        .build();
  }

  public static HttpRequest createChunkRequest() {
    return null;
  }
}
