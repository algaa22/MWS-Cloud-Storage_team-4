package com.mipt.team4.cloud_storage_backend.e2e.storage.utils;

import com.mipt.team4.cloud_storage_backend.utils.FileLoader;
import com.mipt.team4.cloud_storage_backend.utils.ITUtils;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FileSimpleTransferITUtils {
  private final ITUtils itUtils;

  public HttpResponse<String> sendUploadRequest(
      HttpClient client,
      String userToken,
      String localFilePath,
      String targetFilePath,
      String fileTags)
      throws IOException, InterruptedException {
    byte[] testFile = FileLoader.getInputStream(localFilePath).readAllBytes();

    HttpRequest request =
        itUtils
            .createRequest("/api/files/upload?path=" + targetFilePath)
            .header("X-Auth-Token", userToken)
            .header("X-File-Tags", fileTags)
            .POST(HttpRequest.BodyPublishers.ofByteArray(testFile))
            .build();

    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }
}
