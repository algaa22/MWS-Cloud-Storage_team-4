package com.mipt.team4.cloud_storage_backend.controller.user;

import com.mipt.team4.cloud_storage_backend.model.user.dto.TariffInfoDto;
import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.PurchaseTariffRequest;
import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.SimpleUserRequest;
import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.PurchaseTariffRequest;
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

    public void purchaseTariff(PurchaseTariffRequest request) {
        request.validate(jwtService);
        tariffService.purchaseTariff(request);
    }

    public TariffInfoDto getTariffInfo(SimpleUserRequest request) {
        request.validate(jwtService);
        return tariffService.getTariffInfo(request);
    }

    public void setAutoRenew(SimpleUserRequest request, boolean enabled) {
        request.validate(jwtService);
        tariffService.setAutoRenew(request, enabled);
    }

    public void updatePaymentMethod(UpdateAutoRenewRequest request) {
        request.validate(jwtService);
        tariffService.updatePaymentMethod(request);
    }

    public boolean checkAccess(SimpleUserRequest request) {
        request.validate(jwtService);
        return tariffService.hasAccess(request);
    }
}
