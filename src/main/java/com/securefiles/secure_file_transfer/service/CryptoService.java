package com.securefiles.secure_file_transfer.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class CryptoService {

  private final SecretKeySpec masterKey;
  private final SecureRandom rng = new SecureRandom();

  public CryptoService(@Value("${app.crypto.masterKeyBase64}") String keyB64) {
    byte[] k = Base64.getDecoder().decode(keyB64);
    if (k.length != 16 && k.length != 24 && k.length != 32) {
      throw new IllegalArgumentException("AES key must be 16/24/32 bytes after Base64 decode");
    }
    this.masterKey = new SecretKeySpec(k, "AES");
  }

  public String encryptToFile(InputStream plaintext, Path outCipherFile) throws Exception {
    byte[] iv = generateIv();

    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.ENCRYPT_MODE, masterKey, new GCMParameterSpec(128, iv));

    Files.createDirectories(outCipherFile.getParent());

    try (InputStream in = plaintext; OutputStream out = Files.newOutputStream(outCipherFile)) {
      byte[] buffer = new byte[8192];
      int read;

      while ((read = in.read(buffer)) != -1) {
        byte[] chunk = cipher.update(buffer, 0, read);
        if (chunk != null && chunk.length > 0) {
          out.write(chunk);
        }
      }

      byte[] finalBytes = cipher.doFinal();
      if (finalBytes != null && finalBytes.length > 0) {
        out.write(finalBytes);
      }
    }

    return Base64.getEncoder().encodeToString(iv);
  }

  public void decryptToStream(Path cipherFile, String ivBase64, OutputStream out) throws Exception {
    byte[] iv = decodeAndValidateIv(ivBase64);

    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.DECRYPT_MODE, masterKey, new GCMParameterSpec(128, iv));

    try (InputStream in = Files.newInputStream(cipherFile)) {
      byte[] buffer = new byte[8192];
      int read;

      while ((read = in.read(buffer)) != -1) {
        byte[] chunk = cipher.update(buffer, 0, read);
        if (chunk != null && chunk.length > 0) {
          out.write(chunk);
        }
      }

      byte[] finalBytes = cipher.doFinal();
      if (finalBytes != null && finalBytes.length > 0) {
        out.write(finalBytes);
      }
      out.flush();
    }
  }

  private byte[] generateIv() {
    byte[] iv = new byte[12];
    rng.nextBytes(iv);
    return iv;
  }

  private byte[] decodeAndValidateIv(String ivBase64) {
    byte[] iv = Base64.getDecoder().decode(ivBase64);
    if (iv.length != 12) {
      throw new IllegalArgumentException("Invalid AES-GCM IV length");
    }
    return iv;
  }
}