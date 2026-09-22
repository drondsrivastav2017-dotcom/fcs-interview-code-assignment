package com.fulfilment.application.monolith.fulfillment.domain.ports;

import com.fulfilment.application.monolith.fulfillment.domain.models.StoreReference;

/** Driven port resolving the store an association refers to. */
public interface StoreResolver {

  /** Returns the store with the given id, or {@code null} when it does not exist. */
  StoreReference resolveById(Long storeId);
}
