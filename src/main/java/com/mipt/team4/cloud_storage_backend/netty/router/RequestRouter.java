package com.mipt.team4.cloud_storage_backend.netty.router;

import com.mipt.team4.cloud_storage_backend.controller.storage.FileController;
import com.mipt.team4.cloud_storage_backend.controller.user.UserController;
import com.mipt.team4.cloud_storage_backend.netty.handler.ResponseHelper;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;

public class RequestRouter {
  private final FileController fileController;
  private final UserController userController;
  private final ResponseHelper responseHelper;

  public RequestRouter(FileController fileController, UserController userController) {
    this.fileController = fileController;
    this.userController = userController;
    this.responseHelper = new ResponseHelper();
  }

  public HttpResponse route(FullHttpRequest request) {
    String uri = request.uri();

    try {
      if (uri.startsWith("/api/files") || uri.startsWith("/api/folders")) {
        return fileController.handleRequest(request);
      } else if (uri.startsWith("/api/auth")) {
        return userController.handleRequest(request);
      } else {
        return responseHelper.createJsonResponse(
            HttpResponseStatus.OK, "{\"error\": \"Endpoint not found\"}");
      }
    } catch (Exception e) {
      return responseHelper.createJsonResponse(
          HttpResponseStatus.INTERNAL_SERVER_ERROR,
          "{\"error\": \"Internal server error: " + e.getMessage() + "\"}");
    }
  }
}
