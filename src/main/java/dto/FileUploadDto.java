package dto;

import java.util.List;

public record FileUploadDto(
    String name,
    String path,
    List<String> tags,
    String type,
    long size
) {}