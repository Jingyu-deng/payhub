package com.payhub.infra.encryption;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EncryptionClientImplTest {

  // 32-byte key for AES-256, Base64-encoded
  private static final String BASE64_KEY =
      Base64.getEncoder()
          .encodeToString("payhub-aes-256-gcm-key-32bytes!!".getBytes(StandardCharsets.UTF_8));

  private EncryptionClientImpl encryptionClient;

  @BeforeEach
  void setUp() {
    encryptionClient = new EncryptionClientImpl(BASE64_KEY);
  }

  @Test
  void shouldEncryptAndDecryptRoundTrip() {
    String plaintext = "{\"type\":\"COMPLETED\",\"payment\":{\"id\":\"pay-123\"}}";

    String encrypted = encryptionClient.encrypt(plaintext);
    String decrypted = encryptionClient.decrypt(encrypted);

    assertNotEquals(plaintext, encrypted, "Encrypted output should differ from plaintext");
    assertEquals(plaintext, decrypted);
  }

  @Test
  void shouldProduceDifferentCiphertextForSamePlaintext() {
    String plaintext = "same payload";

    String enc1 = encryptionClient.encrypt(plaintext);
    String enc2 = encryptionClient.encrypt(plaintext);

    assertNotEquals(enc1, enc2, "Different IVs should produce different ciphertexts");
    assertEquals(plaintext, encryptionClient.decrypt(enc1));
    assertEquals(plaintext, encryptionClient.decrypt(enc2));
  }

  @Test
  void shouldHandleEmptyString() {
    String encrypted = encryptionClient.encrypt("");
    assertEquals("", encryptionClient.decrypt(encrypted));
  }

  @Test
  void shouldHandleUnicodeString() {
    String plaintext = "支付平台 — PayHub 支付处理系统 ¥€$";

    String encrypted = encryptionClient.encrypt(plaintext);
    String decrypted = encryptionClient.decrypt(encrypted);

    assertEquals(plaintext, decrypted);
  }

  @Test
  void shouldHandleLongPayload() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 1000; i++) {
      sb.append("{\"id\":\"").append(i).append("\",\"amount\":").append(i * 100).append("},");
    }
    String plaintext = sb.toString();

    String encrypted = encryptionClient.encrypt(plaintext);
    String decrypted = encryptionClient.decrypt(encrypted);

    assertEquals(plaintext, decrypted);
  }

  @Test
  void shouldThrowOnTamperedCiphertext() {
    String plaintext = "tamper me";
    String encrypted = encryptionClient.encrypt(plaintext);

    byte[] bytes = Base64.getDecoder().decode(encrypted);
    // Flip a bit in the ciphertext portion (after the 12-byte IV)
    bytes[15] ^= 0x01;
    String tampered = Base64.getEncoder().encodeToString(bytes);

    assertThrows(RuntimeException.class, () -> encryptionClient.decrypt(tampered));
  }

  @Test
  void shouldRejectInvalidBase64() {
    assertThrows(RuntimeException.class, () -> encryptionClient.decrypt("not-valid-base64!!!"));
  }

  @Test
  void shouldRejectShortKey() {
    String shortKey = Base64.getEncoder().encodeToString("short".getBytes(StandardCharsets.UTF_8));
    assertThrows(IllegalArgumentException.class, () -> new EncryptionClientImpl(shortKey));
  }
}
