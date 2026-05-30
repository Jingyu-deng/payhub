package com.payhub.infra.event;

import com.payhub.core.infra.EncryptionClient;
import org.springframework.stereotype.Component;

/** No-op stub — real encryption (AES/KMS) comes later. */
@Component
public class EncryptionClientImpl implements EncryptionClient {

  @Override
  public String encrypt(String plaintext) {
    return plaintext;
  }

  @Override
  public String decrypt(String ciphertext) {
    return ciphertext;
  }
}
