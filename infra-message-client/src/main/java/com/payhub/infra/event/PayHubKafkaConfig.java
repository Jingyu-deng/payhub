package com.payhub.infra.event;

import com.payhub.infra.common.YamlPropertySourceFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = "classpath:kafka.yml", factory = YamlPropertySourceFactory.class)
public class PayHubKafkaConfig {}
