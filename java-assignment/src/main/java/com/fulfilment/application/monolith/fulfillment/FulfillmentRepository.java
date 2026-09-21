package com.fulfilment.application.monolith.fulfillment;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class FulfillmentRepository implements PanacheRepository<Fulfillment> {

  public List<Fulfillment> listAllSorted() {
    return listAll(Sort.by("store.name").and("product.name").and("warehouseBusinessUnitCode"));
  }

  public List<Fulfillment> listByStore(Long storeId) {
    return list("store.id = ?1", storeId);
  }

  public List<Fulfillment> listByWarehouse(String businessUnitCode) {
    return list("warehouseBusinessUnitCode = ?1", businessUnitCode);
  }

  public boolean exists(Long storeId, Long productId, String businessUnitCode) {
    return count(
            "store.id = ?1 and product.id = ?2 and warehouseBusinessUnitCode = ?3",
            storeId,
            productId,
            businessUnitCode)
        > 0;
  }

  public long countWarehousesFulfillingProductInStore(Long storeId, Long productId) {
    return count("store.id = ?1 and product.id = ?2", storeId, productId);
  }

  public List<String> findWarehouseCodesFulfillingStore(Long storeId) {
    return getEntityManager()
        .createQuery(
            "select distinct f.warehouseBusinessUnitCode from Fulfillment f where f.store.id = :storeId",
            String.class)
        .setParameter("storeId", storeId)
        .getResultList();
  }

  public List<Long> findProductIdsFulfilledByWarehouse(String businessUnitCode) {
    return getEntityManager()
        .createQuery(
            "select distinct f.product.id from Fulfillment f where f.warehouseBusinessUnitCode = :businessUnitCode",
            Long.class)
        .setParameter("businessUnitCode", businessUnitCode)
        .getResultList();
  }
}
