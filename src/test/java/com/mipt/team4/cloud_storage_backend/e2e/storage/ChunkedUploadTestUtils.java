package com.mipt.team4.cloud_storage_backend.e2e.storage;

import com.mipt.team4.cloud_storage_backend.utils.TestUtils;
import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.List;

public class ChunkedUploadTestUtils {
  public static List<byte[]> splitFileIntoChunks(byte[] fileData, int maxChunkSize) {
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

  public static HttpRequest createChunkedUploadRequest(
      String userToken, String filePath, long fileSize, int totalChunks, String fileTags) {
    return TestUtils.createRequest("/api/files/upload?path=" + filePath)
        .header("Transfer-Encoding", "chunked")
        .header("X-Auth-Token", userToken)
        .header("X-File-Size", String.valueOf(fileSize))
        .header("X-Total-Chunks", String.valueOf(totalChunks))
        .header("X-File-Tags", fileTags)
        .POST(HttpRequest.BodyPublishers.noBody())
        .build();
  }

  public static HttpRequest createChunkRequest() {
    return null;
  }
}
