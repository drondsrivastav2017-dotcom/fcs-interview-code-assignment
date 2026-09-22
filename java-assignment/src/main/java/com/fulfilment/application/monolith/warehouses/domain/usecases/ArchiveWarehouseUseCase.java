package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import com.fulfilment.application.monolith.warehouses.domain.validation.WarehouseArchiveValidator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;

@ApplicationScoped
public class ArchiveWarehouseUseCase implements ArchiveWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final WarehouseArchiveValidator archiveValidator;

  public ArchiveWarehouseUseCase(
      WarehouseStore warehouseStore, WarehouseArchiveValidator archiveValidator) {
    this.warehouseStore = warehouseStore;
    this.archiveValidator = archiveValidator;
  }

  @Override
  @Transactional
  public void archive(Warehouse warehouse) {
    Warehouse existingWarehouse = archiveValidator.validate(warehouse);

    existingWarehouse.archivedAt = LocalDateTime.now();

    warehouseStore.remove(existingWarehouse);
  }
}
