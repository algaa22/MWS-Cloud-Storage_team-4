package com.mipt.team4.cloud_storage_backend.model.common.dto;

public record PageQuery(int limit, int offset, String order, String direction) {}
