package com.fulfilment.application.monolith.warehouses.domain.validation;

import com.fulfilment.application.monolith.exceptions.ResourceNotFoundException;
import com.fulfilment.application.monolith.exceptions.ValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Validates the additional rules of a replacement: the unit being replaced has to be active, and
 * the new unit has to be able to take over its stock without changing it.
 *
 * <p>The rules that also apply to a plain creation are not repeated here: they are validated by
 * {@link WarehouseCreationValidator} when the replacement creates the new unit.
 */
@ApplicationScoped
public class WarehouseReplacementValidator {

  private final WarehousePayloadValidator payloadValidator;
  private final WarehouseStore warehouseStore;

  public WarehouseReplacementValidator(
      WarehousePayloadValidator payloadValidator, WarehouseStore warehouseStore) {
    this.payloadValidator = payloadValidator;
    this.warehouseStore = warehouseStore;
  }

  /** Returns the active warehouse that the given unit replaces, and which has to be archived. */
  public Warehouse validate(Warehouse newWarehouse) {
    payloadValidator.validate(newWarehouse);

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

    return previousWarehouse;
  }
}
