package com.mipt.team4.cloud_storage_backend.schedulers.user;

import com.mipt.team4.cloud_storage_backend.repository.user.UserJpaRepositoryAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuotaSyncScheduler {
  private final UserJpaRepositoryAdapter userRepository;

  @Scheduled(cron = "${storage.scheduling.cron.quota-sync}")
  public void recalculateRealStorageUsage() {
    log.info("Quota sync: Starting global storage quota synchronization...");
    userRepository.syncAllUsersStorage();
    log.info("Quota synchronization complete.");
  }
}
