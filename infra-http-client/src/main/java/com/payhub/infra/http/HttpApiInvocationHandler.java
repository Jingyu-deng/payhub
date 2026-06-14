package com.payhub.infra.http;

import com.payhub.core.exception.HttpApiException;
import com.payhub.core.http.Body;
import com.payhub.core.http.Delete;
import com.payhub.core.http.Get;
import com.payhub.core.http.Header;
import com.payhub.core.http.Patch;
import com.payhub.core.http.Post;
import com.payhub.core.http.Put;
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

  private static final MediaType DEFAULT_CONTENT_TYPE =
      MediaType.get("application/json; charset=utf-8");
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
    if (method.getDeclaringClass() == Object.class) {
      return handleObjectMethod(proxy, method, args);
    }

    HttpMethod httpMethod = resolveHttpMethod(method);
    Map<String, String> headers = collectHeaders(method);
    ParameterResult params = processParameters(method, args);
    MediaType contentType = resolveContentType(headers);

    String url = buildUrl(httpMethod.path, params.queryString);
    HttpClient.Response response = execute(httpMethod.verb, url, headers, params.body, contentType);

    return handleReturnValue(method, response, httpMethod.verb, url);
  }

  // ── Object method delegation ──────────────────────────────────────────

  private Object handleObjectMethod(Object proxy, Method method, Object[] args) {
    return switch (method.getName()) {
      case "toString" -> "HttpApiProxy[" + method.getDeclaringClass().getName() + "]";
      case "equals" -> proxy == args[0];
      case "hashCode" -> System.identityHashCode(proxy);
      default -> throw new UnsupportedOperationException(method.getName());
    };
  }

  // ── Annotation parsing ────────────────────────────────────────────────

  private record HttpMethod(String verb, String path) {}

  private HttpMethod resolveHttpMethod(Method method) {
    if (method.isAnnotationPresent(Get.class)) {
      return new HttpMethod("GET", method.getAnnotation(Get.class).value());
    }
    if (method.isAnnotationPresent(Post.class)) {
      return new HttpMethod("POST", method.getAnnotation(Post.class).value());
    }
    if (method.isAnnotationPresent(Put.class)) {
      return new HttpMethod("PUT", method.getAnnotation(Put.class).value());
    }
    if (method.isAnnotationPresent(Patch.class)) {
      return new HttpMethod("PATCH", method.getAnnotation(Patch.class).value());
    }
    if (method.isAnnotationPresent(Delete.class)) {
      return new HttpMethod("DELETE", method.getAnnotation(Delete.class).value());
    }
    throw new IllegalStateException(
        "@Get, @Post, @Put, @Patch, or @Delete annotation is required on method: "
            + method.getName());
  }

  private Map<String, String> collectHeaders(Method method) {
    Map<String, String> headers = new LinkedHashMap<>();
    for (Header h : method.getDeclaringClass().getAnnotationsByType(Header.class)) {
      headers.put(h.name(), h.value());
    }
    for (Header h : method.getAnnotationsByType(Header.class)) {
      headers.put(h.name(), h.value());
    }
    return headers;
  }

  private record ParameterResult(String body, String queryString) {}

  private ParameterResult processParameters(Method method, Object[] args) {
    String body = null;
    StringJoiner queryParams = new StringJoiner("&");
    if (args == null) {
      return new ParameterResult(null, "");
    }
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
    return new ParameterResult(body, queryParams.toString());
  }

  // ── Content-type resolution ───────────────────────────────────────────

  private MediaType resolveContentType(Map<String, String> headers) {
    String declaredContentType = headers.remove("Content-Type");
    if (declaredContentType != null) {
      return MediaType.get(declaredContentType);
    }
    return DEFAULT_CONTENT_TYPE;
  }

  // ── URL construction ──────────────────────────────────────────────────

  private String buildUrl(String path, String queryString) {
    if (queryString.isEmpty()) {
      return baseUrl + path;
    }
    return baseUrl + path + "?" + queryString;
  }

  // ── Return value handling ─────────────────────────────────────────────

  private Object handleReturnValue(
      Method method, HttpClient.Response response, String httpMethod, String url) {
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

  private HttpClient.Response execute(
      String httpMethod,
      String url,
      Map<String, String> headers,
      String body,
      MediaType contentType) {
    var builder = new Request.Builder().url(url);
    headers.forEach(builder::addHeader);
    if (body != null) {
      builder.method(httpMethod, RequestBody.create(body, contentType));
    } else {
      builder.method(httpMethod, null);
    }
    return call(builder.build());
  }

  private HttpClient.Response call(Request request) {
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
