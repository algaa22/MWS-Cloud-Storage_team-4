package com.mipt.team4.cloud_storage_backend.config;

import com.mipt.team4.cloud_storage_backend.config.sources.ConfigSource;
import com.mipt.team4.cloud_storage_backend.config.sources.EnvironmentConfigSource;

public class MinioConfig {
    private static volatile MinioConfig instance;

    private final String url;
    private final String username;
    private final String password;

    private MinioConfig(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public static MinioConfig getInstance() {
        if (instance == null) {
            synchronized (MinioConfig.class) {
                if (instance == null) {
                    ConfigSource source = new EnvironmentConfigSource();
                    instance =
                            new MinioConfig(
                                    source.getString("minio.url").orElseThrow(),
                                    source.getString("minio.username").orElseThrow(),
                                    source.getString("minio.password").orElseThrow());
                }
            }
        }
        return instance;
    }


    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
