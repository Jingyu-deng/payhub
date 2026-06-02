package com.payhub.core.http;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares an HTTP header. When placed on the interface type it acts as a default for all methods;
 * when placed on a method it adds to or overrides the type-level headers.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Header {

  /** Header name (e.g. {@code "Content-Type"}). */
  String name();

  /** Header value (e.g. {@code "application/json"}). */
  String value();
}
