package com.fulfilment.application.monolith.fulfillment.domain.models;

/**
 * Association stating that a warehouse acts as a fulfilment unit of a product for a store.
 *
 * <p>The warehouse is referenced by its business unit code rather than by row id: a replacement
 * archives the row but keeps the business unit code, and the new unit is expected to take over the
 * fulfilment duties of the one it replaces.
 */
public class Fulfillment {

  // technical identifier, assigned by the store when the association is persisted
  public Long id;

  public StoreReference store;

  public ProductReference product;

  public String warehouseBusinessUnitCode;

  public Fulfillment() {}

  public Fulfillment(
      StoreReference store, ProductReference product, String warehouseBusinessUnitCode) {
    this.store = store;
    this.product = product;
    this.warehouseBusinessUnitCode = warehouseBusinessUnitCode;
  }
}
