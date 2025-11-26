package com.mipt.team4.cloud_storage_backend.netty.handler;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mipt.team4.cloud_storage_backend.controller.user.UserController;
import com.mipt.team4.cloud_storage_backend.exception.netty.HeaderNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.session.InvalidSessionException;
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

public record UsersRequestHandler(UserController userController) {
  public void handleRegisterRequest(ChannelHandlerContext ctx, HttpRequest request) {
    String token;

    try {
      token =
          userController.registerUser(
              new RegisterRequestDto(
                  RequestUtils.getRequiredHeader(request, "X-Auth-Email"),
                  RequestUtils.getRequiredHeader(request, "X-Auth-Password"),
                  RequestUtils.getRequiredHeader(request, "X-Auth-Username")));
    } catch (ValidationFailedException | HeaderNotFoundException | UserAlreadyExistsException e) {
      ResponseHelper.sendBadRequestExceptionResponse(ctx, e);
      return;
    }

    // TODO: подумать над тем, чтобы преобразовывать ответы DTO в Json
    ObjectNode rootNode =
        ResponseHelper.createJsonResponseNode(
            HttpResponseStatus.CREATED, true, "Account created successfully.");

    rootNode.put("token", token);

    ResponseHelper.sendJsonResponse(ctx, HttpResponseStatus.CREATED, rootNode);
  }

  public void handleLoginRequest(ChannelHandlerContext ctx, HttpRequest request) {
    String token;

    try {
      // TODO: получать хешированный пароль?
      token =
          userController.loginUser(
              new LoginRequestDto(
                  RequestUtils.getRequiredHeader(request, "X-Auth-Email"),
                  RequestUtils.getRequiredHeader(request, "X-Auth-Password")));
    } catch (ValidationFailedException
        | HeaderNotFoundException
        | InvalidEmailOrPassword
        | WrongPasswordException e) {
      ResponseHelper.sendBadRequestExceptionResponse(ctx, e);
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
    } catch (ValidationFailedException
        | InvalidSessionException
        | HeaderNotFoundException
        | UserNotFoundException e) {
      ResponseHelper.sendBadRequestExceptionResponse(ctx, e);
      return;
    }

    ResponseHelper.sendSuccessResponse(
        ctx, HttpResponseStatus.OK, "You have been successfully signed out.");
  }
}
