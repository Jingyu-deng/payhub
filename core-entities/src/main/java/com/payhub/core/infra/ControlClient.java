package com.payhub.core.infra;

import com.payhub.core.controls.base.Control;
import com.payhub.core.controls.base.EventControl;
import com.payhub.core.domain.PaymentEvent;
import com.payhub.core.enums.PaymentStatus;
import java.util.List;

/**
 * Port for obtaining {@link Control} control instances. Looks up the SPI-registered implementation
 * for the requested control type and returns a fully-wired instance.
 *
 * <p>Usage: {@code controlClient.getControl(CreateControl.class)}
 */
public interface ControlClient {

  <T extends Control<?, ?>> T getControl(Class<T> controlType);

  List<EventControl<PaymentEvent>> getEventControls(PaymentStatus eventType);
}
