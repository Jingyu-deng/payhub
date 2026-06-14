package com.payhub.core.http;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Declares an HTTP PUT method on a declarative HTTP API interface. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Put {

  /** The URL path, appended to the base URL. */
  String value() default "";
}
