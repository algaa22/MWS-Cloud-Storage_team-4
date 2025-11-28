package com.mipt.team4.cloud_storage_backend.e2e.storage.utils;

import com.mipt.team4.cloud_storage_backend.utils.TestUtils;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class FileChunkedTransferITUtils {
  public static HttpResponse<String> sendUploadRequest(
      HttpClient client, String userToken, String targetFilePath, byte[] fileData, String fileTags)
      throws IOException, InterruptedException {
    List<byte[]> chunks = splitFileIntoChunks(fileData, 8 * 1024);

    // TODO: прогресс все равно выводится
    HttpRequest request =
        TestUtils.createRequest("/api/files/upload?path=" + targetFilePath)
            .header("Transfer-Encoding", "chunked")
            .header("X-Auth-Token", userToken)
            .header("X-File-Tags", fileTags)
            .header("X-Progress-Update", "false")
            .POST(HttpRequest.BodyPublishers.ofByteArrays(chunks))
            .build();

    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  public static HttpResponse<String> sendDownloadRequest(
      HttpClient client, String userToken, String targetFilePath)
      throws IOException, InterruptedException {
    HttpRequest request =
        TestUtils.createRequest("/api/files?path=" + targetFilePath)
            .header("Transfer-Encoding", "chunked")
            .header("X-Auth-Token", userToken)
            .GET()
            .build();

    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }

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
}
