package com.mipt.team4.cloud_storage_backend.repository.storage;

@FunctionalInterface
public interface MinioOperation<T> {
  T perform() throws Exception;
}
