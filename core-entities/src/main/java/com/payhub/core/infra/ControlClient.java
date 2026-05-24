package com.payhub.core.infra;

import com.payhub.core.controls.Control;

/**
 * Port for obtaining {@link Control} control instances. Looks up the SPI-registered implementation
 * for the requested control type and returns a fully-wired instance.
 *
 * <p>Usage: {@code controlClient.getControl(CreateControl.class)}
 */
public interface ControlClient {

  <T extends Control<?, ?>> T getControl(Class<T> controlType);
}
