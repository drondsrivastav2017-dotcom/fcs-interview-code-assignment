package com.fulfilment.application.monolith.fulfillment.domain.ports;

/** Driving port: remove an existing fulfilment association. */
public interface RemoveFulfillmentOperation {

  void remove(Long id);
}
