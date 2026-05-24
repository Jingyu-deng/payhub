package com.payhub.core.infra;

import java.util.Map;

/**
 * Infrastructure interface for HTTP calls to payment gateways. Implementations live in the infra
 * module (OkHttp, RestTemplate, etc.).
 */
public interface HttpClient {

  Response post(String url, Map<String, String> headers, String body);

  Response get(String url, Map<String, String> headers);

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
