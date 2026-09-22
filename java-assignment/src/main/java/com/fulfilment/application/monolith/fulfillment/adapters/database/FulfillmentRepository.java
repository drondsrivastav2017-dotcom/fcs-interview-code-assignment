package com.fulfilment.application.monolith.fulfillment.adapters.database;

import com.fulfilment.application.monolith.fulfillment.domain.models.Fulfillment;
import com.fulfilment.application.monolith.fulfillment.domain.ports.FulfillmentStore;
import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.stores.Store;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;

/** Persistence adapter of {@link FulfillmentStore}, backed by Hibernate ORM with Panache. */
@ApplicationScoped
public class FulfillmentRepository implements FulfillmentStore, PanacheRepository<DbFulfillment> {

  @Override
  public List<Fulfillment> getAll() {
    return toFulfillments(
        listAll(Sort.by("store.name").and("product.name").and("warehouseBusinessUnitCode")));
  }

  @Override
  public List<Fulfillment> listByStore(Long storeId) {
    return toFulfillments(list("store.id = ?1", storeId));
  }

  @Override
  public List<Fulfillment> listByWarehouse(String businessUnitCode) {
    return toFulfillments(list("warehouseBusinessUnitCode = ?1", businessUnitCode));
  }

  /**
   * The port deliberately does not call this method {@code findById}: that name is already taken by
   * {@link PanacheRepository}, which returns the database entity rather than the domain model.
   */
  @Override
  public Fulfillment findByIdentifier(Long id) {
    DbFulfillment dbFulfillment = id == null ? null : findById(id);
    return dbFulfillment == null ? null : dbFulfillment.toFulfillment();
  }

  @Override
  @Transactional
  public void create(Fulfillment fulfillment) {
    // references instead of loaded entities: the ids were already validated by the domain
    var dbFulfillment =
        new DbFulfillment(
            getEntityManager().getReference(Store.class, fulfillment.store.id()),
            getEntityManager().getReference(Product.class, fulfillment.product.id()),
            fulfillment.warehouseBusinessUnitCode);

    persist(dbFulfillment);

    fulfillment.id = dbFulfillment.id;
  }

  @Override
  @Transactional
  public void remove(Fulfillment fulfillment) {
    deleteById(fulfillment.id);
  }

  @Override
  public boolean exists(Long storeId, Long productId, String businessUnitCode) {
    return count(
            "store.id = ?1 and product.id = ?2 and warehouseBusinessUnitCode = ?3",
            storeId,
            productId,
            businessUnitCode)
        > 0;
  }

  @Override
  public long countWarehousesFulfillingProductInStore(Long storeId, Long productId) {
    return count("store.id = ?1 and product.id = ?2", storeId, productId);
  }

  @Override
  public List<String> findWarehouseCodesFulfillingStore(Long storeId) {
    return getEntityManager()
        .createQuery(
            "select distinct f.warehouseBusinessUnitCode from DbFulfillment f"
                + " where f.store.id = :storeId",
            String.class)
        .setParameter("storeId", storeId)
        .getResultList();
  }

  @Override
  public List<Long> findProductIdsFulfilledByWarehouse(String businessUnitCode) {
    return getEntityManager()
        .createQuery(
            "select distinct f.product.id from DbFulfillment f"
                + " where f.warehouseBusinessUnitCode = :businessUnitCode",
            Long.class)
        .setParameter("businessUnitCode", businessUnitCode)
        .getResultList();
  }

  private static List<Fulfillment> toFulfillments(List<DbFulfillment> dbFulfillments) {
    return dbFulfillments.stream().map(DbFulfillment::toFulfillment).toList();
  }
}
