package com.mipt.team4.cloud_storage_backend.utils.converters;

import com.mipt.team4.cloud_storage_backend.utils.FileTagsMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.List;

@Converter(autoApply = true)
public class FileTagsConverter implements AttributeConverter<List<String>, String> {
  @Override
  public String convertToDatabaseColumn(List<String> attribute) {
    return FileTagsMapper.toString(attribute);
  }

  @Override
  public List<String> convertToEntityAttribute(String dbData) {
    return FileTagsMapper.toList(dbData);
  }
}
