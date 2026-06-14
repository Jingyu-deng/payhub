package com.payhub.infra.monitor.config;

import com.payhub.infra.common.YamlPropertySourceFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/** Loads {@code monitor.yml} as a Spring property source. */
@Configuration
@PropertySource(value = "classpath:monitor.yml", factory = YamlPropertySourceFactory.class)
class MonitorConfig {}
