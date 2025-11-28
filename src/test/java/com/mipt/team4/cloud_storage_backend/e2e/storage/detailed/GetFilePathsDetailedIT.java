package com.mipt.team4.cloud_storage_backend.e2e.storage.detailed;

import com.mipt.team4.cloud_storage_backend.e2e.storage.BaseDetailedFileIT;
import com.mipt.team4.cloud_storage_backend.e2e.storage.QueryType;
import com.mipt.team4.cloud_storage_backend.e2e.storage.utils.FileOperationsITUtils;
import com.mipt.team4.cloud_storage_backend.utils.TestUtils;
import io.netty.handler.codec.http.HttpMethod;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GetFilePathsDetailedIT extends BaseDetailedFileIT {
  public GetFilePathsDetailedIT() {
    super("/api/files", HttpMethod.GET.name(), QueryType.FOLDER);
  }

  @Test
  public void shouldReturnEmptyList_ForNewUser() throws IOException, InterruptedException {
    HttpResponse<String> response =
        FileOperationsITUtils.sendGetFilePathsListRequest(client, currentUserToken);

    JsonNode rootNode = TestUtils.getRootNodeFromResponse(response);
    assertFalse(rootNode.get("files").elements().hasNext());
  }
}
