package com.mipt.team4.cloud_storage_backend.e2e.storage.utils;

import com.mipt.team4.cloud_storage_backend.utils.FileLoader;
import com.mipt.team4.cloud_storage_backend.utils.ITUtils;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.InputStreamEntity;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FileChunkedTransferITUtils {
  private static final int MAX_CHUNK_SIZE = 8 * 1024;

  private final ITUtils itUtils;

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

  public UploadResult sendUploadRequest(
      CloseableHttpClient client,
      String userToken,
      String targetFilePath,
      String filePath,
      String fileTags,
      long fileSize)
      throws IOException {
    HttpPost request =
        new HttpPost(itUtils.createUriString("/api/files/upload?path=" + targetFilePath));

    InputStream fileStream = FileLoader.getInputStream(filePath);
    InputStreamEntity entity =
        new InputStreamEntity(fileStream, -1, ContentType.APPLICATION_OCTET_STREAM);

    request.setEntity(entity);
    request.setHeader(HttpHeaderNames.CONNECTION.toString(), HttpHeaderValues.CLOSE.toString());
    request.setHeader("X-Auth-Token", userToken);
    request.setHeader("X-File-Tags", fileTags);
    request.setHeader("X-File-Size", fileSize);

    return client.execute(request, UploadResult::from);
  }

  public DownloadResult sendDownloadRequest(
      CloseableHttpClient client, String userToken, String targetFilePath) throws IOException {
    HttpGet request =
        new HttpGet(itUtils.createUriString("/api/files/download?path=" + targetFilePath));

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
  }
}
