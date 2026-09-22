package com.fulfilment.application.monolith.fulfillment.domain.ports;

import com.fulfilment.application.monolith.fulfillment.domain.models.Fulfillment;

/** Driving port: associate a warehouse as fulfilment unit of a product for a store. */
public interface AssociateFulfillmentOperation {

  Fulfillment associate(Long storeId, Long productId, String warehouseBusinessUnitCode);
}
