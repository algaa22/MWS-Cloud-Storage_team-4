package com.mipt.team4.cloud_storage_backend.netty.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mipt.team4.cloud_storage_backend.controller.user.UserController;
import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.model.user.dto.LoginRequestDto;
import com.mipt.team4.cloud_storage_backend.model.user.dto.LogoutRequestDto;
import com.mipt.team4.cloud_storage_backend.model.user.dto.RegisterRequestDto;
import com.mipt.team4.cloud_storage_backend.netty.utils.ResponseHelper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;

public class UsersRequestHandler {
  private final UserController userController;

  public UsersRequestHandler(UserController userController) {
    this.userController = userController;
  }

  public void handleRegisterRequest(ChannelHandlerContext ctx, HttpRequest request) {
    HttpHeaders headers = request.headers();

    try {
      userController.registerUser(
          new RegisterRequestDto(
              headers.get("X-Auth-Email"),
              headers.get("X-Auth-Phone-Number"),
              headers.get("X-Auth-Password-Hash"),
              headers.get("X-Auth-Username")));
    } catch (ValidationFailedException e) {
      handleValidationError(ctx, e);
      return;
    }

    ResponseHelper.sendSuccessResponse(
        ctx, HttpResponseStatus.CREATED, "Account created successfully.");
  }

  public void handleLoginRequest(ChannelHandlerContext ctx, HttpRequest request) {
    HttpHeaders headers = request.headers();

    try {
      userController.loginUser(
          new LoginRequestDto(
              headers.get("X-Auth-Email"),
              headers.get("X-Auth-Phone-Number"),
              headers.get("X-Auth-Password-Hash")));
    } catch (ValidationFailedException e) {
      handleValidationError(ctx, e);
      return;
    }

    ResponseHelper.sendSuccessResponse(
        ctx, HttpResponseStatus.OK, "You have successfully signed in.");
  }

  public void handleLogoutRequest(ChannelHandlerContext ctx, HttpRequest request) {
    HttpHeaders headers = request.headers();

    try {
      userController.logoutUser(new LogoutRequestDto(headers.get("X-Auth-Token")));
    } catch (ValidationFailedException e) {
      handleValidationError(ctx, e);
      return;
    }

    ResponseHelper.sendSuccessResponse(
        ctx, HttpResponseStatus.OK, "You have been successfully signed out.");
  }

  private void handleValidationError(ChannelHandlerContext ctx, ValidationFailedException e) {
    ResponseHelper.sendValidationErrorResponse(ctx, e);
  }
}
