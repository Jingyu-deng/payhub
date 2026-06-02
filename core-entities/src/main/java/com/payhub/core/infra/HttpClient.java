package com.payhub.core.infra;

/**
 * Infrastructure port for declarative HTTP calls. Adapters and controls define annotated interfaces
 * describing an HTTP API contract, then call {@link #createHttpApi(Class, String)} to obtain a
 * dynamic proxy that turns method invocations into real HTTP requests.
 *
 * <p>Implementations live in the infra module (e.g. {@code infra-http-client} via OkHttp).
 */
public interface HttpClient {

  /**
   * Creates a dynamic proxy that implements {@code apiInterface}. Each annotated method on the
   * interface becomes an HTTP call whose URL is {@code baseUrl + path}. Annotations are read from
   * {@code com.payhub.core.http}.
   *
   * @param apiInterface annotated interface describing the HTTP API
   * @param baseUrl protocol + host (e.g. {@code "https://openapi.alipay.com"})
   * @param <T> the API interface type
   * @return a proxy instance
   */
  <T> T createHttpApi(Class<T> apiInterface, String baseUrl);

  /** HTTP response value object. */
  class Response {
    private final int statusCode;
    private final String body;

    public Response(int statusCode, String body) {
      this.statusCode = statusCode;
      this.body = body;
    }

    public int getStatusCode() {
      return statusCode;
    }

    public String getBody() {
      return body;
    }

    public boolean is2xx() {
      return statusCode >= 200 && statusCode < 300;
    }
  }
}
