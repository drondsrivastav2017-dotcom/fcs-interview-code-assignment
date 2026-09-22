package com.fulfilment.application.monolith.fulfillment.domain.usecases;

import com.fulfilment.application.monolith.fulfillment.domain.models.Fulfillment;
import com.fulfilment.application.monolith.fulfillment.domain.ports.FulfillmentStore;
import com.fulfilment.application.monolith.fulfillment.domain.ports.RetrieveFulfillmentOperation;
import com.fulfilment.application.monolith.fulfillment.domain.validation.FulfillmentReferenceValidator;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class RetrieveFulfillmentUseCase implements RetrieveFulfillmentOperation {

  private final FulfillmentStore fulfillmentStore;
  private final FulfillmentReferenceValidator referenceValidator;

  public RetrieveFulfillmentUseCase(
      FulfillmentStore fulfillmentStore, FulfillmentReferenceValidator referenceValidator) {
    this.fulfillmentStore = fulfillmentStore;
    this.referenceValidator = referenceValidator;
  }

  @Override
  public List<Fulfillment> listAll() {
    return fulfillmentStore.getAll();
  }

  /** An unknown store is reported as such instead of being answered with an empty list. */
  @Override
  public List<Fulfillment> listByStore(Long storeId) {
    referenceValidator.requireStore(storeId);
    return fulfillmentStore.listByStore(storeId);
  }

  /** Likewise, listing the associations of a warehouse that is not active is an error. */
  @Override
  public List<Fulfillment> listByWarehouse(String businessUnitCode) {
    referenceValidator.requireActiveWarehouse(businessUnitCode);
    return fulfillmentStore.listByWarehouse(businessUnitCode);
  }
}
