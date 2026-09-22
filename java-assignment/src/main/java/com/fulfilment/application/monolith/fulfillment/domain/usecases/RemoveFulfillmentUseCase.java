package com.fulfilment.application.monolith.fulfillment.domain.usecases;

import com.fulfilment.application.monolith.exceptions.ResourceNotFoundException;
import com.fulfilment.application.monolith.fulfillment.domain.events.FulfillmentChangedEvent;
import com.fulfilment.application.monolith.fulfillment.domain.models.Fulfillment;
import com.fulfilment.application.monolith.fulfillment.domain.ports.FulfillmentStore;
import com.fulfilment.application.monolith.fulfillment.domain.ports.RemoveFulfillmentOperation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class RemoveFulfillmentUseCase implements RemoveFulfillmentOperation {

  private final FulfillmentStore fulfillmentStore;
  private final Event<FulfillmentChangedEvent> fulfillmentChanges;

  public RemoveFulfillmentUseCase(
      FulfillmentStore fulfillmentStore, Event<FulfillmentChangedEvent> fulfillmentChanges) {
    this.fulfillmentStore = fulfillmentStore;
    this.fulfillmentChanges = fulfillmentChanges;
  }

  @Override
  @Transactional
  public void remove(Long id) {
    Fulfillment fulfillment = fulfillmentStore.findByIdentifier(id);

    if (fulfillment == null) {
      throw new ResourceNotFoundException("Fulfillment with id of " + id + " does not exist.");
    }

    fulfillmentStore.remove(fulfillment);

    fulfillmentChanges.fire(FulfillmentChangedEvent.removed(fulfillment));
  }
}
