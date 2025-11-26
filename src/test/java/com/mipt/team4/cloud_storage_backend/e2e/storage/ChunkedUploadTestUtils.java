package com.mipt.team4.cloud_storage_backend.e2e.storage;

import com.mipt.team4.cloud_storage_backend.utils.FileLoader;
import com.mipt.team4.cloud_storage_backend.utils.TestUtils;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class ChunkedUploadTestUtils {
  public static HttpResponse<String> createUploadRequest(
      HttpClient client, String userToken, String filePath, byte[] fileData, String fileTags)
      throws IOException, InterruptedException {
    List<byte[]> chunks = splitFileIntoChunks(fileData, 8 * 1024);

    HttpRequest request = TestUtils.createRequest("/api/files/upload?path=" + filePath)
            .header("Transfer-Encoding", "chunked")
            .header("X-Auth-Token", userToken)
            .header("X-File-Tags", fileTags)
            .header("X-Progress-Update", "false")
            .POST(HttpRequest.BodyPublishers.ofByteArrays(chunks))
            .build();

    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }
// TODO: убрать
//  private static HttpRequest.BodyPublisher createChunkedBodyPublisher(List<byte[]> chunks) {
//    List<byte[]> httpChunks = new ArrayList<>();
//
//    for (byte[] chunk : chunks) {
//      String chunkHeader = Integer.toHexString(chunk.length) + "\r\n";
//      String chunkFooter = "\r\n";
//
//      httpChunks.add(chunkHeader.getBytes(StandardCharsets.US_ASCII));
//      httpChunks.add(chunk);
//      httpChunks.add(chunkFooter.getBytes(StandardCharsets.US_ASCII));
//    }
//
//    return HttpRequest.BodyPublishers.ofByteArrays(httpChunks);
//  }

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
