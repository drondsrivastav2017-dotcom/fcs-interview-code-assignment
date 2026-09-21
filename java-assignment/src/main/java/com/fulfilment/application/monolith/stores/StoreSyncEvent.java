package com.fulfilment.application.monolith.stores;

/**
 * Carries a store change towards the legacy system. The store is a detached copy: the event is
 * consumed after the transaction is committed, when the persistence context that loaded the managed
 * entity is already closed.
 */
public record StoreSyncEvent(Operation operation, Store store) {

  public enum Operation {
    CREATED,
    UPDATED
  }

  public static StoreSyncEvent created(Store store) {
    return new StoreSyncEvent(Operation.CREATED, detachedCopyOf(store));
  }

  public static StoreSyncEvent updated(Store store) {
    return new StoreSyncEvent(Operation.UPDATED, detachedCopyOf(store));
  }

  private static Store detachedCopyOf(Store store) {
    var copy = new Store(store.name);
    copy.id = store.id;
    copy.quantityProductsInStock = store.quantityProductsInStock;
    return copy;
  }
}
