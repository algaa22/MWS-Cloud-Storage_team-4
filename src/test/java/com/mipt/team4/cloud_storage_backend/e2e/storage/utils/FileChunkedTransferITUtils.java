package com.mipt.team4.cloud_storage_backend.e2e.storage.utils;

import com.mipt.team4.cloud_storage_backend.utils.TestUtils;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class FileChunkedTransferITUtils {
  private static final int MAX_CHUNK_SIZE = 8 * 1024;

  public static HttpResponse<String> sendUploadRequest(
      HttpClient client, String userToken, String targetFilePath, byte[] fileData, String fileTags)
      throws IOException, InterruptedException {
    List<byte[]> chunks = splitFileIntoChunks(fileData, MAX_CHUNK_SIZE);

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

  public static HttpResponse<InputStream> sendDownloadRequest(
      HttpClient client, String userToken, String targetFilePath)
      throws IOException, InterruptedException {
    // TODO: X-Progress-Download?
    HttpRequest request =
        TestUtils.createRequest("/api/files?path=" + targetFilePath)
            .header("Transfer-Encoding", "chunked")
            .header("X-Auth-Token", userToken)
            .GET()
            .build();

    return client.send(request, HttpResponse.BodyHandlers.ofInputStream());
  }

  public static List<byte[]> readChunksFromResponse(HttpResponse<InputStream> response)
          throws IOException {
    List<byte[]> chunks = new ArrayList<>();

    // TODO: потоково?
    try (InputStream inputStream = response.body()) {
      byte[] buffer = new byte[MAX_CHUNK_SIZE];
      int bytesRead;

      while ((bytesRead = inputStream.read(buffer)) != -1) {
        byte[] chunk = new byte[bytesRead];
        System.arraycopy(buffer, 0, chunk, 0, bytesRead);

        chunks.add(chunk);
      }
    }

    return chunks;
  }

  public static boolean chunkMatchesOriginal(byte[] originalData, byte[] chunk, int offset) {
    for(int i = 0; i < chunk.length; i++) {
      if (originalData[offset + i] != chunk[i])
        return false;
    }

    return true;
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
