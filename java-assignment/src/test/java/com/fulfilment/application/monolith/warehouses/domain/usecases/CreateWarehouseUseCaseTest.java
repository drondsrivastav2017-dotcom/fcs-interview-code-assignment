package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.exceptions.ValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import com.fulfilment.application.monolith.warehouses.domain.validation.WarehouseCreationValidator;
import com.fulfilment.application.monolith.warehouses.domain.validation.WarehousePayloadValidator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CreateWarehouseUseCaseTest {

  private static final Location AMSTERDAM = new Location("AMSTERDAM-001", 2, 100);

  private WarehouseStore warehouseStore;
  private LocationResolver locationResolver;
  private CreateWarehouseUseCase createWarehouseUseCase;

  @BeforeEach
  void setUp() {
    warehouseStore = mock(WarehouseStore.class);
    locationResolver = mock(LocationResolver.class);
    createWarehouseUseCase =
        new CreateWarehouseUseCase(
            warehouseStore,
            new WarehouseCreationValidator(
                new WarehousePayloadValidator(), warehouseStore, locationResolver));

    when(locationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(AMSTERDAM);
    when(warehouseStore.getAll()).thenReturn(List.of());
  }

  @Test
  void shouldCreateWarehouseWhenAllConstraintsAreRespected() {
    var warehouse = new Warehouse("MWH.500", "AMSTERDAM-001", 50, 30);

    createWarehouseUseCase.create(warehouse);

    verify(warehouseStore).create(warehouse);
    assertNotNull(warehouse.createdAt);
  }

  @Test
  void shouldRejectWarehouseWithoutPayload() {
    assertEquals(
        "Warehouse data is required.",
        assertThrows(ValidationException.class, () -> createWarehouseUseCase.create(null))
            .getMessage());
  }

  @Test
  void shouldRejectWarehouseWithoutBusinessUnitCode() {
    var warehouse = new Warehouse(" ", "AMSTERDAM-001", 50, 30);

    assertThrows(ValidationException.class, () -> createWarehouseUseCase.create(warehouse));
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void shouldRejectWarehouseWithoutLocation() {
    var warehouse = new Warehouse("MWH.500", null, 50, 30);

    assertThrows(ValidationException.class, () -> createWarehouseUseCase.create(warehouse));
  }

  @Test
  void shouldRejectWarehouseWithNonPositiveCapacity() {
    var warehouse = new Warehouse("MWH.500", "AMSTERDAM-001", 0, 0);

    assertThrows(ValidationException.class, () -> createWarehouseUseCase.create(warehouse));
  }

  @Test
  void shouldRejectWarehouseWithNegativeStock() {
    var warehouse = new Warehouse("MWH.500", "AMSTERDAM-001", 50, -1);

    assertThrows(ValidationException.class, () -> createWarehouseUseCase.create(warehouse));
  }

  @Test
  void shouldRejectWarehouseWithStockAboveItsOwnCapacity() {
    var warehouse = new Warehouse("MWH.500", "AMSTERDAM-001", 10, 11);

    assertEquals(
        "Stock of 11 does not fit in the informed capacity of 10.",
        assertThrows(ValidationException.class, () -> createWarehouseUseCase.create(warehouse))
            .getMessage());
  }

  @Test
  void shouldRejectWarehouseWhenBusinessUnitCodeAlreadyExists() {
    when(warehouseStore.findByBusinessUnitCode("MWH.500"))
        .thenReturn(new Warehouse("MWH.500", "AMSTERDAM-001", 50, 30));

    var warehouse = new Warehouse("MWH.500", "AMSTERDAM-001", 50, 30);

    assertEquals(
        "A warehouse with business unit code MWH.500 already exists.",
        assertThrows(ValidationException.class, () -> createWarehouseUseCase.create(warehouse))
            .getMessage());
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void shouldRejectWarehouseOnUnknownLocation() {
    when(locationResolver.resolveByIdentifier(anyString())).thenReturn(null);

    var warehouse = new Warehouse("MWH.500", "ATLANTIS-001", 50, 30);

    assertEquals(
        "Location ATLANTIS-001 does not exist.",
        assertThrows(ValidationException.class, () -> createWarehouseUseCase.create(warehouse))
            .getMessage());
  }

  @Test
  void shouldRejectWarehouseWhenLocationAlreadyHostsTheMaximumNumberOfWarehouses() {
    when(warehouseStore.getAll())
        .thenReturn(
            List.of(
                new Warehouse("MWH.001", "AMSTERDAM-001", 10, 1),
                new Warehouse("MWH.002", "AMSTERDAM-001", 10, 1)));

    var warehouse = new Warehouse("MWH.500", "AMSTERDAM-001", 10, 1);

    assertEquals(
        "Location AMSTERDAM-001 already hosts the maximum of 2 warehouses.",
        assertThrows(ValidationException.class, () -> createWarehouseUseCase.create(warehouse))
            .getMessage());
  }

  @Test
  void shouldRejectWarehouseWhenLocationCapacityIsExceeded() {
    when(warehouseStore.getAll())
        .thenReturn(List.of(new Warehouse("MWH.001", "AMSTERDAM-001", 80, 10)));

    var warehouse = new Warehouse("MWH.500", "AMSTERDAM-001", 30, 10);

    assertEquals(
        "Capacity of 30 exceeds the 20 still available at location AMSTERDAM-001.",
        assertThrows(ValidationException.class, () -> createWarehouseUseCase.create(warehouse))
            .getMessage());
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void shouldOnlyAccountForWarehousesOfTheTargetLocation() {
    when(warehouseStore.getAll())
        .thenReturn(List.of(new Warehouse("MWH.001", "ZWOLLE-001", 90, 10)));

    var warehouse = new Warehouse("MWH.500", "AMSTERDAM-001", 90, 10);

    createWarehouseUseCase.create(warehouse);

    verify(warehouseStore).create(warehouse);
  }
}
