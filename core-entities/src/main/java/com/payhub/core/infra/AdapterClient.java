package com.payhub.core.infra;

import com.payhub.core.adapters.Adapter;
import java.util.List;

/**
 * Port for obtaining adapter instances. Looks up SPI-registered implementations by name or type.
 */
public interface AdapterClient {

  Adapter getAdapter(String gatewayName);

  List<Adapter> getAllAdapters();
}
