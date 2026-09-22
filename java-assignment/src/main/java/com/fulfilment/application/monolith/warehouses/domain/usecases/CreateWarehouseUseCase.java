package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import com.fulfilment.application.monolith.warehouses.domain.validation.WarehouseCreationValidator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;

@ApplicationScoped
public class CreateWarehouseUseCase implements CreateWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final WarehouseCreationValidator creationValidator;

  public CreateWarehouseUseCase(
      WarehouseStore warehouseStore, WarehouseCreationValidator creationValidator) {
    this.warehouseStore = warehouseStore;
    this.creationValidator = creationValidator;
  }

  @Override
  @Transactional
  public void create(Warehouse warehouse) {
    creationValidator.validate(warehouse);

    warehouse.createdAt = LocalDateTime.now();
    warehouse.archivedAt = null;

    // if all went well, create the warehouse
    warehouseStore.create(warehouse);
  }
}
