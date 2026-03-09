package com.mipt.team4.cloud_storage_backend.scheduler;

import com.mipt.team4.cloud_storage_backend.exception.user.PaymentException;
import com.mipt.team4.cloud_storage_backend.model.user.entity.UserEntity;
import com.mipt.team4.cloud_storage_backend.notification.NotificationClient;
import com.mipt.team4.cloud_storage_backend.repository.user.UserRepository;
import com.mipt.team4.cloud_storage_backend.service.user.PaymentService;
import com.mipt.team4.cloud_storage_backend.service.user.TariffService;

import java.time.LocalDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TariffScheduler {

    private final UserRepository userRepository;
    private final NotificationClient notificationClient;
    private final PaymentService paymentService;
    private final TariffService tariffService;

    @Scheduled(cron = "0 */10 * * * *")
    public void checkTariffs() {
        checkUsersWithTariffEndingSoon(7);
        checkUsersWithTariffEndingSoon(3);
        checkUsersWithTariffEndingSoon(1);
        checkExpiredTariffs();
    }

    private void checkUsersWithTariffEndingSoon(int days) {
        LocalDateTime from = LocalDateTime.now().plusDays(days);
        LocalDateTime to = from.plusDays(1);

        List<UserEntity> users = userRepository.findUsersWithTariffEndingBetween(from, to);

        for (UserEntity user : users) {
            notificationClient.notifyTariffEndingSoon(
                    user.getEmail(), user.getName(), days, user.getTariffEndDate());
            log.info("Notified user {}: tariff ends in {} days", user.getId(), days);
        }
    }

    private void checkExpiredTariffs() {
        List<UserEntity> expiredUsers = userRepository.findUsersWithExpiredTariff(LocalDateTime.now());

        for (UserEntity user : expiredUsers) {
            if (user.isAutoRenew()) {
                handleAutoRenew(user);
            } else {
                deactivateUser(user);
            }
        }
    }

    private void handleAutoRenew(UserEntity user) {
        try {
            paymentService.autoRenewTariff(user.getId());

            LocalDateTime newEndDate = LocalDateTime.now().plusDays(30);
            userRepository.updateTariffEndDate(user.getId(), newEndDate);

            notificationClient.notifyTariffRenewed(user.getEmail(), user.getName(), newEndDate);

        } catch (PaymentException e) {
            log.error("Auto-renew failed for user: {}", user.getId(), e);
            deactivateUser(user);
        }
    }

    private void deactivateUser(UserEntity user) {
        userRepository.deactivateUser(user.getId());

        // TODO: сделать файлы недоступными

        notificationClient.notifyTariffExpired(user.getEmail(), user.getName());

        log.info("User {} deactivated due to expired tariff", user.getId());
    }
}
