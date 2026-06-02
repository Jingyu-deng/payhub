package com.payhub.core.http;

import com.payhub.core.infra.HttpClient;

/**
 * Generic declarative API for dynamic HTTP calls. Annotated so it can be passed directly to {@link
 * HttpClient#createHttpApi(Class, String)} — the base URL becomes the full target URL and the
 * {@code @Body} parameter is sent as the request body.
 */
@Header(name = "Content-Type", value = "application/json")
public interface DynamicHttpApi {

  @Post("")
  HttpClient.Response post(@Body String body);
}
