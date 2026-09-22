package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.validation.WarehouseReplacementValidator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ReplaceWarehouseUseCase implements ReplaceWarehouseOperation {

  private final WarehouseReplacementValidator replacementValidator;
  private final ArchiveWarehouseOperation archiveWarehouseOperation;
  private final CreateWarehouseOperation createWarehouseOperation;

  public ReplaceWarehouseUseCase(
      WarehouseReplacementValidator replacementValidator,
      ArchiveWarehouseOperation archiveWarehouseOperation,
      CreateWarehouseOperation createWarehouseOperation) {
    this.replacementValidator = replacementValidator;
    this.archiveWarehouseOperation = archiveWarehouseOperation;
    this.createWarehouseOperation = createWarehouseOperation;
  }

  @Override
  @Transactional
  public void replace(Warehouse newWarehouse) {
    Warehouse previousWarehouse = replacementValidator.validate(newWarehouse);

    // Archiving first releases the business unit code and the location capacity, so the new unit is
    // validated against the state the company will actually operate in after the replacement.
    archiveWarehouseOperation.archive(previousWarehouse);
    createWarehouseOperation.create(newWarehouse);
  }
}
