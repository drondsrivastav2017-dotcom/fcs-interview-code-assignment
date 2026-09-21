package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.exceptions.ValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class CreateWarehouseUseCase implements CreateWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public CreateWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  @Transactional
  public void create(Warehouse warehouse) {
    WarehouseValidations.validatePayload(warehouse);

    verifyBusinessUnitCodeIsAvailable(warehouse.businessUnitCode);

    Location location = resolveLocation(warehouse.location);
    List<Warehouse> warehousesAtLocation = activeWarehousesAt(location);

    verifyLocationCanHostAnotherWarehouse(location, warehousesAtLocation);
    verifyLocationHasRoomForCapacity(location, warehousesAtLocation, warehouse.capacity);

    warehouse.createdAt = LocalDateTime.now();
    warehouse.archivedAt = null;

    // if all went well, create the warehouse
    warehouseStore.create(warehouse);
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
    int usedCapacity = warehousesAtLocation.stream().mapToInt(warehouse -> warehouse.capacity).sum();
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
