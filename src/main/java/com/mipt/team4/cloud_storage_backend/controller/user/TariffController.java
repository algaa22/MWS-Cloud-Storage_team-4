package com.mipt.team4.cloud_storage_backend.controller.user;

import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.PurchaseTariffRequest;
import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.SimpleUserRequest;
import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.TariffInfoRequest;
import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.UpdateAutoRenewRequest;
import com.mipt.team4.cloud_storage_backend.service.user.TariffService;
import com.mipt.team4.cloud_storage_backend.service.user.security.AccessTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class TariffController {

  private final TariffService tariffService;
  private final AccessTokenService accessTokenService;

  /** Купить тариф (при регистрации автоматически TRIAL, этот метод для платных) */
  public void purchaseTariff(PurchaseTariffRequest request) {
    request.validate(accessTokenService); // проверяем токен
    tariffService.purchaseTariff(request);
  }

  /** Получить информацию о текущем тарифе */
  public TariffInfoRequest getTariffInfo(SimpleUserRequest request) {
    request.validate(accessTokenService);
    return tariffService.getTariffInfo(request);
  }

  /** Обновить способ оплаты */
  public void updatePaymentMethod(UpdateAutoRenewRequest request) {
    request.validate(accessTokenService);
    tariffService.updatePaymentMethod(request);
  }

  /** Переключить автопродление * */
  public void setAutoRenew(SimpleUserRequest request, boolean enabled) {
    request.validate(accessTokenService);
    tariffService.setAutoRenew(request, enabled);
  }

  /** Проверить доступ к файлам (для FileService) */
  public boolean checkAccess(SimpleUserRequest request) {
    request.validate(accessTokenService);
    return tariffService.hasAccess(request);
  }
}
