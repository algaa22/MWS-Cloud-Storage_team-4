package com.mipt.team4.cloud_storage_backend.utils;

import com.mipt.team4.cloud_storage_backend.exception.transfer.ChecksumMismatchException;
import com.mipt.team4.cloud_storage_backend.exception.utils.UnknownChecksumAlgorithmException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class ChecksumUtils {
  private static final String MD5_NAME = "MD5";

  public static MessageDigest createMD5() {
    try {
      return MessageDigest.getInstance(MD5_NAME);
    } catch (NoSuchAlgorithmException e) {
      throw new UnknownChecksumAlgorithmException(e);
    }
  }

  public static void compareChecksums(String clientChecksum, byte[] serverDigest) {
    String serverChecksum = encodeDigest(serverDigest);

    if (!clientChecksum.equals(serverChecksum)) {
      throw new ChecksumMismatchException();
    }
  }

  private static String encodeDigest(byte[] digest) {
    return HexFormat.of().formatHex(digest);
  }
}
