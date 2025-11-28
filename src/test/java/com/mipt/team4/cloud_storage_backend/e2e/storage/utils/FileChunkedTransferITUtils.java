package com.mipt.team4.cloud_storage_backend.e2e.storage.utils;

import com.mipt.team4.cloud_storage_backend.utils.FileLoader;
import com.mipt.team4.cloud_storage_backend.utils.TestUtils;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.InputStreamEntity;

public class FileChunkedTransferITUtils {
  private static final int MAX_CHUNK_SIZE = 8 * 1024;

  public record UploadResult(int statusCode, String body) {
    public static UploadResult from(ClassicHttpResponse response)
        throws IOException, ParseException {
      return new UploadResult(response.getCode(), EntityUtils.toString(response.getEntity()));
    }
  }

  public static UploadResult sendUploadRequest(
      CloseableHttpClient client,
      String userToken,
      String targetFilePath,
      String filePath,
      String fileTags)
      throws IOException {
    HttpPost request =
        new HttpPost(TestUtils.createUriString("/api/files/upload?path=" + targetFilePath));

    try (InputStream fileStream = FileLoader.getInputStream(filePath)) {
      InputStreamEntity entity =
              new InputStreamEntity(fileStream, -1, ContentType.APPLICATION_OCTET_STREAM);

      request.setEntity(entity);
      request.setHeader("X-Auth-Token", userToken);
      request.setHeader("X-File-Tags", fileTags);

      return client.execute(request, UploadResult::from);
    }
  }

  public static HttpResponse<InputStream> sendDownloadRequest(
      HttpClient client, String userToken, String targetFilePath)
      throws IOException, InterruptedException {
    HttpRequest request =
        TestUtils.createRequest("/api/files?path=" + targetFilePath)
            .header("X-Download-Mode", "chunked")
            .header("X-Auth-Token", userToken)
            .GET()
            .build();

    return client.send(request, HttpResponse.BodyHandlers.ofInputStream());
  }

  public static List<byte[]> readChunksFromResponse(HttpResponse<InputStream> response)
      throws IOException {
    List<byte[]> chunks = new ArrayList<>();

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
    for (int i = 0; i < chunk.length; i++) {
      if (originalData[offset + i] != chunk[i]) return false;
    }

    return true;
  }

  private static List<byte[]> splitFileIntoChunks(byte[] fileData, int maxChunkSize) {
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
