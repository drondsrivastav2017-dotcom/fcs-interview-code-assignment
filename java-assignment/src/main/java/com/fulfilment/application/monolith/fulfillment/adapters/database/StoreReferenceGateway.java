package com.fulfilment.application.monolith.fulfillment.adapters.database;

import com.fulfilment.application.monolith.fulfillment.domain.models.StoreReference;
import com.fulfilment.application.monolith.fulfillment.domain.ports.StoreResolver;
import com.fulfilment.application.monolith.stores.Store;
import jakarta.enterprise.context.ApplicationScoped;

/** Resolves stores for the fulfilment domain, which never sees the {@link Store} entity itself. */
@ApplicationScoped
public class StoreReferenceGateway implements StoreResolver {

  @Override
  public StoreReference resolveById(Long storeId) {
    if (storeId == null) {
      return null;
    }

    Store store = Store.findById(storeId);

    return store == null ? null : new StoreReference(store.id, store.name);
  }
}
