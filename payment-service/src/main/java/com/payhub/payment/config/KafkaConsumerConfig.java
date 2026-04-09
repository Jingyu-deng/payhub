package com.payhub.payment.config;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@Slf4j
public class KafkaConsumerConfig {

  @Autowired private KafkaProperties kafkaProperties; // Spring Boot auto-configures this

  @Bean
  public ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory(
      ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
      ConsumerFactory<Object, Object> kafkaConsumerFactory,
      KafkaTemplate<Object, Object> kafkaTemplate) {

    ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    configurer.configure(factory, kafkaConsumerFactory);

    // Dead letter publishing recoverer: send failed records to topic + ".DLT"
    DeadLetterPublishingRecoverer recoverer =
        new DeadLetterPublishingRecoverer(
            kafkaTemplate,
            (record, exception) ->
                new org.apache.kafka.common.TopicPartition(record.topic() + ".DLT", -1));

    // Retry 3 times with 1 second interval, then send to DLQ
    DefaultErrorHandler errorHandler =
        new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3));

    factory.setCommonErrorHandler(errorHandler);

    return factory;
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, String> dltContainerFactory() {
    // 1. Configure consumer properties
    Map<String, Object> props = new HashMap<>();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "payment-group-dlt"); // unique group for DLQ
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    props.put(
        ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true"); // auto‑commit to avoid reprocessing

    // 2. Create consumer factory
    DefaultKafkaConsumerFactory<String, String> consumerFactory =
        new DefaultKafkaConsumerFactory<>(props);

    // 3. Create listener container factory
    ConcurrentKafkaListenerContainerFactory<String, String> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory);

    // 4. Error handler: only log, do NOT retry or send to DLQ
    factory.setCommonErrorHandler(
        new DefaultErrorHandler(
            (record, exception) -> {
              log.error("Failed to process DLQ message: {}", record, exception);
            },
            new FixedBackOff(0, 0))); // 0 retries, 0 delay

    return factory;
  }
}
