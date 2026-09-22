package com.fulfilment.application.monolith.fulfillment.domain.usecases;

import com.fulfilment.application.monolith.fulfillment.domain.events.FulfillmentChangedEvent;
import com.fulfilment.application.monolith.fulfillment.domain.models.Fulfillment;
import com.fulfilment.application.monolith.fulfillment.domain.models.ProductReference;
import com.fulfilment.application.monolith.fulfillment.domain.models.StoreReference;
import com.fulfilment.application.monolith.fulfillment.domain.ports.AssociateFulfillmentOperation;
import com.fulfilment.application.monolith.fulfillment.domain.ports.FulfillmentStore;
import com.fulfilment.application.monolith.fulfillment.domain.validation.FulfillmentConstraintValidator;
import com.fulfilment.application.monolith.fulfillment.domain.validation.FulfillmentReferenceValidator;
import com.fulfilment.application.monolith.fulfillment.domain.validation.FulfillmentRequestValidator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class AssociateFulfillmentUseCase implements AssociateFulfillmentOperation {

  private final FulfillmentStore fulfillmentStore;
  private final FulfillmentRequestValidator requestValidator;
  private final FulfillmentReferenceValidator referenceValidator;
  private final FulfillmentConstraintValidator constraintValidator;
  private final Event<FulfillmentChangedEvent> fulfillmentChanges;

  public AssociateFulfillmentUseCase(
      FulfillmentStore fulfillmentStore,
      FulfillmentRequestValidator requestValidator,
      FulfillmentReferenceValidator referenceValidator,
      FulfillmentConstraintValidator constraintValidator,
      Event<FulfillmentChangedEvent> fulfillmentChanges) {
    this.fulfillmentStore = fulfillmentStore;
    this.requestValidator = requestValidator;
    this.referenceValidator = referenceValidator;
    this.constraintValidator = constraintValidator;
    this.fulfillmentChanges = fulfillmentChanges;
  }

  @Override
  @Transactional
  public Fulfillment associate(Long storeId, Long productId, String warehouseBusinessUnitCode) {
    requestValidator.validate(storeId, productId, warehouseBusinessUnitCode);

    StoreReference store = referenceValidator.requireStore(storeId);
    ProductReference product = referenceValidator.requireProduct(productId);
    referenceValidator.requireActiveWarehouse(warehouseBusinessUnitCode);

    constraintValidator.validate(store, product, warehouseBusinessUnitCode);

    var fulfillment = new Fulfillment(store, product, warehouseBusinessUnitCode);
    fulfillmentStore.create(fulfillment);

    fulfillmentChanges.fire(FulfillmentChangedEvent.associated(fulfillment));

    return fulfillment;
  }
}
