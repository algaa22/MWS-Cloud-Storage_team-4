package com.mipt.team4.cloud_storage_backend.base.extensions;

import com.mipt.team4.cloud_storage_backend.utils.TestUtils;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.testcontainers.containers.GenericContainer;

public class S3Extension implements BeforeAllCallback {
  public static final GenericContainer<?> S3 = TestUtils.createS3Container();

  @Override
  public void beforeAll(ExtensionContext context) {
    context
        .getRoot()
        .getStore(Namespace.GLOBAL)
        .getOrComputeIfAbsent(
            "s3_init",
            key -> {
              S3.start();

              System.setProperty(
                  "storage.s3.url",
                  "http://" + S3Extension.S3.getHost() + ":" + S3Extension.S3.getMappedPort(8333));
              System.setProperty("storage.s3.access-key", "test-key");
              System.setProperty("storage.s3.secret-key", "test-secret");
              System.setProperty("storage.s3.user-data-bucket.name", "my-test-bucket");

              return (ExtensionContext.Store.CloseableResource) S3::stop;
            });
  }
}
