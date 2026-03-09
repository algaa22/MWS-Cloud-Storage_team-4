package com.mipt.team4.cloud_storage_backend.e2e.storage.smoke;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import com.mipt.team4.cloud_storage_backend.e2e.storage.BaseStorageIT;
import com.mipt.team4.cloud_storage_backend.e2e.storage.utils.DirectoryOperationsITUtils;
import com.mipt.team4.cloud_storage_backend.e2e.storage.utils.FileOperationsITUtils;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Tag("smoke")
public class DirectorySmokeIT extends BaseStorageIT {
  @Autowired private DirectoryOperationsITUtils directoryOperationsITUtils;
  @Autowired private FileOperationsITUtils fileOperationsITUtils;

  @Test
  public void shouldCreateDirectory() throws IOException, InterruptedException {
    HttpResponse<String> createDirectoryResponse =
        directoryOperationsITUtils.sendCreateDirectoryRequest(
            client, currentUserToken, DEFAULT_DIRECTORY_NAME);
    assertEquals(HttpStatus.SC_CREATED, createDirectoryResponse.statusCode());

    assertTrue(fileListContainsNames(List.of(DEFAULT_DIRECTORY_NAME), null));
  }

  @Test
  public void shouldChangeDirectoryName() throws IOException, InterruptedException {
    HttpResponse<String> createDirectoryResponse =
        directoryOperationsITUtils.sendCreateDirectoryRequest(
            client, currentUserToken, DEFAULT_DIRECTORY_NAME);
    UUID directoryId = itUtils.extractIdFromResponse(createDirectoryResponse);

    assertEquals(HttpStatus.SC_CREATED, createDirectoryResponse.statusCode());
    assertNotNull(directoryId);

    List<String> testFiles = addTestFilesToDirectory(directoryId);
    final String NEW_DIRECTORY_NAME = "dir3";

    HttpResponse<String> changeNameResponse =
        directoryOperationsITUtils.sendChangeDirectoryNameRequest(
            client, currentUserToken, directoryId, NEW_DIRECTORY_NAME);
    assertEquals(HttpStatus.SC_OK, changeNameResponse.statusCode());

    assertTrue(fileListContainsNames(testFiles, directoryId));
  }

  @Test
  public void shouldDeleteDirectory() throws IOException, InterruptedException {
    HttpResponse<String> createDirectoryResponse =
        directoryOperationsITUtils.sendCreateDirectoryRequest(
            client, currentUserToken, DEFAULT_DIRECTORY_NAME);
    UUID directoryId = itUtils.extractIdFromResponse(createDirectoryResponse);
    assertEquals(HttpStatus.SC_CREATED, createDirectoryResponse.statusCode());

    List<String> testFiles = addTestFilesToDirectory(directoryId);

    HttpResponse<String> deleteDirectoryResponse =
        directoryOperationsITUtils.sendDeleteDirectoryRequest(
            client, currentUserToken, directoryId);
    assertEquals(HttpStatus.SC_OK, deleteDirectoryResponse.statusCode());

    assertFalse(fileListContainsNames(testFiles, directoryId));
  }

  private List<String> addTestFilesToDirectory(UUID directoryId)
      throws IOException, InterruptedException {
    List<String> testFiles = new ArrayList<>();

    for (int i = 0; i < 2; i++) {
      String fileName = "file" + i;

      testFiles.add(fileName);
      simpleUploadFile(directoryId, fileName, "");
    }

    return testFiles;
  }

  private boolean fileListContainsNames(List<String> fileNames, UUID searchDirectoryId)
      throws IOException, InterruptedException {
    return fileOperationsITUtils.fileListContainsFileNames(
        client, currentUserToken, fileNames, true, true, searchDirectoryId);
  }
}
