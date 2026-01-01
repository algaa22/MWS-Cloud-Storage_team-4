package com.mipt.team4.cloud_storage_backend.e2e.storage.smoke;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import com.mipt.team4.cloud_storage_backend.e2e.storage.BaseStorageIT;
import com.mipt.team4.cloud_storage_backend.e2e.storage.utils.FileChunkedTransferITUtils;
import com.mipt.team4.cloud_storage_backend.e2e.storage.utils.FileOperationsITUtils;
import com.mipt.team4.cloud_storage_backend.e2e.storage.utils.FileSimpleTransferITUtils;
import com.mipt.team4.cloud_storage_backend.utils.TestConstants;
import com.mipt.team4.cloud_storage_backend.utils.TestUtils;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonNode;

public class FileSmokeIT extends BaseStorageIT {

  @Test
  public void shouldUploadAndDownloadFile_Simple() throws IOException, InterruptedException {
    simpleUploadFile(DEFAULT_FILE_TARGET_PATH);

    HttpResponse<byte[]> downloadResponse =
        FileSimpleTransferITUtils.sendDownloadRequest(
            client, currentUserToken, DEFAULT_FILE_TARGET_PATH);

    byte[] originalFile = TestUtils.getSmallTestFile();
    byte[] downloadedFile = downloadResponse.body();

    assertEquals(HttpStatus.SC_OK, downloadResponse.statusCode());
    assertArrayEquals(downloadedFile, originalFile);
  }

  // TODO: тест на удаление файла во время скачивания/загрузки
  // TODO: проверки на content-type?

  @Test
  public void shouldUploadAndDownloadFile_Chunked() throws IOException {
    try (CloseableHttpClient apacheClient = TestUtils.createApacheClient()) {
      byte[] fileData = TestUtils.getBigTestFile();

      FileChunkedTransferITUtils.UploadResult uploadResult =
          FileChunkedTransferITUtils.sendUploadRequest(
              apacheClient,
              currentUserToken,
              DEFAULT_FILE_TARGET_PATH,
              TestConstants.BIG_FILE_LOCAL_PATH,
              "",
              fileData.length);
      assertEquals(HttpStatus.SC_OK, uploadResult.statusCode());

      FileChunkedTransferITUtils.DownloadResult downloadResult =
          FileChunkedTransferITUtils.sendDownloadRequest(
              apacheClient, currentUserToken, DEFAULT_FILE_TARGET_PATH);
      assertEquals(HttpStatus.SC_OK, downloadResult.statusCode());

      assertDownloadResponseValid(downloadResult, fileData.length);
      assertDownloadedChunksMatchOriginalFile(downloadResult, fileData);
    }
  }

  @Test
  public void shouldGetFilePathsList() throws IOException, InterruptedException {
    List<String> filePaths = new ArrayList<>();

    for (int i = 0; i < 10; i++) {
      String targetFilePath = "file" + i + ".txt";
      filePaths.add(targetFilePath);

      simpleUploadFile(targetFilePath);
    }

    assertTrue(
        FileOperationsITUtils.filePathsListContainsFiles(
            client, currentUserToken, filePaths, false, true, null));
  }

  @Test
  public void shouldGetFileInfo() throws IOException, InterruptedException {
    simpleUploadFile(TestConstants.SMALL_FILE_LOCAL_PATH, DEFAULT_FILE_TARGET_PATH, "1,2,3");

    HttpResponse<String> response =
        FileOperationsITUtils.sendGetFileInfoRequest(
            client, currentUserToken, DEFAULT_FILE_TARGET_PATH);
    assertEquals(HttpStatus.SC_OK, response.statusCode());

    byte[] testFile = TestUtils.getSmallTestFile();

    JsonNode rootNode = TestUtils.getRootNodeFromResponse(response);
    assertEquals(DEFAULT_FILE_TARGET_PATH, rootNode.get("Path").asText());
    assertEquals("private", rootNode.get("Visibility").asText());
    assertEquals("1,2,3", rootNode.get("Tags").asText());
    assertEquals(testFile.length, rootNode.get("Size").asLong());
    assertFalse(rootNode.get("Type").asText().isEmpty());
    assertFalse(rootNode.get("IsDeleted").asBoolean());
  }

  @Test
  public void shouldDeleteFile() throws IOException, InterruptedException {
    simpleUploadFile(DEFAULT_FILE_TARGET_PATH);

    HttpResponse<String> deletedResponse =
        FileOperationsITUtils.sendDeleteFileRequest(
            client, currentUserToken, DEFAULT_FILE_TARGET_PATH);
    assertEquals(HttpStatus.SC_OK, deletedResponse.statusCode());

    assertFileExistsIs(false, DEFAULT_FILE_TARGET_PATH);
  }

  @Test
  public void shouldChangeFilePath() throws IOException, InterruptedException {
    simpleUploadFile(DEFAULT_FILE_TARGET_PATH);

    HttpResponse<String> changeFileResponse =
        FileOperationsITUtils.sendChangeFilePathRequest(
            client, currentUserToken, DEFAULT_FILE_TARGET_PATH, "new_file");
    assertEquals(HttpStatus.SC_OK, changeFileResponse.statusCode());

    assertFileExistsIs(false, DEFAULT_FILE_TARGET_PATH);
    assertFileExistsIs(true, "new_file");
  }

  @Test
  public void shouldChangeFileVisibility() throws IOException, InterruptedException {
    simpleUploadFile(DEFAULT_FILE_TARGET_PATH);

    HttpResponse<String> changeFileResponse =
        FileOperationsITUtils.sendChangeFileVisibilityRequest(
            client, currentUserToken, DEFAULT_FILE_TARGET_PATH, "public");
    assertEquals(HttpStatus.SC_OK, changeFileResponse.statusCode());

    assertFileInfoMatches(DEFAULT_FILE_TARGET_PATH, "public", null);
  }

  @Test
  public void shouldChangeFileTags() throws IOException, InterruptedException {
    simpleUploadFile(DEFAULT_FILE_TARGET_PATH);

    HttpResponse<String> changeFileResponse =
        FileOperationsITUtils.sendChangeFileTagsRequest(
            client, currentUserToken, DEFAULT_FILE_TARGET_PATH, "1,2,3");
    assertEquals(HttpStatus.SC_OK, changeFileResponse.statusCode());

    assertFileInfoMatches(DEFAULT_FILE_TARGET_PATH, null, "1,2,3");
  }

  private void assertDownloadResponseValid(
      FileChunkedTransferITUtils.DownloadResult downloadResult, int fileSize) {
    Map<String, String> headers = downloadResult.headers();

    String receivedFilePath = headers.get("X-File-Path");
    String receivedFileSize = headers.get("X-File-Size");

    assertEquals(DEFAULT_FILE_TARGET_PATH, receivedFilePath);
    assertEquals(String.valueOf(fileSize), receivedFileSize);
  }

  private void assertDownloadedChunksMatchOriginalFile(
      FileChunkedTransferITUtils.DownloadResult downloadResult, byte[] fileData) {
    int offset = 0;

    for (byte[] chunk : downloadResult.chunks()) {
      assertTrue(offset + chunk.length <= fileData.length);
      assertTrue(FileChunkedTransferITUtils.chunkMatchesOriginal(fileData, chunk, offset));

      offset += chunk.length;
    }
  }
}
