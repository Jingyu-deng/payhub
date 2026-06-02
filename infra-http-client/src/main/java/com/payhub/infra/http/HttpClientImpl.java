package com.payhub.infra.http;

import com.payhub.core.infra.HttpClient;
import java.lang.reflect.Proxy;
import org.springframework.stereotype.Component;

/**
 * Thin factory that creates declarative HTTP API proxies. All HTTP execution lives in {@link
 * HttpApiInvocationHandler}.
 */
@Component
public class HttpClientImpl implements HttpClient {

  @Override
  @SuppressWarnings("unchecked")
  public <T> T createHttpApi(Class<T> apiInterface, String baseUrl) {
    if (!apiInterface.isInterface()) {
      throw new IllegalArgumentException(apiInterface.getName() + " must be an interface");
    }
    return (T)
        Proxy.newProxyInstance(
            apiInterface.getClassLoader(),
            new Class<?>[] {apiInterface},
            new HttpApiInvocationHandler(baseUrl));
  }
}
