package dto;
import java.util.List;

public record FileDto(
    String name,
    String path, //key
    long size,
    String url,
    List<String> tags,
    String bucketName

) {}