package com.mipt.team4.cloud_storage_backend.model.user.enums;

public enum UserStatus {
  ACTIVE, // Полный доступ
  RESTRICTED, // Не может загружать файлы, менять теги и имена
  PENDING_DELETION // Ожидает удаления файлов
}
