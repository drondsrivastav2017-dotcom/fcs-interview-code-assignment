package com.fulfilment.application.monolith.stores.events;

import com.fulfilment.application.monolith.stores.LegacyStoreManagerGateway;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Propagates store changes to the legacy system. The observer is bound to {@link
 * TransactionPhase#AFTER_SUCCESS}, so the downstream system is only notified with data that is
 * effectively committed to our database: a rolled back transaction never reaches the legacy system.
 */
@ApplicationScoped
public class LegacyStoreSynchronizer {

  private static final Logger LOGGER = Logger.getLogger(LegacyStoreSynchronizer.class);

  @Inject LegacyStoreManagerGateway legacyStoreManagerGateway;

  public void onStoreChanged(
      @Observes(during = TransactionPhase.AFTER_SUCCESS) StoreSyncEvent event) {
    try {
      switch (event.operation()) {
        case CREATED -> legacyStoreManagerGateway.createStoreOnLegacySystem(event.store());
        case UPDATED -> legacyStoreManagerGateway.updateStoreOnLegacySystem(event.store());
      }
    } catch (RuntimeException e) {
      // The transaction is already committed at this point, so the request cannot be failed
      // anymore: the change is logged for a later retry instead of being lost silently.
      LOGGER.errorf(
          e, "Failed to propagate store '%s' to the legacy system.", event.store().name);
    }
  }
}
