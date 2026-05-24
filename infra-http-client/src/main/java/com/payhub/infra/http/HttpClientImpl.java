package com.payhub.infra.http;

import com.payhub.core.infra.HttpClient;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class HttpClientImpl implements HttpClient {
  private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

  private final OkHttpClient client;

  public HttpClientImpl() {
    this.client =
        new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();
  }

  @Override
  public Response post(String url, Map<String, String> headers, String body) {
    var builder = new Request.Builder().url(url);
    headers.forEach(builder::addHeader);
    if (body != null) {
      builder.post(RequestBody.create(body, JSON));
    }
    return execute(builder.build());
  }

  @Override
  public Response get(String url, Map<String, String> headers) {
    var builder = new Request.Builder().url(url);
    headers.forEach(builder::addHeader);
    return execute(builder.build());
  }

  private Response execute(Request request) {
    try (var response = client.newCall(request).execute()) {
      String body = response.body() != null ? response.body().string() : "";
      return new Response(response.code(), body);
    } catch (IOException e) {
      log.error("HTTP call failed: " + request.url() + " - " + e.getMessage());
      return new Response(503, "Upstream unavailable: " + e.getMessage());
    }
  }
}
