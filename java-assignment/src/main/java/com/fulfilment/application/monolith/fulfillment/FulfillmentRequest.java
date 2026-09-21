package com.fulfilment.application.monolith.fulfillment;

public record FulfillmentRequest(Long storeId, Long productId, String warehouseBusinessUnitCode) {}
