package com.fulfilment.application.monolith.fulfillment.adapters.restapi;

/** Request body of the association endpoint. */
public record FulfillmentRequest(Long storeId, Long productId, String warehouseBusinessUnitCode) {}
