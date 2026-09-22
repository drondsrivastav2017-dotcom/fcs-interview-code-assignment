package com.fulfilment.application.monolith.fulfillment.adapters.events;

import com.fulfilment.application.monolith.fulfillment.domain.events.FulfillmentChangedEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import org.jboss.logging.Logger;

/**
 * Records the fulfilment associations that were effectively committed. Which warehouse fulfils which
 * product for which store is what the cost allocation of a store is later built on, so the changes
 * are worth a trace that survives the request.
 *
 * <p>The observer runs during {@link TransactionPhase#AFTER_SUCCESS}: a rolled back association is
 * never logged as if it had happened. It is also the seam where a downstream system - a reporting
 * pipeline, or the legacy store manager - would be notified, with the same guarantee.
 */
@ApplicationScoped
public class FulfillmentAuditLogger {

  private static final Logger LOGGER = Logger.getLogger(FulfillmentAuditLogger.class);

  public void onFulfillmentChanged(
      @Observes(during = TransactionPhase.AFTER_SUCCESS) FulfillmentChangedEvent event) {
    var fulfillment = event.fulfillment();

    LOGGER.infof(
        "Fulfillment %s: store '%s' (%d), product '%s' (%d), warehouse %s.",
        event.operation(),
        fulfillment.store.name(),
        fulfillment.store.id(),
        fulfillment.product.name(),
        fulfillment.product.id(),
        fulfillment.warehouseBusinessUnitCode);
  }
}
