package com.fulfilment.application.monolith.fulfillment.domain.validation;

import com.fulfilment.application.monolith.exceptions.ValidationException;
import com.fulfilment.application.monolith.fulfillment.domain.models.ProductReference;
import com.fulfilment.application.monolith.fulfillment.domain.models.StoreReference;
import com.fulfilment.application.monolith.fulfillment.domain.ports.FulfillmentStore;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

/**
 * Holds the colocation constraints of the fulfilment associations:
 *
 * <ol>
 *   <li>a product is fulfilled by at most {@value #MAX_WAREHOUSES_PER_PRODUCT_IN_A_STORE}
 *       warehouses per store;
 *   <li>a store is fulfilled by at most {@value #MAX_WAREHOUSES_PER_STORE} warehouses;
 *   <li>a warehouse stores at most {@value #MAX_PRODUCT_TYPES_PER_WAREHOUSE} types of products.
 * </ol>
 *
 * <p>The limits live here rather than in the use case so that the rules can be read, changed and
 * tested in one place, without going through the persistence or the HTTP layer.
 */
@ApplicationScoped
public class FulfillmentConstraintValidator {

  public static final int MAX_WAREHOUSES_PER_PRODUCT_IN_A_STORE = 2;
  public static final int MAX_WAREHOUSES_PER_STORE = 3;
  public static final int MAX_PRODUCT_TYPES_PER_WAREHOUSE = 5;

  private final FulfillmentStore fulfillmentStore;

  public FulfillmentConstraintValidator(FulfillmentStore fulfillmentStore) {
    this.fulfillmentStore = fulfillmentStore;
  }

  public void validate(StoreReference store, ProductReference product, String businessUnitCode) {
    verifyAssociationIsNew(store, product, businessUnitCode);
    verifyWarehousesPerProductInStore(store, product);
    verifyWarehousesPerStore(store, businessUnitCode);
    verifyProductTypesPerWarehouse(businessUnitCode, product.id());
  }

  private void verifyAssociationIsNew(
      StoreReference store, ProductReference product, String businessUnitCode) {
    if (fulfillmentStore.exists(store.id(), product.id(), businessUnitCode)) {
      throw new ValidationException(
          "Product "
              + product.name()
              + " is already fulfilled by warehouse "
              + businessUnitCode
              + " for store "
              + store.name()
              + ".");
    }
  }

  private void verifyWarehousesPerProductInStore(StoreReference store, ProductReference product) {
    long warehouses =
        fulfillmentStore.countWarehousesFulfillingProductInStore(store.id(), product.id());

    if (warehouses >= MAX_WAREHOUSES_PER_PRODUCT_IN_A_STORE) {
      throw new ValidationException(
          "Product "
              + product.name()
              + " is already fulfilled by the maximum of "
              + MAX_WAREHOUSES_PER_PRODUCT_IN_A_STORE
              + " warehouses in store "
              + store.name()
              + ".");
    }
  }

  private void verifyWarehousesPerStore(StoreReference store, String businessUnitCode) {
    List<String> warehouseCodes = fulfillmentStore.findWarehouseCodesFulfillingStore(store.id());

    // a warehouse that already fulfils the store does not count towards the limit again
    if (!warehouseCodes.contains(businessUnitCode)
        && warehouseCodes.size() >= MAX_WAREHOUSES_PER_STORE) {
      throw new ValidationException(
          "Store "
              + store.name()
              + " is already fulfilled by the maximum of "
              + MAX_WAREHOUSES_PER_STORE
              + " warehouses.");
    }
  }

  private void verifyProductTypesPerWarehouse(String businessUnitCode, Long productId) {
    List<Long> productIds = fulfillmentStore.findProductIdsFulfilledByWarehouse(businessUnitCode);

    // the same product type stored for another store does not count as a new type
    if (!productIds.contains(productId) && productIds.size() >= MAX_PRODUCT_TYPES_PER_WAREHOUSE) {
      throw new ValidationException(
          "Warehouse "
              + businessUnitCode
              + " already stores the maximum of "
              + MAX_PRODUCT_TYPES_PER_WAREHOUSE
              + " product types.");
    }
  }
}
