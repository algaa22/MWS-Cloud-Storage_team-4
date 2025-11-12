package com.mipt.team4.cloud_storage_backend.repository.storage;

import com.mipt.team4.cloud_storage_backend.config.MinioConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MinioContentRepositoryTest {
    @Test
    public void shouldInitialize() {
        MinioConfig minioConfig = MinioConfig.getInstance();
        MinioContentRepository minioContentRepository = new MinioContentRepository(minioConfig);
        minioContentRepository.initialize();
    }
}
