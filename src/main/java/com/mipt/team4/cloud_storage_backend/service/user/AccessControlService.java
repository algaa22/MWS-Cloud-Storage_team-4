package com.mipt.team4.cloud_storage_backend.service.user;

import com.mipt.team4.cloud_storage_backend.exception.user.AccessDeniedException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.model.user.entity.UserEntity;
import com.mipt.team4.cloud_storage_backend.model.user.enums.UserStatus;
import com.mipt.team4.cloud_storage_backend.repository.user.UserJpaRepositoryAdapter;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccessControlService {

  private final UserJpaRepositoryAdapter userRepository;
  private final TariffService tariffService;

  public void checkCanUpload(UUID userId) {
    if (!tariffService.canModifyFiles(userId)) {
      UserEntity user =
          userRepository.getUserById(userId).orElseThrow(() -> new UserNotFoundException(userId));

      if (user.getUserStatus() == UserStatus.RESTRICTED) {
        throw AccessDeniedException.cannotUpload();
      }
      throw AccessDeniedException.cannotUpload();
    }
  }

  public void checkCanModifyTags(UUID userId) {
    if (!tariffService.canModifyFiles(userId)) {
      UserEntity user =
          userRepository.getUserById(userId).orElseThrow(() -> new UserNotFoundException(userId));

      if (user.getUserStatus() == UserStatus.RESTRICTED) {
        throw AccessDeniedException.cannotModifyTags();
      }
      throw AccessDeniedException.cannotModifyTags();
    }
  }

  public void checkCanRenameFile(UUID userId) {
    if (!tariffService.canModifyFiles(userId)) {
      UserEntity user =
          userRepository.getUserById(userId).orElseThrow(() -> new UserNotFoundException(userId));

      if (user.getUserStatus() == UserStatus.RESTRICTED) {
        throw AccessDeniedException.cannotRenameFile();
      }
      throw AccessDeniedException.cannotRenameFile();
    }
  }

  public void checkCanDeleteFile(UUID userId) {
    // Удалять файлы можно даже в ограниченном режиме?
    // Обычно да, чтобы можно было освободить место
    if (!tariffService.hasFullAccess(userId)) {
      log.debug("User {} has restricted access but can delete files", userId);
    }
  }

  public void checkCanDownloadFile(UUID userId) {
    // Скачивать файлы можно даже в ограниченном режиме
    if (!tariffService.hasFullAccess(userId)) {
      log.debug("User {} has restricted access but can download files", userId);
    }
  }
}
