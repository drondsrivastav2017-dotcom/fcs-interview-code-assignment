package com.fulfilment.application.monolith.fulfillment;

import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.stores.Store;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Association stating that a warehouse acts as a fulfilment unit of a product for a store.
 *
 * <p>The warehouse is referenced by its business unit code rather than by row id: a replacement
 * archives the row but keeps the business unit code, and the new unit is expected to take over the
 * fulfilment duties of the one it replaces.
 */
@Entity
@Table(
    name = "fulfillment",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_fulfillment_store_product_warehouse",
            columnNames = {"store_id", "product_id", "warehouseBusinessUnitCode"}))
public class Fulfillment extends PanacheEntity {

  @ManyToOne(optional = false)
  @JoinColumn(name = "store_id", nullable = false)
  public Store store;

  @ManyToOne(optional = false)
  @JoinColumn(name = "product_id", nullable = false)
  public Product product;

  @Column(nullable = false, length = 40)
  public String warehouseBusinessUnitCode;

  public Fulfillment() {}

  public Fulfillment(Store store, Product product, String warehouseBusinessUnitCode) {
    this.store = store;
    this.product = product;
    this.warehouseBusinessUnitCode = warehouseBusinessUnitCode;
  }
}
