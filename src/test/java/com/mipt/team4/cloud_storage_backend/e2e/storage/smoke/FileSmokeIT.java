package com.mipt.team4.cloud_storage_backend.e2e.storage.smoke;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import com.mipt.team4.cloud_storage_backend.base.BaseIT;
import com.mipt.team4.cloud_storage_backend.e2e.storage.BaseStorageIT;
import com.mipt.team4.cloud_storage_backend.e2e.storage.utils.FileChunkedTransferITUtils;
import com.mipt.team4.cloud_storage_backend.e2e.storage.utils.FileChunkedTransferITUtils.UploadResult;
import com.mipt.team4.cloud_storage_backend.e2e.storage.utils.FileOperationsITUtils;
import com.mipt.team4.cloud_storage_backend.utils.TestFiles;
import com.mipt.team4.cloud_storage_backend.utils.file.ChecksumUtils;
import io.netty.handler.codec.http.HttpHeaderNames;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonNode;

@Tag("smoke")
public class FileSmokeIT extends BaseStorageIT {
  @Autowired private FileChunkedTransferITUtils chunkedITUtils;
  @Autowired private FileOperationsITUtils fileOperationsITUtils;

  @Test
  public void shouldSimpleUploadAndDownloadFile() throws IOException, InterruptedException {
    UUID fileId = simpleUploadFile(DEFAULT_FILE_TARGET_NAME);
    checkDownloadFile(fileId, TestFiles.SMALL_FILE.getData());
  }

  // TODO: тест на удаление файла во время скачивания/загрузки
  // TODO: проверки на content-type?

  @Test
  public void shouldChunkedUploadAndDownloadFile() throws IOException {
    final int partSize = 5 * 1024 * 1024;

    byte[] fileData = TestFiles.BIG_FILE.getData();
    List<byte[]> parts = chunkedITUtils.splitData(fileData, partSize);

    UploadResult startUploadResult =
        chunkedITUtils.startUploadSession(
            apacheClient,
            currentUserToken,
            DEFAULT_FILE_TARGET_NAME,
            fileData.length,
            parts.size());
    assertEquals(HttpStatus.SC_OK, startUploadResult.statusCode());

    UUID sessionId =
        UUID.fromString(
            itUtils.getRootNodeFromBody(startUploadResult.body()).get("sessionId").asText());

    for (int i = 0; i < parts.size(); i++) {
      int partNumber = i + 1;
      byte[] partData = parts.get(i);
      String checksum = ChecksumUtils.calculateSha256(partData);

      UploadResult uploadPartResult =
          chunkedITUtils.uploadPart(
              apacheClient, currentUserToken, sessionId, partNumber, partData, checksum);
      assertEquals(HttpStatus.SC_OK, uploadPartResult.statusCode());
    }

    UploadResult completeUploadResult =
        chunkedITUtils.completeUploadSession(apacheClient, currentUserToken, sessionId);
    assertEquals(HttpStatus.SC_CREATED, completeUploadResult.statusCode());

    UUID fileId = itUtils.extractIdFromBody(completeUploadResult.body());
    checkDownloadFile(fileId, fileData);
  }

  @Test
  public void shouldGetFileNamesList() throws IOException, InterruptedException {
    List<String> fileNames = new ArrayList<>();

    for (int i = 0; i < 10; i++) {
      String targetFileName = "file" + i + ".txt";
      fileNames.add(targetFileName);

      simpleUploadFile(targetFileName);
    }

    assertTrue(
        fileOperationsITUtils.fileListContainsFileNames(
            client, currentUserToken, fileNames, false, true, null));
  }

  @Test
  public void shouldGetFileInfo() throws IOException, InterruptedException {
    UUID fileId = simpleUploadFile(DEFAULT_FILE_TARGET_NAME, "1,2,3");

    HttpResponse<String> response =
        fileOperationsITUtils.sendGetFileInfoRequest(client, currentUserToken, fileId);
    assertEquals(HttpStatus.SC_OK, response.statusCode());

    byte[] testFile = TestFiles.SMALL_FILE.getData();

    JsonNode rootNode = itUtils.getRootNodeFromResponse(response);
    assertEquals(fileId.toString(), rootNode.get("id").asText());
    assertEquals(DEFAULT_FILE_TARGET_NAME, rootNode.get("name").asText());
    assertEquals(testFile.length, rootNode.get("size").asLong());
    assertEquals("1,2,3", rootNode.get("tags").asText());
    assertEquals("private", rootNode.get("visibility").asText());
    assertFalse(rootNode.get("isDirectory").asText().isEmpty());
    assertNull(rootNode.get("parentId"));
  }

  @Test
  public void shouldDeleteFile() throws IOException, InterruptedException {
    UUID fileId = simpleUploadFile(DEFAULT_FILE_TARGET_NAME);

    HttpResponse<String> deletedResponse =
        fileOperationsITUtils.sendDeleteFileRequest(client, currentUserToken, fileId, true);
    assertEquals(HttpStatus.SC_OK, deletedResponse.statusCode());

    assertFileNotFound(currentUserToken, fileId);
  }

  @Test
  public void shouldSoftDeleteAndRestoreFile() throws IOException, InterruptedException {
    UUID fileId = simpleUploadFile("restore-test-" + UUID.randomUUID() + ".txt");
    byte[] content = TestFiles.SMALL_FILE.getData();

    HttpResponse<String> deleteResponse =
        fileOperationsITUtils.sendDeleteFileRequest(client, currentUserToken, fileId, false);
    assertEquals(HttpStatus.SC_OK, deleteResponse.statusCode());

    FileChunkedTransferITUtils.DownloadResult downloadResult =
        chunkedITUtils.sendDownloadRequest(BaseIT.apacheClient, currentUserToken, fileId);
    assertTrue(downloadResult.statusCode() >= 400);

    HttpResponse<String> restoreResponse =
        fileOperationsITUtils.sendRestoreFileRequest(client, currentUserToken, fileId);

    assertEquals(HttpStatus.SC_OK, restoreResponse.statusCode());

    checkDownloadFile(fileId, content);
  }

  @Test
  public void shouldChangeFilePath() throws IOException, InterruptedException {
    UUID fileId = simpleUploadFile(DEFAULT_FILE_TARGET_NAME);
    final String NEW_FILE_NAME = "new_file";

    HttpResponse<String> changeFileResponse =
        fileOperationsITUtils.sendChangeFilePathRequest(
            client, currentUserToken, fileId, NEW_FILE_NAME);
    assertEquals(HttpStatus.SC_OK, changeFileResponse.statusCode());

    assertFileInfoMatches(fileId, NEW_FILE_NAME, null, null);
  }

  @Test
  public void shouldChangeFileVisibility() throws IOException, InterruptedException {
    UUID fileId = simpleUploadFile(DEFAULT_FILE_TARGET_NAME);

    HttpResponse<String> changeFileResponse =
        fileOperationsITUtils.sendChangeFileVisibilityRequest(
            client, currentUserToken, fileId, "public");
    assertEquals(HttpStatus.SC_OK, changeFileResponse.statusCode());

    assertFileInfoMatches(fileId, null, "PUBLIC", null);
  }

  @Test
  public void shouldChangeFileTags() throws IOException, InterruptedException {
    UUID fileId = simpleUploadFile(DEFAULT_FILE_TARGET_NAME);

    HttpResponse<String> changeFileResponse =
        fileOperationsITUtils.sendChangeFileTagsRequest(client, currentUserToken, fileId, "1,2,3");
    assertEquals(HttpStatus.SC_OK, changeFileResponse.statusCode());

    assertFileInfoMatches(fileId, null, null, "1,2,3");
  }

  private void checkDownloadFile(UUID fileId, byte[] originalFileData) throws IOException {
    FileChunkedTransferITUtils.DownloadResult downloadResult =
        chunkedITUtils.sendDownloadRequest(BaseIT.apacheClient, currentUserToken, fileId);
    assertEquals(HttpStatus.SC_OK, downloadResult.statusCode());

    assertDownloadResponseValid(downloadResult, originalFileData.length);
    assertDownloadedChunksMatchOriginalFile(downloadResult, originalFileData);
  }

  private void assertDownloadResponseValid(
      FileChunkedTransferITUtils.DownloadResult downloadResult, int fileSize) {
    Map<String, String> headers = downloadResult.headers();
    String receivedFileSize = headers.get(HttpHeaderNames.CONTENT_LENGTH.toString());

    assertEquals(String.valueOf(fileSize), receivedFileSize);
  }

  private void assertDownloadedChunksMatchOriginalFile(
      FileChunkedTransferITUtils.DownloadResult downloadResult, byte[] fileData) {
    int offset = 0;

    for (byte[] chunk : downloadResult.chunks()) {
      assertTrue(offset + chunk.length <= fileData.length);
      assertTrue(chunkedITUtils.chunkMatchesOriginal(fileData, chunk, offset));

      offset += chunk.length;
    }
  }
}
