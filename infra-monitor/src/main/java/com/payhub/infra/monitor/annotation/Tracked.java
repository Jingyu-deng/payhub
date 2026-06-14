package com.payhub.infra.monitor.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Marks a method for business-metric tracking with timing. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Tracked {

  /** Logical operation name — e.g. {@code "initiate"} or {@code "process"}. */
  String operation();
}
