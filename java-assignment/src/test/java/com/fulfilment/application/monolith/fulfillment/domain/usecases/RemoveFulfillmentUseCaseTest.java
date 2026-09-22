package com.fulfilment.application.monolith.fulfillment.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.exceptions.ResourceNotFoundException;
import com.fulfilment.application.monolith.fulfillment.domain.events.FulfillmentChangedEvent;
import com.fulfilment.application.monolith.fulfillment.domain.models.Fulfillment;
import com.fulfilment.application.monolith.fulfillment.domain.models.ProductReference;
import com.fulfilment.application.monolith.fulfillment.domain.models.StoreReference;
import com.fulfilment.application.monolith.fulfillment.domain.ports.FulfillmentStore;
import jakarta.enterprise.event.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class RemoveFulfillmentUseCaseTest {

  private FulfillmentStore fulfillmentStore;

  @SuppressWarnings("unchecked")
  private final Event<FulfillmentChangedEvent> fulfillmentChanges = mock(Event.class);

  private RemoveFulfillmentUseCase removeFulfillmentUseCase;

  @BeforeEach
  void setUp() {
    fulfillmentStore = mock(FulfillmentStore.class);
    removeFulfillmentUseCase = new RemoveFulfillmentUseCase(fulfillmentStore, fulfillmentChanges);
  }

  @Test
  void shouldRemoveAnExistingAssociationAndPublishIt() {
    var fulfillment =
        new Fulfillment(
            new StoreReference(1L, "TONSTAD"), new ProductReference(7L, "KALLAX"), "MWH.001");
    fulfillment.id = 42L;
    when(fulfillmentStore.findByIdentifier(42L)).thenReturn(fulfillment);

    removeFulfillmentUseCase.remove(42L);

    verify(fulfillmentStore).remove(fulfillment);

    var published = ArgumentCaptor.forClass(FulfillmentChangedEvent.class);
    verify(fulfillmentChanges).fire(published.capture());
    assertEquals(FulfillmentChangedEvent.Operation.REMOVED, published.getValue().operation());
  }

  @Test
  void shouldRejectRemovingAnAssociationThatDoesNotExist() {
    when(fulfillmentStore.findByIdentifier(999L)).thenReturn(null);

    assertEquals(
        "Fulfillment with id of 999 does not exist.",
        assertThrows(
                ResourceNotFoundException.class, () -> removeFulfillmentUseCase.remove(999L))
            .getMessage());

    verify(fulfillmentStore, never()).remove(any());
    verify(fulfillmentChanges, never()).fire(any());
  }
}
