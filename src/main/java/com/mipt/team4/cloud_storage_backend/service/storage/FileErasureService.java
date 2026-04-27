package com.mipt.team4.cloud_storage_backend.service.storage;

import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.repository.storage.StorageRepository;
import com.mipt.team4.cloud_storage_backend.repository.user.UserJpaRepositoryAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FileErasureService {
  private final StorageRepository storageRepository;
  private final UserJpaRepositoryAdapter userRepository;

  @Transactional
  public void hardDelete(StorageEntity entity) {
    storageRepository.hardDelete(entity);
    userRepository.decreaseUsedStorage(entity.getUserId(), entity.getSize());
  }
}
