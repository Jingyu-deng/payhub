package com.payhub.infra.message.serde;

import com.payhub.core.infra.EncryptionClient;
import com.payhub.infra.message.SpringContextHolder;
import java.util.Map;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;

/**
 * Kafka value {@link Serializer} that encrypts the payload before delegating to {@link
 * StringSerializer}.
 */
public class EncryptingValueSerializer implements Serializer<String> {

  private volatile EncryptionClient encryptionClient;
  private final StringSerializer delegate = new StringSerializer();

  /** No-arg constructor for Kafka instantiation from configuration. */
  public EncryptingValueSerializer() {}

  @Override
  public void configure(Map<String, ?> configs, boolean isKey) {
    delegate.configure(configs, isKey);
  }

  @Override
  public byte[] serialize(String topic, String data) {
    if (data == null) {
      return null;
    }
    String encrypted = encryptionClient().encrypt(data);
    return delegate.serialize(topic, encrypted);
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
