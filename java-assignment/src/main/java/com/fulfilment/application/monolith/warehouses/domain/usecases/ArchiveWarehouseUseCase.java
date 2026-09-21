package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.exceptions.ResourceNotFoundException;
import com.fulfilment.application.monolith.exceptions.ValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;

@ApplicationScoped
public class ArchiveWarehouseUseCase implements ArchiveWarehouseOperation {

  private final WarehouseStore warehouseStore;

  public ArchiveWarehouseUseCase(WarehouseStore warehouseStore) {
    this.warehouseStore = warehouseStore;
  }

  @Override
  @Transactional
  public void archive(Warehouse warehouse) {
    if (warehouse == null
        || warehouse.businessUnitCode == null
        || warehouse.businessUnitCode.isBlank()) {
      throw new ValidationException("Business unit code is required to archive a warehouse.");
    }

    Warehouse existingWarehouse =
        warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode);

    if (existingWarehouse == null) {
      throw new ResourceNotFoundException(
          "No active warehouse found with business unit code " + warehouse.businessUnitCode + ".");
    }

    existingWarehouse.archivedAt = LocalDateTime.now();

    warehouseStore.remove(existingWarehouse);
  }
}
