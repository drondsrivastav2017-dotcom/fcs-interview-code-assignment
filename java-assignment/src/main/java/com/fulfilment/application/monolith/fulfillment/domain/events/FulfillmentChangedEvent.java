package com.fulfilment.application.monolith.fulfillment.domain.events;

import com.fulfilment.application.monolith.fulfillment.domain.models.Fulfillment;

/**
 * Published when a fulfilment association changes. Observers are bound to {@code AFTER_SUCCESS}, so
 * they only see associations that are effectively committed - the same guarantee the store
 * synchronisation relies on.
 *
 * <p>The event carries the domain model, which is already detached from the persistence context,
 * and is therefore safe to read after the transaction is over.
 */
public record FulfillmentChangedEvent(Operation operation, Fulfillment fulfillment) {

  public enum Operation {
    ASSOCIATED,
    REMOVED
  }

  public static FulfillmentChangedEvent associated(Fulfillment fulfillment) {
    return new FulfillmentChangedEvent(Operation.ASSOCIATED, fulfillment);
  }

  public static FulfillmentChangedEvent removed(Fulfillment fulfillment) {
    return new FulfillmentChangedEvent(Operation.REMOVED, fulfillment);
  }
}
