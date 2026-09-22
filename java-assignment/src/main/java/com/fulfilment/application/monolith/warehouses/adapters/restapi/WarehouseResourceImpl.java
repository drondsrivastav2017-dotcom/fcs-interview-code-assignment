package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fulfilment.application.monolith.exceptions.ResourceNotFoundException;
import com.fulfilment.application.monolith.exceptions.ValidationException;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import com.warehouse.api.WarehouseResource;
import com.warehouse.api.beans.Warehouse;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * REST adapter of the warehouse API. It only translates between the generated API beans and the
 * domain model: every rule lives behind the ports it depends on.
 */
@RequestScoped
public class WarehouseResourceImpl implements WarehouseResource {

  @Inject private WarehouseStore warehouseStore;

  @Inject private CreateWarehouseOperation createWarehouseOperation;

  @Inject private ReplaceWarehouseOperation replaceWarehouseOperation;

  @Inject private ArchiveWarehouseOperation archiveWarehouseOperation;

  @Override
  public List<Warehouse> listAllWarehousesUnits() {
    return warehouseStore.getAll().stream().map(this::toWarehouseResponse).toList();
  }

  @Override
  public Warehouse createANewWarehouseUnit(@NotNull Warehouse data) {
    var warehouse = toDomainWarehouse(data);

    createWarehouseOperation.create(warehouse);

    return toWarehouseResponse(warehouse);
  }

  @Override
  public Warehouse getAWarehouseUnitByID(String id) {
    return toWarehouseResponse(requireActiveWarehouse(id));
  }

  @Override
  public void archiveAWarehouseUnitByID(String id) {
    archiveWarehouseOperation.archive(requireActiveWarehouse(id));
  }

  @Override
  public Warehouse replaceTheCurrentActiveWarehouse(
      String businessUnitCode, @NotNull Warehouse data) {
    var newWarehouse = toDomainWarehouse(data);
    newWarehouse.businessUnitCode = businessUnitCode;

    replaceWarehouseOperation.replace(newWarehouse);

    return toWarehouseResponse(newWarehouse);
  }

  private com.fulfilment.application.monolith.warehouses.domain.models.Warehouse
      requireActiveWarehouse(String id) {
    var warehouse = warehouseStore.findActiveById(toIdentifier(id));

    if (warehouse == null) {
      throw new ResourceNotFoundException("Warehouse with id of " + id + " does not exist.");
    }

    return warehouse;
  }

  private Long toIdentifier(String id) {
    try {
      return Long.valueOf(id);
    } catch (NumberFormatException e) {
      throw new ValidationException("Warehouse id " + id + " is not a valid identifier.");
    }
  }

  private com.fulfilment.application.monolith.warehouses.domain.models.Warehouse toDomainWarehouse(
      Warehouse data) {
    if (data == null) {
      throw new ValidationException("Warehouse data is required.");
    }

    return new com.fulfilment.application.monolith.warehouses.domain.models.Warehouse(
        data.getBusinessUnitCode(), data.getLocation(), data.getCapacity(), data.getStock());
  }

  private Warehouse toWarehouseResponse(
      com.fulfilment.application.monolith.warehouses.domain.models.Warehouse warehouse) {
    var response = new Warehouse();
    response.setId(warehouse.id == null ? null : String.valueOf(warehouse.id));
    response.setBusinessUnitCode(warehouse.businessUnitCode);
    response.setLocation(warehouse.location);
    response.setCapacity(warehouse.capacity);
    response.setStock(warehouse.stock);

    return response;
  }
}
