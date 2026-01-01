package com.mipt.team4.cloud_storage_backend.netty.ssl;

import com.mipt.team4.cloud_storage_backend.utils.FileLoader;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.IdentityCipherSuiteFilter;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import javax.net.ssl.KeyManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SslContextFactory {

  private static final Logger logger = LoggerFactory.getLogger(SslContextFactory.class);

  public static SslContext createFromResources()
      throws IOException,
      NoSuchAlgorithmException,
      UnrecoverableKeyException,
      KeyStoreException,
      CertificateException {
    try (InputStream p12Stream = FileLoader.getInputStream("ssl/server.p12")) {
      logger.info("Loading SSL from PKCS12 file");

      KeyStore keyStore = KeyStore.getInstance("PKCS12");
      keyStore.load(p12Stream, "password".toCharArray());

      KeyManagerFactory kmf =
          KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      kmf.init(keyStore, "password".toCharArray());

      return SslContextBuilder.forServer(kmf)
          .sslProvider(SslProvider.OPENSSL_REFCNT)
          .ciphers(null, IdentityCipherSuiteFilter.INSTANCE)
          .applicationProtocolConfig(
              new ApplicationProtocolConfig(
                  ApplicationProtocolConfig.Protocol.ALPN,
                  ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                  ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                  ApplicationProtocolNames.HTTP_2,
                  ApplicationProtocolNames.HTTP_1_1))
          .build();
    }
  }
}
