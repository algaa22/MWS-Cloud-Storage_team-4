package com.mipt.team4.cloud_storage_backend.e2e.storage.detailed;

import static org.junit.jupiter.api.Assertions.assertFalse;

import com.mipt.team4.cloud_storage_backend.e2e.storage.BaseDetailedFileIT;
import com.mipt.team4.cloud_storage_backend.e2e.storage.PathParam;
import com.mipt.team4.cloud_storage_backend.e2e.storage.utils.FileOperationsITUtils;
import com.mipt.team4.cloud_storage_backend.utils.TestUtils;
import io.netty.handler.codec.http.HttpMethod;
import java.io.IOException;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonNode;

public class GetFilePathsDetailedIT extends BaseDetailedFileIT {
  public GetFilePathsDetailedIT() {
    super("/api/files/list", HttpMethod.GET.name(), PathParam.EXISTENT_FOLDER);
  }

  @Test
  public void shouldReturnEmptyList_ForNewUser() throws IOException, InterruptedException {
    HttpResponse<String> response =
        FileOperationsITUtils.sendGetFilePathsListRequest(client, currentUserToken, false,  true,null);

    JsonNode rootNode = TestUtils.getRootNodeFromResponse(response);
    assertFalse(rootNode.get("files").elements().hasNext());
  }

  // TODO: тест на параметр includeDirectories
}
