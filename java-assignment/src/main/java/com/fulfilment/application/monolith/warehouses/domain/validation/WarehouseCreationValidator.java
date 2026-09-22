package com.fulfilment.application.monolith.warehouses.domain.validation;

import com.fulfilment.application.monolith.exceptions.ValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

/**
 * Validates that a new warehouse may be created: the business unit code has to be free and the
 * location has to exist, still accept another unit and still have room for the informed capacity.
 */
@ApplicationScoped
public class WarehouseCreationValidator {

  private final WarehousePayloadValidator payloadValidator;
  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public WarehouseCreationValidator(
      WarehousePayloadValidator payloadValidator,
      WarehouseStore warehouseStore,
      LocationResolver locationResolver) {
    this.payloadValidator = payloadValidator;
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  /** Returns the resolved location, so that the caller does not have to look it up again. */
  public Location validate(Warehouse warehouse) {
    payloadValidator.validate(warehouse);

    verifyBusinessUnitCodeIsAvailable(warehouse.businessUnitCode);

    Location location = resolveLocation(warehouse.location);
    List<Warehouse> warehousesAtLocation = activeWarehousesAt(location);

    verifyLocationCanHostAnotherWarehouse(location, warehousesAtLocation);
    verifyLocationHasRoomForCapacity(location, warehousesAtLocation, warehouse.capacity);

    return location;
  }

  private void verifyBusinessUnitCodeIsAvailable(String businessUnitCode) {
    if (warehouseStore.findByBusinessUnitCode(businessUnitCode) != null) {
      throw new ValidationException(
          "A warehouse with business unit code " + businessUnitCode + " already exists.");
    }
  }

  private Location resolveLocation(String identifier) {
    Location location = locationResolver.resolveByIdentifier(identifier);
    if (location == null) {
      throw new ValidationException("Location " + identifier + " does not exist.");
    }
    return location;
  }

  private List<Warehouse> activeWarehousesAt(Location location) {
    return warehouseStore.getAll().stream()
        .filter(warehouse -> location.identification.equalsIgnoreCase(warehouse.location))
        .toList();
  }

  private void verifyLocationCanHostAnotherWarehouse(
      Location location, List<Warehouse> warehousesAtLocation) {
    if (warehousesAtLocation.size() >= location.maxNumberOfWarehouses) {
      throw new ValidationException(
          "Location "
              + location.identification
              + " already hosts the maximum of "
              + location.maxNumberOfWarehouses
              + " warehouses.");
    }
  }

  private void verifyLocationHasRoomForCapacity(
      Location location, List<Warehouse> warehousesAtLocation, int capacity) {
    int usedCapacity =
        warehousesAtLocation.stream().mapToInt(warehouse -> warehouse.capacity).sum();
    int availableCapacity = location.maxCapacity - usedCapacity;

    if (capacity > availableCapacity) {
      throw new ValidationException(
          "Capacity of "
              + capacity
              + " exceeds the "
              + availableCapacity
              + " still available at location "
              + location.identification
              + ".");
    }
  }
}
