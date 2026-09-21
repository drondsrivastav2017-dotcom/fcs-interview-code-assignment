package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.exceptions.ValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;

/** Payload rules shared by the warehouse creation and replacement use cases. */
final class WarehouseValidations {

  private WarehouseValidations() {}

  static void validatePayload(Warehouse warehouse) {
    if (warehouse == null) {
      throw new ValidationException("Warehouse data is required.");
    }

    if (isBlank(warehouse.businessUnitCode)) {
      throw new ValidationException("Business unit code is required.");
    }

    if (isBlank(warehouse.location)) {
      throw new ValidationException("Location is required.");
    }

    if (warehouse.capacity == null || warehouse.capacity <= 0) {
      throw new ValidationException("Capacity must be a positive number.");
    }

    if (warehouse.stock == null || warehouse.stock < 0) {
      throw new ValidationException("Stock must be zero or a positive number.");
    }

    if (warehouse.stock > warehouse.capacity) {
      throw new ValidationException(
          "Stock of "
              + warehouse.stock
              + " does not fit in the informed capacity of "
              + warehouse.capacity
              + ".");
    }
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
