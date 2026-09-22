package com.fulfilment.application.monolith.fulfillment.domain.validation;

import com.fulfilment.application.monolith.exceptions.ValidationException;
import jakarta.enterprise.context.ApplicationScoped;

/** Validates the submitted values of an association on their own, before anything is looked up. */
@ApplicationScoped
public class FulfillmentRequestValidator {

  public void validate(Long storeId, Long productId, String warehouseBusinessUnitCode) {
    if (storeId == null
        || productId == null
        || warehouseBusinessUnitCode == null
        || warehouseBusinessUnitCode.isBlank()) {
      throw new ValidationException(
          "storeId, productId and warehouseBusinessUnitCode are required.");
    }
  }
}
