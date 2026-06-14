package com.payhub.infra.encryption;

import com.payhub.core.infra.EncryptionClient;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** AES-256-GCM implementation. 12-byte random IV prepended to ciphertext, Base64 encoded. */
@Component
public class EncryptionClientImpl implements EncryptionClient {

  private static final String ALGORITHM = "AES/GCM/NoPadding";
  private static final int GCM_IV_LENGTH = 12;
  private static final int GCM_TAG_LENGTH = 128;
  private static final int KEY_LENGTH = 32;

  private final SecretKeySpec key;

  public EncryptionClientImpl(@Value("${payhub.encryption.key}") String base64Key) {
    byte[] decoded = Base64.getDecoder().decode(base64Key);
    if (decoded.length != KEY_LENGTH) {
      throw new IllegalArgumentException(
          "Encryption key must be 32 bytes (AES-256), got " + decoded.length);
    }
    this.key = new SecretKeySpec(decoded, "AES");
  }

  @Override
  public String encrypt(String plaintext) {
    try {
      byte[] iv = new byte[GCM_IV_LENGTH];
      new SecureRandom().nextBytes(iv);

      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

      byte[] ciphertext =
          cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));

      // Prepend IV to ciphertext: [12 bytes IV][N bytes ciphertext]
      byte[] combined = new byte[GCM_IV_LENGTH + ciphertext.length];
      System.arraycopy(iv, 0, combined, 0, GCM_IV_LENGTH);
      System.arraycopy(ciphertext, 0, combined, GCM_IV_LENGTH, ciphertext.length);

      return Base64.getEncoder().encodeToString(combined);
    } catch (Exception e) {
      throw new RuntimeException("Encryption failed", e);
    }
  }

  @Override
  public String decrypt(String ciphertext) {
    try {
      byte[] combined = Base64.getDecoder().decode(ciphertext);

      // Extract IV (first 12 bytes)
      byte[] iv = new byte[GCM_IV_LENGTH];
      System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);

      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

      byte[] plaintext = cipher.doFinal(combined, GCM_IV_LENGTH, combined.length - GCM_IV_LENGTH);

      return new String(plaintext, java.nio.charset.StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new RuntimeException("Decryption failed", e);
    }
  }
}
