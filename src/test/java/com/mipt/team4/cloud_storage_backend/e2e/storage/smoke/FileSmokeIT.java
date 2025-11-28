package com.mipt.team4.cloud_storage_backend.e2e.storage.smoke;

import static org.junit.jupiter.api.Assertions.*;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import com.mipt.team4.cloud_storage_backend.e2e.storage.BaseFileIT;
import com.mipt.team4.cloud_storage_backend.e2e.storage.utils.FileChunkedTransferITUtils;
import com.mipt.team4.cloud_storage_backend.e2e.storage.utils.FileOperationsITUtils;
import com.mipt.team4.cloud_storage_backend.e2e.storage.utils.FileSimpleTransferITUtils;
import com.mipt.team4.cloud_storage_backend.utils.FileLoader;
import com.mipt.team4.cloud_storage_backend.utils.TestUtils;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import io.netty.handler.codec.http.HttpHeaderNames;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonNode;

public class FileSmokeIT extends BaseFileIT {
  @Test
  public void shouldUploadAndDownloadFile_Simple() throws IOException, InterruptedException {
    simpleUploadFile(DEFAULT_FILE_TARGET_PATH);

    HttpResponse<byte[]> downloadResponse =
        FileSimpleTransferITUtils.sendDownloadRequest(
            client, currentUserToken, DEFAULT_FILE_TARGET_PATH);

    byte[] originalFile = FileLoader.getInputStream(SMALL_FILE_LOCAL_PATH).readAllBytes();
    byte[] downloadedFile = downloadResponse.body();

    assertEquals(HttpStatus.SC_OK, downloadResponse.statusCode());
    assertArrayEquals(downloadedFile, originalFile);
  }

  // TODO: тест на удаление файла во время скачивания/загрузки
  // TODO: проверки на content-type?

  @Test
  public void shouldUploadAndDownloadFile_Chunked() throws IOException, InterruptedException {
    byte[] fileData = FileLoader.getInputStream(BIG_FILE_LOCAL_PATH).readAllBytes();

    HttpResponse<String> uploadResponse =
        FileChunkedTransferITUtils.sendUploadRequest(
            client, currentUserToken, DEFAULT_FILE_TARGET_PATH, fileData, "");
    assertEquals(HttpStatus.SC_OK, uploadResponse.statusCode());

    HttpResponse<InputStream> downloadResponse =
        FileChunkedTransferITUtils.sendDownloadRequest(
            client, currentUserToken, DEFAULT_FILE_TARGET_PATH);
    assertEquals(HttpStatus.SC_OK, downloadResponse.statusCode());

    // TODO: вынести в отдельную функцию
    String receivedTransferEncoding =
        TestUtils.getHeader(downloadResponse, HttpHeaderNames.TRANSFER_ENCODING.toString());
    String receivedFilePath = TestUtils.getHeader(downloadResponse, "X-File-Path");
    String receivedFileSize = TestUtils.getHeader(downloadResponse, "X-File-Size");

    assertEquals("chunked", receivedTransferEncoding);
    assertEquals(DEFAULT_FILE_TARGET_PATH, receivedFilePath);
    assertEquals(String.valueOf(fileData.length), receivedFileSize);

    List<byte[]> downloadedChunks =
        FileChunkedTransferITUtils.readChunksFromResponse(downloadResponse);

    int offset = 0;

    for (int i = 0; i < downloadedChunks.size(); ++i) {
      byte[] chunk = downloadedChunks.get(i);

      assertTrue(offset + chunk.length <= fileData.length);
      assertTrue(FileChunkedTransferITUtils.chunkMatchesOriginal(fileData, chunk, offset));

      offset += chunk.length;
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

    HttpResponse<String> response =
        FileOperationsITUtils.sendGetFilePathsListRequest(client, currentUserToken);
    assertEquals(HttpStatus.SC_OK, response.statusCode());

    JsonNode rootNode = TestUtils.getRootNodeFromResponse(response);
    List<String> responseFilePaths = new ArrayList<>();

    for (int i = 0; i < filePaths.size(); i++) {
      responseFilePaths.add(rootNode.get("files").get(i).get("path").asText());
    }

    for (int i = 0; i < filePaths.size(); i++) {
      assertTrue(responseFilePaths.contains(filePaths.get(i)));
    }
  }

  @Test
  public void shouldGetFileInfo() throws IOException, InterruptedException {
    simpleUploadFile(DEFAULT_FILE_TARGET_PATH, "1,2,3");

    HttpResponse<String> response =
        FileOperationsITUtils.sendGetFileInfoRequest(
            client, currentUserToken, DEFAULT_FILE_TARGET_PATH);
    assertEquals(HttpStatus.SC_OK, response.statusCode());

    byte[] testFile = FileLoader.getInputStream(SMALL_FILE_LOCAL_PATH).readAllBytes();

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
}
