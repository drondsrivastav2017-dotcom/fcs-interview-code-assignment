package com.fulfilment.application.monolith.fulfillment.domain.ports;

/**
 * Driven port towards the warehouse module. Fulfilment only needs to know whether a business unit
 * code identifies a warehouse that is currently active, so the port exposes nothing else.
 */
public interface WarehouseResolver {

  boolean isActive(String businessUnitCode);
}
