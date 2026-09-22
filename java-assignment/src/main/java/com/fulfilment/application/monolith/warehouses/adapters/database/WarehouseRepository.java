package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.exceptions.ResourceNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class WarehouseRepository implements WarehouseStore, PanacheRepository<DbWarehouse> {

  private static final String ACTIVE = "archivedAt is null";
  private static final String ACTIVE_BY_BUSINESS_UNIT_CODE =
      "businessUnitCode = ?1 and archivedAt is null";

  /** Only active units are part of the operational view; archived ones are kept as history. */
  @Override
  public List<Warehouse> getAll() {
    return this.list(ACTIVE).stream().map(DbWarehouse::toWarehouse).toList();
  }

  @Override
  @Transactional
  public void create(Warehouse warehouse) {
    var dbWarehouse = new DbWarehouse();
    dbWarehouse.businessUnitCode = warehouse.businessUnitCode;
    dbWarehouse.location = warehouse.location;
    dbWarehouse.capacity = warehouse.capacity;
    dbWarehouse.stock = warehouse.stock;
    dbWarehouse.createdAt = warehouse.createdAt != null ? warehouse.createdAt : LocalDateTime.now();
    dbWarehouse.archivedAt = warehouse.archivedAt;

    this.persist(dbWarehouse);

    warehouse.id = dbWarehouse.id;
    warehouse.createdAt = dbWarehouse.createdAt;
  }

  @Override
  @Transactional
  public void update(Warehouse warehouse) {
    var dbWarehouse = requireActiveByBusinessUnitCode(warehouse.businessUnitCode);
    dbWarehouse.applyMutableFieldsOf(warehouse);

    this.persist(dbWarehouse);

    warehouse.id = dbWarehouse.id;
    warehouse.createdAt = dbWarehouse.createdAt;
  }

  /**
   * Warehouses are never physically deleted: the business unit code has to stay traceable after a
   * replacement, so removing a unit means archiving it.
   */
  @Override
  @Transactional
  public void remove(Warehouse warehouse) {
    var dbWarehouse = requireActiveByBusinessUnitCode(warehouse.businessUnitCode);
    dbWarehouse.archivedAt =
        warehouse.archivedAt != null ? warehouse.archivedAt : LocalDateTime.now();

    this.persist(dbWarehouse);

    warehouse.id = dbWarehouse.id;
    warehouse.archivedAt = dbWarehouse.archivedAt;
  }

  @Override
  public Warehouse findByBusinessUnitCode(String buCode) {
    var dbWarehouse = findActiveByBusinessUnitCode(buCode);
    return dbWarehouse == null ? null : dbWarehouse.toWarehouse();
  }

  @Override
  public Warehouse findActiveById(Long id) {
    var dbWarehouse = this.findById(id);
    return dbWarehouse == null || dbWarehouse.archivedAt != null ? null : dbWarehouse.toWarehouse();
  }

  private DbWarehouse findActiveByBusinessUnitCode(String buCode) {
    if (buCode == null || buCode.isBlank()) {
      return null;
    }
    return this.find(ACTIVE_BY_BUSINESS_UNIT_CODE, buCode).firstResult();
  }

  private DbWarehouse requireActiveByBusinessUnitCode(String buCode) {
    var dbWarehouse = findActiveByBusinessUnitCode(buCode);
    if (dbWarehouse == null) {
      throw new ResourceNotFoundException(
          "Warehouse with business unit code " + buCode + " does not exist.");
    }
    return dbWarehouse;
  }
}
