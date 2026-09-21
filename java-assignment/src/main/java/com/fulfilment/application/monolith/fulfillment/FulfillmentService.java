package com.fulfilment.application.monolith.fulfillment;

import com.fulfilment.application.monolith.exceptions.ResourceNotFoundException;
import com.fulfilment.application.monolith.exceptions.ValidationException;
import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.products.ProductRepository;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class FulfillmentService {

  static final int MAX_WAREHOUSES_PER_PRODUCT_IN_A_STORE = 2;
  static final int MAX_WAREHOUSES_PER_STORE = 3;
  static final int MAX_PRODUCT_TYPES_PER_WAREHOUSE = 5;

  @Inject FulfillmentRepository fulfillmentRepository;

  @Inject ProductRepository productRepository;

  @Inject WarehouseStore warehouseStore;

  public List<Fulfillment> listAll() {
    return fulfillmentRepository.listAllSorted();
  }

  public List<Fulfillment> listByStore(Long storeId) {
    requireStore(storeId);
    return fulfillmentRepository.listByStore(storeId);
  }

  public List<Fulfillment> listByWarehouse(String businessUnitCode) {
    requireActiveWarehouse(businessUnitCode);
    return fulfillmentRepository.listByWarehouse(businessUnitCode);
  }

  @Transactional
  public Fulfillment associate(Long storeId, Long productId, String businessUnitCode) {
    if (storeId == null || productId == null || businessUnitCode == null
        || businessUnitCode.isBlank()) {
      throw new ValidationException(
          "storeId, productId and warehouseBusinessUnitCode are required.");
    }

    Store store = requireStore(storeId);
    Product product = requireProduct(productId);
    requireActiveWarehouse(businessUnitCode);

    if (fulfillmentRepository.exists(storeId, productId, businessUnitCode)) {
      throw new ValidationException(
          "Product "
              + product.name
              + " is already fulfilled by warehouse "
              + businessUnitCode
              + " for store "
              + store.name
              + ".");
    }

    verifyWarehousesPerProductInStore(store, product, storeId, productId);
    verifyWarehousesPerStore(store, storeId, businessUnitCode);
    verifyProductTypesPerWarehouse(businessUnitCode, productId);

    var fulfillment = new Fulfillment(store, product, businessUnitCode);
    fulfillmentRepository.persist(fulfillment);

    return fulfillment;
  }

  @Transactional
  public void remove(Long id) {
    Fulfillment fulfillment = fulfillmentRepository.findById(id);
    if (fulfillment == null) {
      throw new ResourceNotFoundException("Fulfillment with id of " + id + " does not exist.");
    }
    fulfillmentRepository.delete(fulfillment);
  }

  private void verifyWarehousesPerProductInStore(
      Store store, Product product, Long storeId, Long productId) {
    long warehouses =
        fulfillmentRepository.countWarehousesFulfillingProductInStore(storeId, productId);

    if (warehouses >= MAX_WAREHOUSES_PER_PRODUCT_IN_A_STORE) {
      throw new ValidationException(
          "Product "
              + product.name
              + " is already fulfilled by the maximum of "
              + MAX_WAREHOUSES_PER_PRODUCT_IN_A_STORE
              + " warehouses in store "
              + store.name
              + ".");
    }
  }

  private void verifyWarehousesPerStore(Store store, Long storeId, String businessUnitCode) {
    List<String> warehouseCodes = fulfillmentRepository.findWarehouseCodesFulfillingStore(storeId);

    if (!warehouseCodes.contains(businessUnitCode)
        && warehouseCodes.size() >= MAX_WAREHOUSES_PER_STORE) {
      throw new ValidationException(
          "Store "
              + store.name
              + " is already fulfilled by the maximum of "
              + MAX_WAREHOUSES_PER_STORE
              + " warehouses.");
    }
  }

  private void verifyProductTypesPerWarehouse(String businessUnitCode, Long productId) {
    List<Long> productIds =
        fulfillmentRepository.findProductIdsFulfilledByWarehouse(businessUnitCode);

    if (!productIds.contains(productId) && productIds.size() >= MAX_PRODUCT_TYPES_PER_WAREHOUSE) {
      throw new ValidationException(
          "Warehouse "
              + businessUnitCode
              + " already stores the maximum of "
              + MAX_PRODUCT_TYPES_PER_WAREHOUSE
              + " product types.");
    }
  }

  private Store requireStore(Long storeId) {
    Store store = Store.findById(storeId);
    if (store == null) {
      throw new ResourceNotFoundException("Store with id of " + storeId + " does not exist.");
    }
    return store;
  }

  private Product requireProduct(Long productId) {
    Product product = productRepository.findById(productId);
    if (product == null) {
      throw new ResourceNotFoundException("Product with id of " + productId + " does not exist.");
    }
    return product;
  }

  private void requireActiveWarehouse(String businessUnitCode) {
    if (warehouseStore.findByBusinessUnitCode(businessUnitCode) == null) {
      throw new ResourceNotFoundException(
          "No active warehouse found with business unit code " + businessUnitCode + ".");
    }
  }
}
