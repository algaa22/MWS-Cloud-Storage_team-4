package com.mipt.team4.cloud_storage_backend.netty.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.mipt.team4.cloud_storage_backend.model.user.dto.RefreshTokenDto;
import com.mipt.team4.cloud_storage_backend.model.user.dto.RegisterRequestDto;
import com.mipt.team4.cloud_storage_backend.model.user.dto.SimpleUserRequestDto;
import com.mipt.team4.cloud_storage_backend.model.user.dto.UpdateUserInfoDto;
import com.mipt.team4.cloud_storage_backend.model.user.dto.UserDto;
import com.mipt.team4.cloud_storage_backend.netty.utils.RequestUtils;
import com.mipt.team4.cloud_storage_backend.netty.utils.ResponseHelper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.util.Optional;

public record UsersRequestHandler(UserController userController) {
  private static final ObjectMapper mapper = new ObjectMapper();

  public void handleRegisterRequest(ChannelHandlerContext ctx, HttpRequest request) {
    String responseJson;

    try {
      responseJson =
          userController.registerUser(
              new RegisterRequestDto(
                  RequestUtils.getRequiredHeader(request, "X-Auth-Email"),
                  RequestUtils.getRequiredHeader(request, "X-Auth-Password"),
                  RequestUtils.getRequiredHeader(request, "X-Auth-Username")));
    } catch (ValidationFailedException | HeaderNotFoundException | UserAlreadyExistsException e) {
      ResponseHelper.sendBadRequestExceptionResponse(ctx, e);
      return;
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }

    try {
      ObjectNode responseNode = (ObjectNode) mapper.readTree(responseJson);
      responseNode.put("success", true);
      responseNode.put("message", "Account created successfully.");

      ResponseHelper.sendJsonResponse(ctx, HttpResponseStatus.CREATED, responseNode);
    } catch (Exception e) {
      ResponseHelper.sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Error processing response");
    }
  }

  public void handleLoginRequest(ChannelHandlerContext ctx, HttpRequest request) {
    String responseJson;

    try {
      responseJson =
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
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }

    try {
      ObjectNode responseNode = (ObjectNode) mapper.readTree(responseJson);
      responseNode.put("success", true);
      responseNode.put("message", "You have successfully signed in.");

      ResponseHelper.sendJsonResponse(ctx, HttpResponseStatus.OK, responseNode);
    } catch (Exception e) {
      ResponseHelper.sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Error processing response");
    }
  }

  public void handleLogoutRequest(ChannelHandlerContext ctx, HttpRequest request) {
    try {
      userController.logoutUser(
          new SimpleUserRequestDto(RequestUtils.getRequiredHeader(request, "X-Auth-Token")));
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

  public void handleGetUserRequest(ChannelHandlerContext ctx, HttpRequest request) {
    UserDto userInfo;

    try {
      userInfo =
          userController.getUserInfo(
              new SimpleUserRequestDto(RequestUtils.getRequiredHeader(request, "X-Auth-Token")));
    } catch (ValidationFailedException | UserNotFoundException | HeaderNotFoundException e) {
      ResponseHelper.sendBadRequestExceptionResponse(ctx, e);
      return;
    }

    ObjectMapper mapper = new ObjectMapper();
    ObjectNode rootNode = mapper.createObjectNode();

    rootNode.put("Name", userInfo.name());
    rootNode.put("Email", userInfo.email());
    rootNode.put("Storage limit", userInfo.storageLimit());
    rootNode.put("IsActive", userInfo.isActive());

    ResponseHelper.sendJsonResponse(ctx, HttpResponseStatus.OK, rootNode);
  }

  public void handleUpdateUserRequest(ChannelHandlerContext ctx, HttpRequest request) {
    Optional<String> newUsername = RequestUtils.getHeader(request, "X-New-Username");
    Optional<String> oldUserPassword = RequestUtils.getHeader(request, "X-Old-Password");
    Optional<String> newUserPassword = RequestUtils.getHeader(request, "X-New-Password");

    try {
      userController.updateUserInfo(
          new UpdateUserInfoDto(
              RequestUtils.getRequiredHeader(request, "X-Auth-Token"),
              oldUserPassword,
              newUserPassword,
              newUsername));
    } catch (ValidationFailedException | HeaderNotFoundException | UserNotFoundException e) {
      ResponseHelper.sendBadRequestExceptionResponse(ctx, e);
      return;
    }

    ResponseHelper.sendSuccessResponse(
        ctx, HttpResponseStatus.OK, "User info successfully changed");
  }
  public void handleRefreshTokenRequest(ChannelHandlerContext ctx, HttpRequest request) {
    String responseJson;

    try {
      responseJson =
          userController.refresh(
              new RefreshTokenDto(
                  RequestUtils.getRequiredHeader(request, "X-Refresh-Token")));
    } catch (InvalidSessionException | HeaderNotFoundException e) {
      ResponseHelper.sendBadRequestExceptionResponse(ctx, e);
      return;
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }

    try {
      ObjectNode responseNode = (ObjectNode) mapper.readTree(responseJson);
      responseNode.put("success", true);
      responseNode.put("message", "Tokens refreshed successfully.");

      ResponseHelper.sendJsonResponse(ctx, HttpResponseStatus.OK, responseNode);
    } catch (Exception e) {
      ResponseHelper.sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Error processing response");
    }
  }
}
