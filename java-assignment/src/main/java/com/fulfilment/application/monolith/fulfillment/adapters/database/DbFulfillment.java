package com.fulfilment.application.monolith.fulfillment.adapters.database;

import com.fulfilment.application.monolith.fulfillment.domain.models.Fulfillment;
import com.fulfilment.application.monolith.fulfillment.domain.models.ProductReference;
import com.fulfilment.application.monolith.fulfillment.domain.models.StoreReference;
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
 * Persistence view of a fulfilment association. The store and the product are mapped as real
 * relations so the database keeps the referential integrity, while the warehouse is kept as its
 * business unit code: that code survives a replacement, the warehouse row does not.
 */
@Entity
@Table(
    name = "fulfillment",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_fulfillment_store_product_warehouse",
            columnNames = {"store_id", "product_id", "warehouseBusinessUnitCode"}))
public class DbFulfillment extends PanacheEntity {

  @ManyToOne(optional = false)
  @JoinColumn(name = "store_id", nullable = false)
  public Store store;

  @ManyToOne(optional = false)
  @JoinColumn(name = "product_id", nullable = false)
  public Product product;

  @Column(nullable = false, length = 40)
  public String warehouseBusinessUnitCode;

  public DbFulfillment() {}

  public DbFulfillment(Store store, Product product, String warehouseBusinessUnitCode) {
    this.store = store;
    this.product = product;
    this.warehouseBusinessUnitCode = warehouseBusinessUnitCode;
  }

  public Fulfillment toFulfillment() {
    var fulfillment =
        new Fulfillment(
            new StoreReference(store.id, store.name),
            new ProductReference(product.id, product.name),
            warehouseBusinessUnitCode);
    fulfillment.id = this.id;
    return fulfillment;
  }
}
