package com.mipt.team4.cloud_storage_backend.config.sources;

import com.mipt.team4.cloud_storage_backend.exception.utils.InputStreamNotFoundException;
import com.mipt.team4.cloud_storage_backend.utils.FileLoader;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnvironmentConfigSource extends ConfigSource {
  private static final Logger logger = LoggerFactory.getLogger(EnvironmentConfigSource.class);
  Map<String, String> envVars = new HashMap<>();

  public EnvironmentConfigSource(String filePath) {
    loadEnvFile(filePath);
  }

  @Override
  public Optional<String> getString(String key) {
    key = convertToEnvVarName(key);

    String systemValue = System.getenv(key);
    if (systemValue != null) return Optional.of(systemValue);

    return Optional.ofNullable(envVars.get(key));
  }

  private String convertToEnvVarName(String key) {
    return key.toUpperCase().replace('.', '_').replace('-', '_');
  }

  private void loadEnvFile(String filePath) {
    try (InputStream inputStream = FileLoader.getInputStream(filePath);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
      String line;

      while ((line = reader.readLine()) != null) {
        line = line.trim();

        if (line.isEmpty() || line.startsWith("#")) {
          continue;
        }

        String[] parts = line.split("=");

        if (parts.length == 2) {
          String key = parts[0].trim();
          String value = parts[1].trim().replaceAll("(^[\"'])|([\"']$)", "");

          envVars.put(key, value);
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    } catch (InputStreamNotFoundException _) {
      logger.warn(".env file not found: path={}", filePath);
    }
  }
}
