package com.mipt.team4.cloud_storage_backend.controller.user;

import com.mipt.team4.cloud_storage_backend.model.user.dto.TariffInfoDto;
import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.AvailableTariffsRequest;
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
import io.netty.handler.codec.http.HttpResponseStatus;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class TariffController {
  private final TariffService tariffService;

  public void purchaseTariff(ChannelHandlerContext ctx, PurchaseTariffRequest request) {
    tariffService.purchaseTariff(request);
    ResponseUtils.sendSuccess(ctx, HttpResponseStatus.OK, "Tariff purchased successfully");
  }

  public void getTariffInfo(ChannelHandlerContext ctx, TariffInfoRequest request) {
    TariffInfoDto info = tariffService.getTariffInfo(request);
    ctx.writeAndFlush(info);
  }

  public void setAutoRenew(ChannelHandlerContext ctx, SetAutoRenewRequest request) {
    tariffService.setAutoRenew(request);
    String message = request.enabled() ? "Auto-renew enabled" : "Auto-renew disabled";
    ResponseUtils.sendSuccess(ctx, HttpResponseStatus.OK, message);
  }

  public void updatePaymentMethod(ChannelHandlerContext ctx, UpdatePaymentMethodRequest request) {
    tariffService.updatePaymentMethod(request);
    ResponseUtils.sendSuccess(ctx, HttpResponseStatus.OK, "Payment method updated");
  }

  public void getAvailableTariffs(ChannelHandlerContext ctx, AvailableTariffsRequest request) {
    List<TariffPlanResponse> plans =
        Arrays.stream(TariffPlan.values()).map(TariffPlanResponse::from).toList();
    AvailableTariffsResponse response = new AvailableTariffsResponse(plans);
    ResponseUtils.send(ctx, response);
  }
}
