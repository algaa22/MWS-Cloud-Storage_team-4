package com.mipt.team4.cloud_storage_backend.utils.converter;

import com.mipt.team4.cloud_storage_backend.exception.utils.InvalidContentRangeException;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.ContentRangeDto;
import com.mipt.team4.cloud_storage_backend.utils.parser.SafeParser;

public class ContentRangeConverter {
  private static final String CONTENT_TYPE = "bytes";
  private static final int NOT_FOUND = -1;

  public static ContentRangeDto fromClientRange(String rangeStr, long totalSize) {
    String[] rangeParts = split(rangeStr, '=');
    if (!rangeParts[0].equalsIgnoreCase(CONTENT_TYPE)) {
      throw new InvalidContentRangeException();
    }

    rangeParts = split(rangeParts[1], '-');

    ContentRangeDto rangeDto =
        new ContentRangeDto(
            getRangePart(rangeParts, "Range start", 0), getRangePart(rangeParts, "Range end", 1));

    rangeDto = normalizeRange(rangeDto, totalSize);

    if (rangeDto.start() > rangeDto.end() || rangeDto.end() - rangeDto.start() + 1 > totalSize) {
      throw new InvalidContentRangeException();
    }

    return rangeDto;
  }

  public static String toServerRangeString(ContentRangeDto rangeDto, long totalSize) {
    return "%s %s-%s/%s".formatted(CONTENT_TYPE, rangeDto.start(), rangeDto.end(), totalSize);
  }

  private static String[] split(String str, char delimiter) {
    String[] parts = new String[2];
    int index = str.indexOf(delimiter);

    if (index == NOT_FOUND || tooManyDelimiters(str, delimiter, index)) {
      throw new InvalidContentRangeException();
    }

    parts[0] = str.substring(0, index);
    parts[1] = index < str.length() - 1 ? str.substring(index + 1) : "";

    return parts;
  }

  private static ContentRangeDto normalizeRange(ContentRangeDto rangeDto, long totalSize) {
    Long start = rangeDto.start();
    Long end = rangeDto.end();

    if (start == null && end == null) {
      throw new InvalidContentRangeException();
    }

    if (start == null) {
      return new ContentRangeDto(Math.max(0, totalSize - end), totalSize - 1);
    } else if (end == null) {
      return new ContentRangeDto(start, totalSize - 1);
    }

    return rangeDto;
  }

  private static boolean tooManyDelimiters(String str, char delimiter, int firstHyphenIndex) {
    return str.indexOf(delimiter, firstHyphenIndex + 1) != NOT_FOUND;
  }

  private static Long getRangePart(String[] rangeParts, String field, int index) {
    if (!rangeParts[index].isBlank()) {
      return SafeParser.parseLong(field, rangeParts[index]);
    }

    return null;
  }
}
