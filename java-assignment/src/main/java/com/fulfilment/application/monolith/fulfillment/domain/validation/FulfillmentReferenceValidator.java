package com.fulfilment.application.monolith.fulfillment.domain.validation;

import com.fulfilment.application.monolith.exceptions.ResourceNotFoundException;
import com.fulfilment.application.monolith.fulfillment.domain.models.ProductReference;
import com.fulfilment.application.monolith.fulfillment.domain.models.StoreReference;
import com.fulfilment.application.monolith.fulfillment.domain.ports.ProductResolver;
import com.fulfilment.application.monolith.fulfillment.domain.ports.StoreResolver;
import com.fulfilment.application.monolith.fulfillment.domain.ports.WarehouseResolver;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Validates that the entities an association refers to actually exist, and resolves them on the
 * way. Gathering the three lookups here keeps the messages consistent between the operations that
 * need them: an unknown store returns the same error whether it is being associated or listed.
 */
@ApplicationScoped
public class FulfillmentReferenceValidator {

  private final StoreResolver storeResolver;
  private final ProductResolver productResolver;
  private final WarehouseResolver warehouseResolver;

  public FulfillmentReferenceValidator(
      StoreResolver storeResolver,
      ProductResolver productResolver,
      WarehouseResolver warehouseResolver) {
    this.storeResolver = storeResolver;
    this.productResolver = productResolver;
    this.warehouseResolver = warehouseResolver;
  }

  public StoreReference requireStore(Long storeId) {
    StoreReference store = storeResolver.resolveById(storeId);
    if (store == null) {
      throw new ResourceNotFoundException("Store with id of " + storeId + " does not exist.");
    }
    return store;
  }

  public ProductReference requireProduct(Long productId) {
    ProductReference product = productResolver.resolveById(productId);
    if (product == null) {
      throw new ResourceNotFoundException("Product with id of " + productId + " does not exist.");
    }
    return product;
  }

  public void requireActiveWarehouse(String businessUnitCode) {
    if (!warehouseResolver.isActive(businessUnitCode)) {
      throw new ResourceNotFoundException(
          "No active warehouse found with business unit code " + businessUnitCode + ".");
    }
  }
}
