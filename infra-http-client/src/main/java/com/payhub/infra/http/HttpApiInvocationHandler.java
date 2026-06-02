package com.payhub.infra.http;

import com.payhub.core.exception.HttpApiException;
import com.payhub.core.http.Body;
import com.payhub.core.http.Get;
import com.payhub.core.http.Header;
import com.payhub.core.http.Post;
import com.payhub.core.http.Query;
import com.payhub.core.infra.HttpClient;
import com.payhub.core.utils.JsonUtils;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * {@link InvocationHandler} that owns the OkHttp client and translates annotated interface method
 * calls into real HTTP requests.
 *
 * <p>Package-private — callers obtain a proxy through {@link HttpClient#createHttpApi(Class,
 * String)}.
 */
@Slf4j
class HttpApiInvocationHandler implements InvocationHandler {

  private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
  private static final OkHttpClient CLIENT =
      new OkHttpClient.Builder()
          .connectTimeout(10, TimeUnit.SECONDS)
          .readTimeout(30, TimeUnit.SECONDS)
          .build();

  private final String baseUrl;

  HttpApiInvocationHandler(String baseUrl) {
    this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    // Handle java.lang.Object methods
    if (method.getDeclaringClass() == Object.class) {
      return switch (method.getName()) {
        case "toString" -> "HttpApiProxy[" + method.getDeclaringClass().getName() + "]";
        case "equals" -> proxy == args[0];
        case "hashCode" -> System.identityHashCode(proxy);
        default -> throw new UnsupportedOperationException(method.getName());
      };
    }

    // Determine HTTP method and path
    Get getAnno = method.getAnnotation(Get.class);
    Post postAnno = method.getAnnotation(Post.class);
    if (getAnno == null && postAnno == null) {
      throw new IllegalStateException(
          "@Get or @Post annotation is required on method: " + method.getName());
    }

    String httpMethod = getAnno != null ? "GET" : "POST";
    String path = getAnno != null ? getAnno.value() : postAnno.value();

    // Collect headers: class-level defaults first, then method-level overrides
    Map<String, String> headers = new LinkedHashMap<>();
    for (Header h : method.getDeclaringClass().getAnnotationsByType(Header.class)) {
      headers.put(h.name(), h.value());
    }
    for (Header h : method.getAnnotationsByType(Header.class)) {
      headers.put(h.name(), h.value());
    }

    // Process parameters: find @Body (serialize to JSON), @Query (build query string)
    String body = null;
    StringJoiner queryParams = new StringJoiner("&");
    if (args != null) {
      var parameters = method.getParameters();
      for (int i = 0; i < parameters.length; i++) {
        if (parameters[i].isAnnotationPresent(Body.class)) {
          if (body != null) {
            throw new IllegalStateException(
                "Multiple @Body parameters found on method: " + method.getName());
          }
          body = args[i] instanceof String ? (String) args[i] : JsonUtils.toJson(args[i]);
        }
        Query query = parameters[i].getAnnotation(Query.class);
        if (query != null) {
          queryParams.add(encode(query.value()) + "=" + encode(String.valueOf(args[i])));
        }
      }
    }

    // Build URL with optional query string
    String url = baseUrl + path;
    if (queryParams.length() > 0) {
      url += "?" + queryParams;
    }
    HttpClient.Response response;
    if ("GET".equals(httpMethod)) {
      response = get(url, headers);
    } else {
      response = post(url, headers, body);
    }

    // Handle return type
    Class<?> returnType = method.getReturnType();
    if (returnType == void.class || returnType == Void.class) {
      return null;
    }
    if (returnType == HttpClient.Response.class) {
      return response;
    }
    if (!response.is2xx()) {
      throw new HttpApiException(httpMethod, url, response.getStatusCode(), response.getBody());
    }
    if (returnType == String.class) {
      return response.getBody();
    }
    return JsonUtils.fromJson(response.getBody(), returnType);
  }

  HttpClient.Response post(String url, Map<String, String> headers, String body) {
    var builder = new Request.Builder().url(url);
    headers.forEach(builder::addHeader);
    if (body != null) {
      builder.post(RequestBody.create(body, JSON));
    }
    return execute(builder.build());
  }

  HttpClient.Response get(String url, Map<String, String> headers) {
    var builder = new Request.Builder().url(url);
    headers.forEach(builder::addHeader);
    return execute(builder.build());
  }

  private HttpClient.Response execute(Request request) {
    try (var response = CLIENT.newCall(request).execute()) {
      String body = response.body() != null ? response.body().string() : "";
      return new HttpClient.Response(response.code(), body);
    } catch (IOException e) {
      log.error("HTTP call failed: " + request.url() + " - " + e.getMessage());
      return new HttpClient.Response(503, "Upstream unavailable: " + e.getMessage());
    }
  }

  private static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }
}
