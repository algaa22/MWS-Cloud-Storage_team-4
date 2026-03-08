package com.mipt.team4.cloud_storage_backend.controller.user;

import com.mipt.team4.cloud_storage_backend.model.user.dto.TariffInfoDto;
import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.TariffRequest;
import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.SimpleUserRequest;
import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.UpdateAutoRenewRequest;
import com.mipt.team4.cloud_storage_backend.service.user.TariffService;
import com.mipt.team4.cloud_storage_backend.service.user.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class TariffController {

    private final TariffService tariffService;
    private final JwtService jwtService;

    /**
     * Купить тариф (при регистрации автоматически TRIAL, этот метод для платных)
     */
    public void purchaseTariff(TariffRequest request) {
        request.validate(jwtService);  // проверяем токен
        tariffService.purchaseTariff(request);
    }

    /**
     * Получить информацию о текущем тарифе
     */
    public TariffInfoDto getTariffInfo(SimpleUserRequest request) {
        request.validate(jwtService);
        return tariffService.getTariffInfo(request);
    }

    /**
     * Отключить автопродление
     */
    public void disableAutoRenew(SimpleUserRequest request) {
        request.validate(jwtService);
        tariffService.disableAutoRenew(request);
    }

    /**
     * Включить автопродление
     */
    public void enableAutoRenew(SimpleUserRequest request) {
        request.validate(jwtService);
        tariffService.enableAutoRenew(request);
    }

    /**
     * Обновить способ оплаты
     */
    public void updatePaymentMethod(UpdateAutoRenewRequest request) {
        request.validate(jwtService);
        tariffService.updatePaymentMethod(request);
    }

    /**
     * Проверить доступ к файлам (для FileService)
     */
    public boolean checkAccess(SimpleUserRequest request) {
        request.validate(jwtService);
        return tariffService.hasAccess(request);
    }
}