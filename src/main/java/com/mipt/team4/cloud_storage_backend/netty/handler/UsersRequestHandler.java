package com.mipt.team4.cloud_storage_backend.netty.handler;

import com.mipt.team4.cloud_storage_backend.controller.user.UserController;
import com.mipt.team4.cloud_storage_backend.exception.netty.HeaderNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.InvalidEmailOrPassword;
import com.mipt.team4.cloud_storage_backend.exception.user.UserAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.user.WrongPasswordException;
import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.model.user.dto.LoginRequestDto;
import com.mipt.team4.cloud_storage_backend.model.user.dto.LogoutRequestDto;
import com.mipt.team4.cloud_storage_backend.model.user.dto.RegisterRequestDto;
import com.mipt.team4.cloud_storage_backend.netty.utils.RequestUtils;
import com.mipt.team4.cloud_storage_backend.netty.utils.ResponseHelper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;

public class UsersRequestHandler {
  private final UserController userController;

  public UsersRequestHandler(UserController userController) {
    this.userController = userController;
  }

  public void handleRegisterRequest(ChannelHandlerContext ctx, HttpRequest request) {
    try {
      userController.registerUser(
          new RegisterRequestDto(
              RequestUtils.getRequiredHeader(request, "X-Auth-Email"),
              RequestUtils.getRequiredHeader(request, "X-Auth-Password-Hash"),
              RequestUtils.getRequiredHeader(request, "X-Auth-Username")));
    } catch (ValidationFailedException e) {
      handleValidationError(ctx, e);
      return;
    } catch (HeaderNotFoundException e) {
      ResponseHelper.sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, e.getMessage());
      return;
    } catch (UserAlreadyExistsException e) {
      // TODO
    }

    ResponseHelper.sendSuccessResponse(
        ctx, HttpResponseStatus.CREATED, "Account created successfully.");
  }

  public void handleLoginRequest(ChannelHandlerContext ctx, HttpRequest request) {
    try {
      userController.loginUser(
          new LoginRequestDto(
              RequestUtils.getRequiredHeader(request, "X-Auth-Email"),
              RequestUtils.getRequiredHeader(request, "X-Auth-Phone-Number"),
              RequestUtils.getRequiredHeader(request, "X-Auth-Password-Hash")));
    } catch (ValidationFailedException e) {
      handleValidationError(ctx, e);
      return;
    } catch (HeaderNotFoundException e) {
      ResponseHelper.sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, e.getMessage());
      return;
    } catch (InvalidEmailOrPassword e) {
      // TODO
    } catch (WrongPasswordException e) {
      // TODO
    }

    ResponseHelper.sendSuccessResponse(
        ctx, HttpResponseStatus.OK, "You have successfully signed in.");
  }

  public void handleLogoutRequest(ChannelHandlerContext ctx, HttpRequest request) {
    try {
      userController.logoutUser(
          new LogoutRequestDto(RequestUtils.getRequiredHeader(request, "X-Auth-Token")));
    } catch (ValidationFailedException e) {
      handleValidationError(ctx, e);
      return;
    } catch (HeaderNotFoundException e) {
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
