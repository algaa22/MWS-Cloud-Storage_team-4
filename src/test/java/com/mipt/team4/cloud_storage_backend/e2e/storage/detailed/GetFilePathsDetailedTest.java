package com.mipt.team4.cloud_storage_backend.e2e.storage.detailed;

import com.mipt.team4.cloud_storage_backend.e2e.storage.BaseDetailedFileE2ETest;
import com.mipt.team4.cloud_storage_backend.e2e.storage.QueryType;
import com.mipt.team4.cloud_storage_backend.e2e.storage.utils.FileOperationsTestUtils;
import com.mipt.team4.cloud_storage_backend.e2e.user.UserAuthUtils;
import com.mipt.team4.cloud_storage_backend.utils.TestUtils;
import io.netty.handler.codec.http.HttpMethod;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GetFilePathsDetailedTest extends BaseDetailedFileE2ETest {
  public GetFilePathsDetailedTest() {
    super("/api/files", HttpMethod.GET.name(), QueryType.FOLDER);
  }

  @Test
  public void shouldReturnEmptyList_ForNewUser() throws IOException, InterruptedException {
    HttpResponse<String> response =
        FileOperationsTestUtils.sendGetFilePathsListRequest(client, currentUserToken);

    JsonNode rootNode = TestUtils.getRootNodeFromResponse(response);
    assertFalse(rootNode.get("files").elements().hasNext());
  }
}
