package com.mipt.team4.cloud_storage_backend.controller.user;

import com.mipt.team4.cloud_storage_backend.model.common.dto.responses.SuccessResponse;
import com.mipt.team4.cloud_storage_backend.model.payment.dto.PaymentHistoryResponse;
import com.mipt.team4.cloud_storage_backend.model.user.dto.TariffInfoDto;
import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.AvailableTariffsRequest;
import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.GetPaymentHistoryRequest;
import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.PurchaseTariffRequest;
import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.SetAutoRenewRequest;
import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.TariffInfoRequest;
import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.UpdatePaymentMethodRequest;
import com.mipt.team4.cloud_storage_backend.model.user.dto.responses.AvailableTariffsResponse;
import com.mipt.team4.cloud_storage_backend.model.user.dto.responses.TariffPlanResponse;
import com.mipt.team4.cloud_storage_backend.model.user.enums.TariffPlan;
import com.mipt.team4.cloud_storage_backend.netty.utils.ResponseUtils;
import com.mipt.team4.cloud_storage_backend.service.user.TariffService;
import io.netty.channel.ChannelHandlerContext;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class TariffController {
  private final TariffService tariffService;

  public void purchaseTariff(ChannelHandlerContext ctx, PurchaseTariffRequest request) {
    tariffService.purchaseTariff(request);
    ResponseUtils.send(ctx, new SuccessResponse("Tariff purchased successfully"));
  }

  public void getTariffInfo(ChannelHandlerContext ctx, TariffInfoRequest request) {
    TariffInfoDto info = tariffService.getTariffInfo(request);
    ResponseUtils.send(ctx, info);
  }

  public void setAutoRenew(ChannelHandlerContext ctx, SetAutoRenewRequest request) {
    tariffService.setAutoRenew(request);
    String message = request.enabled() ? "Auto-renew enabled" : "Auto-renew disabled";
    ResponseUtils.send(ctx, new SuccessResponse(message));
  }

  public void updatePaymentMethod(ChannelHandlerContext ctx, UpdatePaymentMethodRequest request) {
    tariffService.updatePaymentMethod(request);
    ResponseUtils.send(ctx, new SuccessResponse("Payment method updated"));
  }

  public void getAvailableTariffs(ChannelHandlerContext ctx, AvailableTariffsRequest request) {
    List<TariffPlanResponse> plans =
        Arrays.stream(TariffPlan.values()).map(TariffPlanResponse::from).toList();
    AvailableTariffsResponse response = new AvailableTariffsResponse(plans);
    ResponseUtils.send(ctx, response);
  }

  public void getPaymentHistory(ChannelHandlerContext ctx, GetPaymentHistoryRequest request) {
    PaymentHistoryResponse response = tariffService.getPaymentHistory(request.userId());
    ResponseUtils.send(ctx, response);
  }
}
