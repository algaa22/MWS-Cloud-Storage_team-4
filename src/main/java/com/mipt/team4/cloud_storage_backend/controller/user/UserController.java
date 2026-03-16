package com.mipt.team4.cloud_storage_backend.controller.user;

import com.mipt.team4.cloud_storage_backend.model.common.dto.responses.SuccessResponse;
import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.*;
import com.mipt.team4.cloud_storage_backend.model.user.dto.responses.TokenPairResponse;
import com.mipt.team4.cloud_storage_backend.model.user.dto.responses.UserInfoResponse;
import com.mipt.team4.cloud_storage_backend.netty.utils.ResponseUtils;
import com.mipt.team4.cloud_storage_backend.service.user.UserService;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class UserController {
  private final UserService userService;

  public void register(ChannelHandlerContext ctx, RegisterRequest request) {
    TokenPairResponse tokens =
        new TokenPairResponse(
            HttpResponseStatus.CREATED,
            "Successfully registered",
            userService.registerUser(request));
    ResponseUtils.send(ctx, tokens);
  }

  public void login(ChannelHandlerContext ctx, LoginRequest request) {
    TokenPairResponse tokens =
        new TokenPairResponse(
            HttpResponseStatus.OK, "Successfully signed in", userService.loginUser(request));
    ResponseUtils.send(ctx, tokens);
  }

  public void logout(ChannelHandlerContext ctx, LogoutRequest request) {
    userService.logoutUser(request);
    ResponseUtils.send(ctx, new SuccessResponse("Successfully signed out"));
  }

  public void getUserInfo(ChannelHandlerContext ctx, UserInfoRequest request) {
    UserInfoResponse userInfo = userService.getUserInfo(request);
    ResponseUtils.send(ctx, userInfo);
  }

  public void updateUser(ChannelHandlerContext ctx, UpdateUserInfoRequest request) {
    userService.updateUserInfo(request);
    ResponseUtils.send(ctx, new SuccessResponse("User info updated"));
  }

  public void refreshToken(ChannelHandlerContext ctx, RefreshTokenRequest request) {
    TokenPairResponse tokens =
        new TokenPairResponse(
            HttpResponseStatus.OK,
            "Tokens were successfully refreshed",
            userService.refreshTokens(request));
    ResponseUtils.send(ctx, tokens);
  }
}
