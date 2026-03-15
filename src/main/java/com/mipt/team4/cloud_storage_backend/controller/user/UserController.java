package com.mipt.team4.cloud_storage_backend.controller.user;

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
    TokenPairResponse tokens = userService.registerUser(request);
    ctx.writeAndFlush(tokens);
  }

  public void login(ChannelHandlerContext ctx, LoginRequest request) {
    TokenPairResponse tokens = userService.loginUser(request);
    ctx.writeAndFlush(tokens);
  }

  public void logout(ChannelHandlerContext ctx, LogoutRequest request) {
    userService.logoutUser(request);
    ResponseUtils.sendSuccess(ctx, HttpResponseStatus.OK, "Successfully signed out");
  }

  public void getUserInfo(ChannelHandlerContext ctx, UserInfoRequest request) {
    UserInfoResponse userInfo = userService.getUserInfo(request);
    ctx.writeAndFlush(userInfo);
  }

  public void updateUser(ChannelHandlerContext ctx, UpdateUserInfoRequest request) {
    userService.updateUserInfo(request);
    ResponseUtils.sendSuccess(ctx, HttpResponseStatus.OK, "User info updated");
  }

  public void refreshToken(ChannelHandlerContext ctx, RefreshTokenRequest request) {
    TokenPairResponse tokens = userService.refreshTokens(request);
    ctx.writeAndFlush(tokens);
  }
}
