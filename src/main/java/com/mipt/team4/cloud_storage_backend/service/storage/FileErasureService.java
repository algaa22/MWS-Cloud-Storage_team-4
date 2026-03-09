package com.mipt.team4.cloud_storage_backend.service.storage;

import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.repository.storage.StorageRepository;
import com.mipt.team4.cloud_storage_backend.repository.user.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FileErasureService {
  private final StorageRepository storageRepository;
  private final UserRepository userRepository;

  @Transactional
  public void hardDelete(StorageEntity entity) {
    storageRepository.hardDeleteFile(entity);
    userRepository.decreaseUsedStorage(entity.getUserId(), entity.getSize());
  }
}
