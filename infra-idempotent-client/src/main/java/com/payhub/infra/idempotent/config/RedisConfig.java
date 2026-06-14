package com.payhub.infra.idempotent.config;

import com.payhub.infra.common.YamlPropertySourceFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/** Loads {@code redis.yml} as a Spring property source. */
@Configuration
@PropertySource(value = "classpath:redis.yml", factory = YamlPropertySourceFactory.class)
class RedisConfig {}
