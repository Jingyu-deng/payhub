package com.payhub.infra.runtime;

import com.payhub.core.adapters.Adapter;
import com.payhub.core.adapters.AdapterInjector;
import com.payhub.core.enums.PaymentGateway;
import com.payhub.core.infra.AdapterClient;
import com.payhub.core.infra.HttpClient;
import java.util.List;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class AdapterClientImpl implements AdapterClient {

  private final HttpClient httpClient;
  private final ApplicationContext applicationContext;

  public AdapterClientImpl(HttpClient httpClient, ApplicationContext applicationContext) {
    this.httpClient = httpClient;
    this.applicationContext = applicationContext;
  }

  @Override
  public Adapter getAdapter(PaymentGateway gatewayName) {
    var beans = applicationContext.getBeansOfType(Adapter.class);
    Adapter adapter =
        beans.values().stream()
            .filter(a -> a.getGateway() == gatewayName)
            .findFirst()
            .orElseThrow(
                () -> new IllegalArgumentException("No Adapter found for name: " + gatewayName));

    injectHttpClient(adapter);
    return adapter;
  }

  @Override
  public List<Adapter> getAllAdapters() {
    return applicationContext.getBeansOfType(Adapter.class).values().stream()
        .peek(this::injectHttpClient)
        .toList();
  }

  private void injectHttpClient(Object adapter) {
    if (adapter instanceof AdapterInjector injector) {
      injector.setHttpClient(httpClient);
    }
  }
}
