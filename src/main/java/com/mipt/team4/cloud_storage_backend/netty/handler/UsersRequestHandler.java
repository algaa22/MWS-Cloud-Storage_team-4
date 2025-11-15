package com.mipt.team4.cloud_storage_backend.netty.handler;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mipt.team4.cloud_storage_backend.controller.user.UserController;
import com.mipt.team4.cloud_storage_backend.exception.netty.HeaderNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.InvalidEmailOrPassword;
import com.mipt.team4.cloud_storage_backend.exception.user.UserAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.WrongPasswordException;
import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.model.user.dto.LoginRequestDto;
import com.mipt.team4.cloud_storage_backend.model.user.dto.LogoutRequestDto;
import com.mipt.team4.cloud_storage_backend.model.user.dto.RegisterRequestDto;
import com.mipt.team4.cloud_storage_backend.netty.utils.RequestUtils;
import com.mipt.team4.cloud_storage_backend.netty.utils.ResponseHelper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;

public class UsersRequestHandler {
  private final UserController userController;

  public UsersRequestHandler(UserController userController) {
    this.userController = userController;
  }

  public void handleRegisterRequest(ChannelHandlerContext ctx, HttpRequest request) {
    String token;

    try {
      token =
          userController.registerUser(
              new RegisterRequestDto(
                  RequestUtils.getRequiredHeader(request, "X-Auth-Email"),
                  RequestUtils.getRequiredHeader(request, "X-Auth-Password"),
                  RequestUtils.getRequiredHeader(request, "X-Auth-Username")));
    } catch (ValidationFailedException e) {
      handleValidationError(ctx, e);
      return;
    } catch (HeaderNotFoundException | UserAlreadyExistsException e) {
      ResponseHelper.sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, e.getMessage());
      return;
    }

    ObjectNode rootNode =
        ResponseHelper.createJsonResponseNode(
            HttpResponseStatus.CREATED, true, "Account created successfully.");

    rootNode.put("token", token);

    ResponseHelper.sendJsonResponse(ctx, HttpResponseStatus.CREATED, rootNode);
  }

  public void handleLoginRequest(ChannelHandlerContext ctx, HttpRequest request) {
    String token;

    try {
      token =
          userController.loginUser(
              new LoginRequestDto(
                  RequestUtils.getRequiredHeader(request, "X-Auth-Email"),
                  RequestUtils.getRequiredHeader(request, "X-Auth-Password")));
    } catch (ValidationFailedException e) {
      handleValidationError(ctx, e);
      return;
    } catch (HeaderNotFoundException | InvalidEmailOrPassword | WrongPasswordException e) {
      ResponseHelper.sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, e.getMessage());
      return;
    }

    ObjectNode rootNode =
        ResponseHelper.createJsonResponseNode(
            HttpResponseStatus.OK, true, "You have successfully signed in.");

    rootNode.put("token", token);

    ResponseHelper.sendJsonResponse(ctx, HttpResponseStatus.OK, rootNode);
  }

  public void handleLogoutRequest(ChannelHandlerContext ctx, HttpRequest request) {
    try {
      userController.logoutUser(
          new LogoutRequestDto(RequestUtils.getRequiredHeader(request, "X-Auth-Token")));
    } catch (ValidationFailedException e) {
      handleValidationError(ctx, e);
      return;
    } catch (HeaderNotFoundException | UserNotFoundException e) {
      ResponseHelper.sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, e.getMessage());
      return;
    }

    ResponseHelper.sendSuccessResponse(
        ctx, HttpResponseStatus.OK, "You have been successfully signed out.");
  }

  private void handleValidationError(ChannelHandlerContext ctx, ValidationFailedException e) {
    ResponseHelper.sendValidationErrorResponse(ctx, e);
  }
}
