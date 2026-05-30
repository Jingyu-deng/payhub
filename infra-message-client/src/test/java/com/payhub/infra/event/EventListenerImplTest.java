package com.payhub.infra.event;

import static org.mockito.Mockito.*;

import com.payhub.core.controls.base.EventControl;
import com.payhub.core.domain.PaymentEvent;
import com.payhub.core.enums.PaymentStatus;
import com.payhub.core.infra.ControlClient;
import com.payhub.core.infra.EncryptionClient;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EventListenerImplTest {

  private EncryptionClient encryptionClient;
  private ControlClient controlClient;
  private EventListenerImpl listener;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    encryptionClient = mock(EncryptionClient.class);
    controlClient = mock(ControlClient.class);
    listener = new EventListenerImpl(encryptionClient, controlClient);
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldDecryptDeserializeAndDispatch() {
    String encryptedPayload = "encrypted";
    String json =
        "{\"type\":\"COMPLETED\",\"payment\":{\"id\":\"pay-1\",\"orderId\":\"ord-1\",\"status\":\"COMPLETED\"},\"timestamp\":1717000000000}";

    when(encryptionClient.decrypt(encryptedPayload)).thenReturn(json);

    EventControl<PaymentEvent> mockControl = mock(EventControl.class);
    when(controlClient.getEventControls(PaymentStatus.COMPLETED)).thenReturn(List.of(mockControl));

    listener.onMessage(encryptedPayload);

    verify(encryptionClient).decrypt(encryptedPayload);
    verify(controlClient).getEventControls(PaymentStatus.COMPLETED);
    verify(mockControl).execute(any(PaymentEvent.class));
  }

  @Test
  void shouldHandleNoMatchingControls() {
    String encryptedPayload = "encrypted";
    String json =
        "{\"type\":\"INITIATED\",\"payment\":{\"id\":\"pay-2\",\"orderId\":\"ord-2\",\"status\":\"INITIATED\"},\"timestamp\":1717000000000}";

    when(encryptionClient.decrypt(encryptedPayload)).thenReturn(json);
    when(controlClient.getEventControls(PaymentStatus.INITIATED)).thenReturn(List.of());

    listener.onMessage(encryptedPayload);

    verify(encryptionClient).decrypt(encryptedPayload);
    verify(controlClient).getEventControls(PaymentStatus.INITIATED);
  }
}
