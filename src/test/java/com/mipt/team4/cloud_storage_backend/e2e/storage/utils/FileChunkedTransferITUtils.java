package com.mipt.team4.cloud_storage_backend.e2e.storage.utils;

import com.mipt.team4.cloud_storage_backend.config.constants.netty.ApiEndpoints;
import com.mipt.team4.cloud_storage_backend.utils.ITUtils;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FileChunkedTransferITUtils {
  private static final int MAX_CHUNK_SIZE = 8 * 1024;

  private final ITUtils itUtils;

  public UploadResult startUploadSession(
      HttpClient client, String token, String name, long size, int totalParts) throws IOException {
    String url =
        itUtils.createUriString(
            itUtils.fillQuery(ApiEndpoints.FILES_CHUNKED_UPLOAD_START + "?name=%s", name));

    HttpPost request = new HttpPost(url);
    request.setHeader("X-Auth-Token", token);
    request.setHeader("X-Total-Parts", totalParts);
    request.setHeader("X-File-Size", size);

    return client.execute(request, UploadResult::from);
  }

  public UploadResult uploadPart(
      HttpClient client, String token, UUID sessionId, int part, byte[] data, String md5)
      throws IOException {
    String url =
        itUtils.createUriString(
            itUtils.fillQuery(
                ApiEndpoints.FILES_CHUNKED_UPLOAD_PART + "?sessionId=%s&part=%s", sessionId, part));

    HttpPost request = new HttpPost(url);
    request.setHeader("X-Auth-Token", token);
    request.setHeader("Content-MD5", md5);
    request.setEntity(new ByteArrayEntity(data, ContentType.APPLICATION_OCTET_STREAM));

    return client.execute(request, UploadResult::from);
  }

  public UploadResult completeUploadSession(HttpClient client, String token, UUID sessionId)
      throws IOException {
    String url =
        itUtils.createUriString(
            itUtils.fillQuery(
                ApiEndpoints.FILES_CHUNKED_UPLOAD_COMPLETE + "?sessionId=%s", sessionId));

    HttpPost request = new HttpPost(url);
    request.setHeader("X-Auth-Token", token);

    return client.execute(request, UploadResult::from);
  }

  public DownloadResult sendDownloadRequest(
      CloseableHttpClient client, String userToken, UUID targetFileId) throws IOException {
    HttpGet request =
        new HttpGet(
            itUtils.createUriString(itUtils.fillQuery("/api/files/download?id=%s", targetFileId)));

    request.setHeader(HttpHeaderNames.CONNECTION.toString(), HttpHeaderValues.CLOSE.toString());
    request.setHeader("X-Auth-Token", userToken);

    return client.execute(request, DownloadResult::from);
  }

  public boolean chunkMatchesOriginal(byte[] originalData, byte[] chunk, int offset) {
    for (int i = 0; i < chunk.length; i++) {
      if (originalData[offset + i] != chunk[i]) {
        return false;
      }
    }

    return true;
  }

  public List<byte[]> splitData(byte[] data, int chunkSize) {
    List<byte[]> parts = new ArrayList<>();
    int totalLength = data.length;
    int offset = 0;

    while (offset < totalLength) {
      int currentChunkSize = Math.min(chunkSize, totalLength - offset);

      byte[] chunk = new byte[currentChunkSize];
      System.arraycopy(data, offset, chunk, 0, currentChunkSize);

      parts.add(chunk);
      offset += currentChunkSize;
    }

    return parts;
  }

  public record UploadResult(int statusCode, String body) {
    public static UploadResult from(ClassicHttpResponse response)
        throws IOException, ParseException {
      return new UploadResult(response.getCode(), EntityUtils.toString(response.getEntity()));
    }
  }

  public record DownloadResult(int statusCode, Map<String, String> headers, List<byte[]> chunks) {

    public static DownloadResult from(ClassicHttpResponse response) throws IOException {
      Map<String, String> headers = new HashMap<>();

      for (Header header : response.getHeaders()) {
        headers.put(header.getName(), header.getValue());
      }

      return new DownloadResult(
          response.getCode(),
          headers,
          readChunksFromInputStream(response.getEntity().getContent()));
    }

    private static List<byte[]> readChunksFromInputStream(InputStream inputStream)
        throws IOException {
      List<byte[]> chunks = new ArrayList<>();
      byte[] buffer = new byte[MAX_CHUNK_SIZE];
      int bytesRead;

      while ((bytesRead = inputStream.read(buffer)) != -1) {
        byte[] chunk = new byte[bytesRead];
        System.arraycopy(buffer, 0, chunk, 0, bytesRead);

        chunks.add(chunk);
      }

      return chunks;
    }
  }
}
