package com.mipt.team4.cloud_storage_backend.e2e.storage.utils;

import com.mipt.team4.cloud_storage_backend.utils.ITUtils;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DirectoryOperationsITUtils {
    private final ITUtils itUtils;

    public HttpResponse<String> sendCreateDirectoryRequest(
            HttpClient client, String userToken, String directoryName)
            throws IOException, InterruptedException {
        HttpRequest request =
                itUtils
                        .createRequest(itUtils.fillQuery("/api/directories?name=%s", directoryName))
                        .header("X-Auth-Token", userToken)
                        .PUT(HttpRequest.BodyPublishers.noBody())
                        .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public HttpResponse<String> sendDeleteDirectoryRequest(
            HttpClient client, String userToken, UUID directoryId)
            throws IOException, InterruptedException {
        HttpRequest request =
                itUtils
                        .createRequest(itUtils.fillQuery("/api/directories?id=%s", directoryId))
                        .header("X-Auth-Token", userToken)
                        .DELETE()
                        .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public HttpResponse<String> sendChangeDirectoryNameRequest(
            HttpClient client, String currentUserToken, UUID oldDirectoryId, String newDirectoryName)
            throws IOException, InterruptedException {
        HttpRequest request =
                itUtils
                        .createRequest(
                                itUtils.fillQuery(
                                        "/api/directories?id=%s&newName=%s", oldDirectoryId, newDirectoryName))
                        .header("X-Auth-Token", currentUserToken)
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
