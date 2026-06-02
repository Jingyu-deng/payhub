package com.payhub.core.exception;

/**
 * Thrown by the declarative HTTP proxy when a non-2xx response is received and the API method's
 * return type requires automatic deserialization (i.e. the return type is not {@link
 * com.payhub.core.infra.HttpClient.Response}).
 */
public class HttpApiException extends RuntimeException {

  private final int statusCode;
  private final String responseBody;

  public HttpApiException(String httpMethod, String url, int statusCode, String responseBody) {
    super(httpMethod + " " + url + " returned " + statusCode + ": " + responseBody);
    this.statusCode = statusCode;
    this.responseBody = responseBody;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public String getResponseBody() {
    return responseBody;
  }
}
