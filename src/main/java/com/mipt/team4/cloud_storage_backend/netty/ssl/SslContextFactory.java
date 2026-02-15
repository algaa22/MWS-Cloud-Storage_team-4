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
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SslContextFactory {

  public SslContext createFromResources()
      throws IOException,
          NoSuchAlgorithmException,
          UnrecoverableKeyException,
          KeyStoreException,
          CertificateException {
    try (InputStream p12Stream = FileLoader.getInputStream("ssl/server.p12")) {
      log.info("Loading SSL from PKCS12 file");

      KeyStore keyStore = KeyStore.getInstance("PKCS12");
      keyStore.load(p12Stream, "password".toCharArray()); // TODO: "password"?

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
