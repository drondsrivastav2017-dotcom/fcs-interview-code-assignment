package com.fulfilment.application.monolith.fulfillment.adapters.restapi;

import com.fulfilment.application.monolith.fulfillment.domain.models.Fulfillment;

/**
 * Response body of the fulfilment endpoints. It is a dedicated bean rather than the domain model, so
 * the API contract does not change every time the model does.
 */
public record FulfillmentResponse(
    Long id,
    Long storeId,
    String storeName,
    Long productId,
    String productName,
    String warehouseBusinessUnitCode) {

  public static FulfillmentResponse from(Fulfillment fulfillment) {
    return new FulfillmentResponse(
        fulfillment.id,
        fulfillment.store.id(),
        fulfillment.store.name(),
        fulfillment.product.id(),
        fulfillment.product.name(),
        fulfillment.warehouseBusinessUnitCode);
  }
}
