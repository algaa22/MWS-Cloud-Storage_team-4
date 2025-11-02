package com.mipt.team4.cloud_storage_backend.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class EnvironmentConfigSource extends ConfigSource {
  Map<String, String> envVars = new HashMap<>();

  public EnvironmentConfigSource() {
    loadEnvFile(".env");
  }

  @Override
  public Optional<String> getString(String key) {
    String systemValue = System.getenv(key);

    if (systemValue != null) {
      return Optional.of(systemValue);
    }

    return Optional.ofNullable(envVars.get(key));
  }

  private String convertToEnvVarName(String path) {
    return path.toUpperCase()
            .replace('.', '_')
            .replace('-', '_');
  }

  private void loadEnvFile(String filePath) {
    Path envPath = Paths.get(filePath);

    if (!Files.exists(envPath)) {
      System.err.println("Error: .env file not found: " + envPath.toAbsolutePath());
      return;
    }

    try {
      List<String> lines = Files.readAllLines(envPath);

      for (String line : lines) {
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
      throw new RuntimeException("Failed to load a file with path " + envPath.toAbsolutePath(), e);
    }
  }
}
