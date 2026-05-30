package com.payhub.core.infra;

/** Infrastructure interface for encrypting/decrypting data at rest and in transit. */
public interface EncryptionClient {

  String encrypt(String plaintext);

  String decrypt(String ciphertext);
}
