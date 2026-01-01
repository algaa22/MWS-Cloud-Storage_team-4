package com.mipt.team4.cloud_storage_backend.model.storage.dto;

public record DownloadedChunkDto(String path, int chunkIndex, byte[] chunkData) {

}

