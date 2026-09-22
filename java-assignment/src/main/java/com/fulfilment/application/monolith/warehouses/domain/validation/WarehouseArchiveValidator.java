package com.fulfilment.application.monolith.warehouses.domain.validation;

import com.fulfilment.application.monolith.exceptions.ResourceNotFoundException;
import com.fulfilment.application.monolith.exceptions.ValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;

/** Validates that a warehouse may be archived: it has to be identified and still be active. */
@ApplicationScoped
public class WarehouseArchiveValidator {

  private final WarehouseStore warehouseStore;

  public WarehouseArchiveValidator(WarehouseStore warehouseStore) {
    this.warehouseStore = warehouseStore;
  }

  /** Returns the active warehouse matching the business unit code of the given unit. */
  public Warehouse validate(Warehouse warehouse) {
    if (warehouse == null
        || warehouse.businessUnitCode == null
        || warehouse.businessUnitCode.isBlank()) {
      throw new ValidationException("Business unit code is required to archive a warehouse.");
    }

    Warehouse existingWarehouse = warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode);

    if (existingWarehouse == null) {
      throw new ResourceNotFoundException(
          "No active warehouse found with business unit code " + warehouse.businessUnitCode + ".");
    }

    return existingWarehouse;
  }
}
