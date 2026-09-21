package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.exceptions.ResourceNotFoundException;
import com.fulfilment.application.monolith.exceptions.ValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ReplaceWarehouseUseCase implements ReplaceWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final ArchiveWarehouseOperation archiveWarehouseOperation;
  private final CreateWarehouseOperation createWarehouseOperation;

  public ReplaceWarehouseUseCase(
      WarehouseStore warehouseStore,
      ArchiveWarehouseOperation archiveWarehouseOperation,
      CreateWarehouseOperation createWarehouseOperation) {
    this.warehouseStore = warehouseStore;
    this.archiveWarehouseOperation = archiveWarehouseOperation;
    this.createWarehouseOperation = createWarehouseOperation;
  }

  @Override
  @Transactional
  public void replace(Warehouse newWarehouse) {
    WarehouseValidations.validatePayload(newWarehouse);

    Warehouse previousWarehouse =
        warehouseStore.findByBusinessUnitCode(newWarehouse.businessUnitCode);

    if (previousWarehouse == null) {
      throw new ResourceNotFoundException(
          "No active warehouse found with business unit code "
              + newWarehouse.businessUnitCode
              + ".");
    }

    int previousStock = previousWarehouse.stock == null ? 0 : previousWarehouse.stock;

    if (newWarehouse.capacity < previousStock) {
      throw new ValidationException(
          "The new warehouse capacity of "
              + newWarehouse.capacity
              + " cannot accommodate the "
              + previousStock
              + " items of the warehouse being replaced.");
    }

    if (newWarehouse.stock != previousStock) {
      throw new ValidationException(
          "The new warehouse stock of "
              + newWarehouse.stock
              + " does not match the "
              + previousStock
              + " items of the warehouse being replaced.");
    }

    // Archiving first releases the business unit code and the location capacity, so the new unit is
    // validated against the state the company will actually operate in after the replacement.
    archiveWarehouseOperation.archive(previousWarehouse);
    createWarehouseOperation.create(newWarehouse);
  }
}
