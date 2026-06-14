package com.payhub.infra.encryption.config;

import com.payhub.infra.common.YamlPropertySourceFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/** Loads {@code encryption.yml} as a Spring property source. */
@Configuration
@PropertySource(value = "classpath:encryption.yml", factory = YamlPropertySourceFactory.class)
class EncryptionConfig {}
