package com.mipt.team4.cloud_storage_backend.utils;

import com.mipt.team4.cloud_storage_backend.exception.utils.InputStreamNotFoundException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class FileLoader {

  public static InputStream getInputStream(String filePath) {
    InputStream classPathStream = FileLoader.class.getClassLoader().getResourceAsStream(filePath);
    if (classPathStream != null) {
      return classPathStream;
    }

    try {
      return new FileInputStream(filePath);
    } catch (FileNotFoundException e) {
      throw new InputStreamNotFoundException(filePath);
    }
  }
}
