package com.payhub.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("com.payhub")
public class PaymentPlatformApplication {

  public static void main(String[] args) {
    SpringApplication.run(PaymentPlatformApplication.class, args);
  }
}
