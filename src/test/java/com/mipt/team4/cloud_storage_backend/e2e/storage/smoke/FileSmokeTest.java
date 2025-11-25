package com.mipt.team4.cloud_storage_backend.e2e.storage.smoke;

import static org.junit.jupiter.api.Assertions.*;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import com.mipt.team4.cloud_storage_backend.e2e.storage.BaseFileE2ETest;
import com.mipt.team4.cloud_storage_backend.e2e.storage.utils.FileChunkedTransferTestUtils;
import com.mipt.team4.cloud_storage_backend.e2e.storage.utils.FileOperationsTestUtils;
import com.mipt.team4.cloud_storage_backend.e2e.storage.utils.FileSimpleTransferTestUtils;
import com.mipt.team4.cloud_storage_backend.utils.FileLoader;
import com.mipt.team4.cloud_storage_backend.utils.TestUtils;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonNode;

public class FileSmokeTest extends BaseFileE2ETest {
  @Test
  public void shouldUploadAndDownloadFile_Simple() throws IOException, InterruptedException {
    simpleUploadFile(DEFAULT_FILE_TARGET_PATH);

    HttpResponse<byte[]> downloadResponse =
        FileSimpleTransferTestUtils.sendDownloadFileRequest(
            client, currentUserToken, DEFAULT_FILE_TARGET_PATH);

    byte[] originalFile = FileLoader.getInputStream("files/small_file.txt").readAllBytes();
    byte[] downloadedFile = downloadResponse.body();

    assertEquals(HttpStatus.SC_OK, downloadResponse.statusCode());
    assertArrayEquals(downloadedFile, originalFile);
  }

  @Disabled
  @Test
  public void shouldUploadAndDownloadFile_Chunked() throws IOException, InterruptedException {
    byte[] fileData = FileLoader.getInputStream(BIG_FILE_LOCAL_PATH).readAllBytes();

    HttpResponse<String> response =
        FileChunkedTransferTestUtils.createUploadRequest(
            client, currentUserToken, "file.jpg", fileData, "");

    HttpResponse<byte[]> downloadResponse =
        FileSimpleTransferTestUtils.sendDownloadFileRequest(client, currentUserToken, "file.jpg");

    byte[] originalFile = FileLoader.getInputStream("files/big_file.jpg").readAllBytes();
    byte[] downloadedFile = downloadResponse.body();

    assertEquals(HttpStatus.SC_OK, downloadResponse.statusCode());
    assertArrayEquals(downloadedFile, originalFile);

    System.out.println(response.statusCode() + " " + response.body());
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
        FileOperationsTestUtils.sendGetFilePathsListRequest(client, currentUserToken);
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
        FileOperationsTestUtils.sendGetFileInfoRequest(
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
        FileOperationsTestUtils.sendDeleteFileRequest(
            client, currentUserToken, DEFAULT_FILE_TARGET_PATH);
    assertEquals(HttpStatus.SC_OK, deletedResponse.statusCode());

    assertFileExistsIs(false, DEFAULT_FILE_TARGET_PATH);
  }

  @Test
  public void shouldChangeFilePath() throws IOException, InterruptedException {
    simpleUploadFile(DEFAULT_FILE_TARGET_PATH);

    HttpResponse<String> changeFileResponse =
        FileOperationsTestUtils.sendChangeFilePathRequest(
            client, currentUserToken, DEFAULT_FILE_TARGET_PATH, "new_file");
    assertEquals(HttpStatus.SC_OK, changeFileResponse.statusCode());

    assertFileExistsIs(false, DEFAULT_FILE_TARGET_PATH);
    assertFileExistsIs(true, "new_file");
  }

  @Test
  public void shouldChangeFileVisibility() throws IOException, InterruptedException {
    simpleUploadFile(DEFAULT_FILE_TARGET_PATH);

    HttpResponse<String> changeFileResponse =
        FileOperationsTestUtils.sendChangeFileVisibilityRequest(
            client, currentUserToken, DEFAULT_FILE_TARGET_PATH, "public");
    assertEquals(HttpStatus.SC_OK, changeFileResponse.statusCode());

    assertFileInfoMatches(DEFAULT_FILE_TARGET_PATH, "public", null);
  }

  @Test
  public void shouldChangeFileTags() throws IOException, InterruptedException {
    simpleUploadFile(DEFAULT_FILE_TARGET_PATH);

    HttpResponse<String> changeFileResponse =
        FileOperationsTestUtils.sendChangeFileTagsRequest(
            client, currentUserToken, DEFAULT_FILE_TARGET_PATH, "1,2,3");
    assertEquals(HttpStatus.SC_OK, changeFileResponse.statusCode());

    assertFileInfoMatches(DEFAULT_FILE_TARGET_PATH, null, "1,2,3");
  }
}
