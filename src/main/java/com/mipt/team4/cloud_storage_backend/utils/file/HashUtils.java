package com.mipt.team4.cloud_storage_backend.utils.file;

import com.mipt.team4.cloud_storage_backend.exception.upload.ChecksumMismatchException;
import com.mipt.team4.cloud_storage_backend.exception.utils.UnknownChecksumAlgorithmException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class HashUtils {
  private static final String SHA256_NAME = "SHA-256";

  public static MessageDigest createSha256() {
    try {
      return MessageDigest.getInstance(SHA256_NAME);
    } catch (NoSuchAlgorithmException e) {
      throw new UnknownChecksumAlgorithmException(e);
    }
  }

  public static String calculateSha256(byte[] data) {
    MessageDigest messageDigest = HashUtils.createSha256();
    return encodeDigest(messageDigest.digest(data));
  }

  public static void compareChecksums(String clientChecksum, byte[] serverDigest) {
    String serverChecksum = encodeDigest(serverDigest);

    if (!clientChecksum.equals(serverChecksum)) {
      throw new ChecksumMismatchException();
    }
  }

  public static void update(MessageDigest digest, String hash) {
    digest.update(decodeHash(hash));
  }

  public static String encodeDigest(byte[] digest) {
    return HexFormat.of().formatHex(digest);
  }

  public static byte[] decodeHash(String hash) {
    return HexFormat.of().parseHex(hash);
  }
}
