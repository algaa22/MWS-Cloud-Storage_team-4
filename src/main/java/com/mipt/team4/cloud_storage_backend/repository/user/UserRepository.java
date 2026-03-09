package com.mipt.team4.cloud_storage_backend.repository.user;

import com.mipt.team4.cloud_storage_backend.exception.user.UserAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.model.user.entity.UserEntity;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class UserRepository {
  private final UserJpaRepository jpaRepository;
  private final EntityManager entityManager;

  public void addUser(UserEntity userEntity) {
    if (userEntity.getId() != null && jpaRepository.existsById(userEntity.getId())) {
      throw new UserAlreadyExistsException(userEntity.getId());
    }

    jpaRepository.saveAndFlush(userEntity);
  }

  public Optional<UserEntity> getUserByEmail(String email) {
    return jpaRepository.findByEmail(email);
  }

  public Optional<UserEntity> getUserById(UUID id) {
    return jpaRepository.findById(id);
  }

  @Transactional
  public void increaseUsedStorage(UUID id, long delta) {
    jpaRepository.updateUsedStorage(id, delta);
  }

  @Transactional
  public void decreaseUsedStorage(UUID id, long delta) {
    jpaRepository.updateUsedStorage(id, -delta);
  }

  @Transactional
  public void syncAllUsersStorage() {
    jpaRepository.syncAllUsersStorage();
  }
}
