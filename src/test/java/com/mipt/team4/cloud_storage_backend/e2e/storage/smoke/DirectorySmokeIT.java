package com.mipt.team4.cloud_storage_backend.e2e.storage.smoke;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import com.mipt.team4.cloud_storage_backend.e2e.storage.BaseStorageIT;
import com.mipt.team4.cloud_storage_backend.e2e.storage.utils.DirectoryOperationsITUtils;
import com.mipt.team4.cloud_storage_backend.e2e.storage.utils.FileOperationsITUtils;
import com.mipt.team4.cloud_storage_backend.utils.TestConstants;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("smoke")
public class DirectorySmokeIT extends BaseStorageIT {

  @Test
  public void shouldCreateDirectory() throws IOException, InterruptedException {
    HttpResponse<String> createDirectoryResponse =
        DirectoryOperationsITUtils.sendCreateDirectoryRequest(
            client, currentUserToken, DEFAULT_DIRECTORY_PATH);
    assertEquals(HttpStatus.SC_CREATED, createDirectoryResponse.statusCode());

    assertTrue(filePathsListContainsFiles(List.of(DEFAULT_DIRECTORY_PATH), null));
  }

  @Test
  public void shouldChangeDirectoryPath() throws IOException, InterruptedException {
    HttpResponse<String> createDirectoryResponse =
        DirectoryOperationsITUtils.sendCreateDirectoryRequest(
            client, currentUserToken, DEFAULT_DIRECTORY_PATH);
    assertEquals(HttpStatus.SC_CREATED, createDirectoryResponse.statusCode());

    List<String> testFiles = addTestFilesToDirectory(DEFAULT_DIRECTORY_PATH);
    final String NEW_DIRECTORY_PATH = "dir3/";

    HttpResponse<String> changePathResponse =
        DirectoryOperationsITUtils.sendChangeDirectoryPathRequest(
            client, currentUserToken, DEFAULT_DIRECTORY_PATH, NEW_DIRECTORY_PATH);
    assertEquals(HttpStatus.SC_OK, changePathResponse.statusCode());

    assertFalse(filePathsListContainsFiles(testFiles, DEFAULT_DIRECTORY_PATH));
    assertTrue(
        filePathsListContainsFiles(
            testFiles.stream()
                .map(filePath -> filePath.replaceFirst(DEFAULT_DIRECTORY_PATH, NEW_DIRECTORY_PATH))
                .toList(),
            NEW_DIRECTORY_PATH));
  }

  @Test
  public void shouldDeleteDirectory() throws IOException, InterruptedException {
    HttpResponse<String> createDirectoryResponse =
        DirectoryOperationsITUtils.sendCreateDirectoryRequest(
            client, currentUserToken, DEFAULT_DIRECTORY_PATH);
    assertEquals(HttpStatus.SC_CREATED, createDirectoryResponse.statusCode());

    List<String> testFiles = addTestFilesToDirectory(DEFAULT_DIRECTORY_PATH);

    HttpResponse<String> deleteDirectoryResponse =
        DirectoryOperationsITUtils.sendDeleteDirectoryRequest(
            client, currentUserToken, DEFAULT_DIRECTORY_PATH);
    assertEquals(HttpStatus.SC_OK, deleteDirectoryResponse.statusCode());

    assertFalse(filePathsListContainsFiles(testFiles, DEFAULT_DIRECTORY_PATH));
  }

  private List<String> addTestFilesToDirectory(String directoryPath)
      throws IOException, InterruptedException {
    List<String> testFiles = new ArrayList<>();

    for (int i = 0; i < 2; i++) {
      String filePath = directoryPath + "file" + i;

      testFiles.add(filePath);
      simpleUploadFile(TestConstants.SMALL_FILE_LOCAL_PATH, filePath, "");
    }

    return testFiles;
  }

  private boolean filePathsListContainsFiles(List<String> filePaths, String searchDirectory)
      throws IOException, InterruptedException {
    return FileOperationsITUtils.filePathsListContainsFiles(
        client, currentUserToken, filePaths, true, true, searchDirectory);
  }
}
