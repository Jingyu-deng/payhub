package com.payhub.infra.message.serde;

import com.payhub.core.infra.EncryptionClient;
import com.payhub.infra.message.SpringContextHolder;
import java.util.Map;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

/**
 * Kafka value {@link Deserializer} that decrypts the payload after delegating to {@link
 * StringDeserializer}.
 */
public class EncryptingValueDeserializer implements Deserializer<String> {

  private volatile EncryptionClient encryptionClient;
  private final StringDeserializer delegate = new StringDeserializer();

  /** No-arg constructor for Kafka instantiation from configuration. */
  public EncryptingValueDeserializer() {}

  @Override
  public void configure(Map<String, ?> configs, boolean isKey) {
    delegate.configure(configs, isKey);
  }

  @Override
  public String deserialize(String topic, byte[] data) {
    if (data == null) {
      return null;
    }
    String encrypted = delegate.deserialize(topic, data);
    return encryptionClient().decrypt(encrypted);
  }

  @Override
  public void close() {
    delegate.close();
  }

  private EncryptionClient encryptionClient() {
    if (encryptionClient == null) {
      encryptionClient = SpringContextHolder.getBean(EncryptionClient.class);
    }
    return encryptionClient;
  }
}
