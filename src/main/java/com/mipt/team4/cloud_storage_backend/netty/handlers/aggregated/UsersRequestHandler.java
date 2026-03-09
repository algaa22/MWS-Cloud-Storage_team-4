package com.mipt.team4.cloud_storage_backend.netty.handlers.aggregated;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mipt.team4.cloud_storage_backend.controller.user.TariffController;
import com.mipt.team4.cloud_storage_backend.controller.user.UserController;
import com.mipt.team4.cloud_storage_backend.exception.netty.HeaderNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.session.InvalidSessionException;
import com.mipt.team4.cloud_storage_backend.exception.user.InvalidEmailOrPassword;
import com.mipt.team4.cloud_storage_backend.exception.user.UserAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.WrongPasswordException;
import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.model.user.dto.TariffInfoDto;
import com.mipt.team4.cloud_storage_backend.model.user.dto.TokenPairDto;
import com.mipt.team4.cloud_storage_backend.model.user.dto.UserDto;
import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.*;
import com.mipt.team4.cloud_storage_backend.model.user.enums.TariffPlan;
import com.mipt.team4.cloud_storage_backend.netty.utils.RequestUtils;
import com.mipt.team4.cloud_storage_backend.netty.utils.ResponseUtils;
import com.mipt.team4.cloud_storage_backend.utils.SafeParser;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class UsersRequestHandler {
  private final UserController userController;
  private final TariffController tariffController;
  private final ObjectMapper mapper = new ObjectMapper();

  public void handleRegisterRequest(ChannelHandlerContext ctx, HttpRequest request)
      throws HeaderNotFoundException, ValidationFailedException, UserAlreadyExistsException {
    TokenPairDto tokenPair =
        userController.registerUser(
            new RegisterRequest(
                RequestUtils.getRequiredHeader(request, "X-Auth-Email"),
                RequestUtils.getRequiredHeader(request, "X-Auth-Password"),
                RequestUtils.getRequiredHeader(request, "X-Auth-Username")));

    sendTokens(ctx, HttpResponseStatus.CREATED, tokenPair);
  }

  public void handleLoginRequest(ChannelHandlerContext ctx, HttpRequest request)
      throws HeaderNotFoundException,
          ValidationFailedException,
          InvalidEmailOrPassword,
          WrongPasswordException {
    TokenPairDto tokenPair;

    tokenPair =
        userController.loginUser(
            new LoginRequest(
                RequestUtils.getRequiredHeader(request, "X-Auth-Email"),
                RequestUtils.getRequiredHeader(request, "X-Auth-Password")));

    sendTokens(ctx, HttpResponseStatus.OK, tokenPair);
  }

  public void handleLogoutRequest(ChannelHandlerContext ctx, HttpRequest request)
      throws HeaderNotFoundException,
          UserNotFoundException,
          InvalidSessionException,
          ValidationFailedException {
    userController.logoutUser(
        new SimpleUserRequest(RequestUtils.getRequiredHeader(request, "X-Auth-Token")));

    ResponseUtils.sendSuccess(ctx, HttpResponseStatus.OK, "You have been successfully signed out.");
  }

  public void handleGetUserRequest(ChannelHandlerContext ctx, HttpRequest request)
      throws HeaderNotFoundException, UserNotFoundException, ValidationFailedException {
    UserDto userInfo =
        userController.getUserInfo(
            new SimpleUserRequest(RequestUtils.getRequiredHeader(request, "X-Auth-Token")));

    ObjectMapper mapper = new ObjectMapper();
    ObjectNode rootNode = mapper.createObjectNode();

    rootNode.put("Name", userInfo.name());
    rootNode.put("Email", userInfo.email());
    rootNode.put("StorageLimit", userInfo.storageLimit());
    rootNode.put("UsedStorage", userInfo.usedStorage());
    rootNode.put("IsActive", userInfo.isActive());

    ResponseUtils.sendJson(ctx, HttpResponseStatus.OK, rootNode);
  }

  public void handleUpdateUserRequest(ChannelHandlerContext ctx, HttpRequest request)
      throws HeaderNotFoundException,
          UserNotFoundException,
          ValidationFailedException,
          WrongPasswordException {
    Optional<String> newUsername = RequestUtils.getHeader(request, "X-New-Username");
    Optional<String> oldUserPassword = RequestUtils.getHeader(request, "X-Old-Password");
    Optional<String> newUserPassword = RequestUtils.getHeader(request, "X-New-Password");

    userController.updateUserInfo(
        new UpdateUserInfoRequest(
            RequestUtils.getRequiredHeader(request, "X-Auth-Token"),
            oldUserPassword,
            newUserPassword,
            newUsername));

    ResponseUtils.sendSuccess(ctx, HttpResponseStatus.OK, "User info successfully changed");
  }

  public void handleRefreshTokenRequest(ChannelHandlerContext ctx, HttpRequest request)
      throws HeaderNotFoundException, InvalidSessionException, ValidationFailedException {
    TokenPairDto tokenPair =
        userController.refresh(
            new RefreshTokenRequest(RequestUtils.getRequiredHeader(request, "X-Refresh-Token")));

    sendTokens(ctx, HttpResponseStatus.OK, tokenPair);
  }

  private void sendTokens(
      ChannelHandlerContext ctx, HttpResponseStatus status, TokenPairDto tokenPair) {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode rootNode = mapper.createObjectNode();

    rootNode.put("AccessToken", tokenPair.accessToken());
    rootNode.put("RefreshToken", tokenPair.refreshToken());

    ResponseUtils.sendJson(ctx, status, rootNode);
  }

  public void handlePurchaseTariff(ChannelHandlerContext ctx, HttpRequest request)
      throws HeaderNotFoundException, ValidationFailedException {

    System.out.println("🔥🔥🔥 handlePurchaseTariff EXECUTING! 🔥🔥🔥");
    System.out.println("Request URI: " + request.uri());
    String userToken = RequestUtils.getRequiredHeader(request, "X-Auth-Token");
    TariffPlan tariffPlan = TariffPlan.valueOf(RequestUtils.getRequiredQueryParam(request, "plan"));
    String paymentToken = RequestUtils.getRequiredHeader(request, "X-Payment-Token");
    boolean autoRenew =
        SafeParser.parseBoolean(
            "Auto renew", RequestUtils.getQueryParam(request, "autoRenew", "true"));

    tariffController.purchaseTariff(
        new PurchaseTariffRequest(userToken, tariffPlan, paymentToken, autoRenew));

    ResponseUtils.sendSuccess(ctx, HttpResponseStatus.OK, "Tariff purchased successfully");
  }

  public void handleGetTariffInfo(ChannelHandlerContext ctx, HttpRequest request)
      throws HeaderNotFoundException, ValidationFailedException {

    String userToken = RequestUtils.getRequiredHeader(request, "X-Auth-Token");
    SimpleUserRequest userRequest = new SimpleUserRequest(userToken);

    TariffInfoDto info = tariffController.getTariffInfo(userRequest);

    ObjectNode rootNode = mapper.createObjectNode();
    rootNode.put("tariffPlan", info.getTariffPlan().name());
    rootNode.put("storageLimit", info.getStorageLimit());
    rootNode.put("usedStorage", info.getUsedStorage());
    rootNode.put("startDate", info.getStartDate() != null ? info.getStartDate().toString() : null);
    rootNode.put("endDate", info.getEndDate() != null ? info.getEndDate().toString() : null);
    rootNode.put("autoRenew", info.isAutoRenew());
    rootNode.put("isActive", info.isActive());
    rootNode.put("daysLeft", info.getDaysLeft());

    ResponseUtils.sendJson(ctx, HttpResponseStatus.OK, rootNode);
  }

  public void handleDisableAutoRenew(ChannelHandlerContext ctx, HttpRequest request)
      throws HeaderNotFoundException, ValidationFailedException {

    String userToken = RequestUtils.getRequiredHeader(request, "X-Auth-Token");
    SimpleUserRequest userRequest = new SimpleUserRequest(userToken);

    tariffController.disableAutoRenew(userRequest);

    ResponseUtils.sendSuccess(ctx, HttpResponseStatus.OK, "Auto-renew disabled");
  }

  public void handleEnableAutoRenew(ChannelHandlerContext ctx, HttpRequest request)
      throws HeaderNotFoundException, ValidationFailedException {

    String userToken = RequestUtils.getRequiredHeader(request, "X-Auth-Token");
    SimpleUserRequest userRequest = new SimpleUserRequest(userToken);

    tariffController.enableAutoRenew(userRequest);

    ResponseUtils.sendSuccess(ctx, HttpResponseStatus.OK, "Auto-renew enabled");
  }

  public void handleUpdatePaymentMethod(ChannelHandlerContext ctx, HttpRequest request)
      throws HeaderNotFoundException, ValidationFailedException {

    String userToken = RequestUtils.getRequiredHeader(request, "X-Auth-Token");
    String paymentMethodId = RequestUtils.getRequiredHeader(request, "X-Payment-Method-Id");

    UpdateAutoRenewRequest updateRequest = new UpdateAutoRenewRequest(userToken, paymentMethodId);

    tariffController.updatePaymentMethod(updateRequest);

    ResponseUtils.sendSuccess(ctx, HttpResponseStatus.OK, "Payment method updated");
  }

  public void handleGetAvailableTariffs(ChannelHandlerContext ctx, HttpRequest request) {
    ObjectNode rootNode = mapper.createObjectNode();
    ObjectNode tariffsNode = mapper.createObjectNode();

    for (TariffPlan plan : TariffPlan.values()) {
      if (!plan.isTrial()) {
        ObjectNode planNode = mapper.createObjectNode();
        planNode.put("name", plan.name());
        planNode.put("storageLimit", plan.getStorageLimit());
        planNode.put("priceRub", plan.getPriceRub());
        planNode.put("durationDays", plan.getDurationDays());
        tariffsNode.set(plan.name(), planNode);
      }
    }

    rootNode.set("tariffs", tariffsNode);
    ResponseUtils.sendJson(ctx, HttpResponseStatus.OK, rootNode);
  }
}
