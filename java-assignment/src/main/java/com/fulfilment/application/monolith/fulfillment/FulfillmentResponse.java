package com.fulfilment.application.monolith.fulfillment;

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
        fulfillment.store.id,
        fulfillment.store.name,
        fulfillment.product.id,
        fulfillment.product.name,
        fulfillment.warehouseBusinessUnitCode);
  }
}
