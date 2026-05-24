package com.payhub.core.adapters;

import com.payhub.core.infra.HttpClient;
import lombok.Getter;
import lombok.Setter;

/**
 * Base class for adapters whose {@link HttpClient} dependency is injected via setter by {@code
 * AdapterClientImpl} at load time — the adapter-side parallel of {@link
 * com.payhub.core.controls.ControlInjector}. Concrete adapters extend this and implement whichever
 * adapter SPI interfaces they support.
 */
@Getter
@Setter
public abstract class AdapterInjector {

  HttpClient httpClient;
}
