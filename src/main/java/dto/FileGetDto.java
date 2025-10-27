package dto;
import java.util.List;

public record FileGetDto(
    String name,
    String key,
    long size,
    String url,
    List<String> tags,
    String bucketName //нужно ли
) {}