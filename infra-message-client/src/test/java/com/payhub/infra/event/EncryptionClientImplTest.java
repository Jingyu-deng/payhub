package com.payhub.infra.event;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EncryptionClientImplTest {

  private EncryptionClientImpl encryptionClient;

  @BeforeEach
  void setUp() {
    encryptionClient = new EncryptionClientImpl();
  }

  @Test
  void shouldEncryptAndDecryptAsIdentity() {
    String plaintext = "{\"type\":\"COMPLETED\",\"payment\":{\"id\":\"pay-123\"}}";

    String encrypted = encryptionClient.encrypt(plaintext);
    String decrypted = encryptionClient.decrypt(encrypted);

    assertEquals(plaintext, encrypted);
    assertEquals(plaintext, decrypted);
  }

  @Test
  void shouldHandleEmptyString() {
    assertEquals("", encryptionClient.encrypt(""));
    assertEquals("", encryptionClient.decrypt(""));
  }
}
