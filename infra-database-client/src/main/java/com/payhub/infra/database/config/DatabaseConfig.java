package com.payhub.infra.database.config;

import com.payhub.infra.common.YamlPropertySourceFactory;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/** Loads {@code database.yml} as a Spring property source and enables JPA scanning. */
@Configuration
@PropertySource(value = "classpath:database.yml", factory = YamlPropertySourceFactory.class)
@EntityScan(basePackages = "com.payhub.infra.database")
@EnableJpaRepositories(basePackages = "com.payhub.infra.database")
class DatabaseConfig {}
