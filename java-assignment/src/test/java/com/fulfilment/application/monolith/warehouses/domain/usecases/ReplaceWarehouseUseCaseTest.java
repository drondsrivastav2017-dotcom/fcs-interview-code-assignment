package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.exceptions.ResourceNotFoundException;
import com.fulfilment.application.monolith.exceptions.ValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import com.fulfilment.application.monolith.warehouses.domain.validation.WarehousePayloadValidator;
import com.fulfilment.application.monolith.warehouses.domain.validation.WarehouseReplacementValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ReplaceWarehouseUseCaseTest {

  private WarehouseStore warehouseStore;
  private ArchiveWarehouseOperation archiveWarehouseOperation;
  private CreateWarehouseOperation createWarehouseOperation;
  private ReplaceWarehouseUseCase replaceWarehouseUseCase;

  @BeforeEach
  void setUp() {
    warehouseStore = mock(WarehouseStore.class);
    archiveWarehouseOperation = mock(ArchiveWarehouseOperation.class);
    createWarehouseOperation = mock(CreateWarehouseOperation.class);
    replaceWarehouseUseCase =
        new ReplaceWarehouseUseCase(
            new WarehouseReplacementValidator(new WarehousePayloadValidator(), warehouseStore),
            archiveWarehouseOperation,
            createWarehouseOperation);
  }

  @Test
  void shouldArchiveThePreviousWarehouseBeforeCreatingTheNewOne() {
    var previous = new Warehouse("MWH.001", "ZWOLLE-001", 30, 10);
    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(previous);

    var replacement = new Warehouse("MWH.001", "ZWOLLE-001", 40, 10);

    replaceWarehouseUseCase.replace(replacement);

    var order = inOrder(archiveWarehouseOperation, createWarehouseOperation);
    order.verify(archiveWarehouseOperation).archive(previous);
    order.verify(createWarehouseOperation).create(replacement);
  }

  @Test
  void shouldRejectReplacementOfAnUnknownBusinessUnitCode() {
    when(warehouseStore.findByBusinessUnitCode("MWH.999")).thenReturn(null);

    var replacement = new Warehouse("MWH.999", "ZWOLLE-001", 40, 10);

    assertEquals(
        "No active warehouse found with business unit code MWH.999.",
        assertThrows(
                ResourceNotFoundException.class, () -> replaceWarehouseUseCase.replace(replacement))
            .getMessage());
    verify(archiveWarehouseOperation, never()).archive(any());
    verify(createWarehouseOperation, never()).create(any());
  }

  @Test
  void shouldRejectReplacementThatCannotAccommodateThePreviousStock() {
    var previous = new Warehouse("MWH.001", "ZWOLLE-001", 30, 25);
    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(previous);

    var replacement = new Warehouse("MWH.001", "ZWOLLE-001", 20, 20);

    assertEquals(
        "The new warehouse capacity of 20 cannot accommodate the 25 items of the warehouse being"
            + " replaced.",
        assertThrows(ValidationException.class, () -> replaceWarehouseUseCase.replace(replacement))
            .getMessage());
    verify(archiveWarehouseOperation, never()).archive(any());
  }

  @Test
  void shouldRejectReplacementWithADifferentStock() {
    var previous = new Warehouse("MWH.001", "ZWOLLE-001", 30, 10);
    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(previous);

    var replacement = new Warehouse("MWH.001", "ZWOLLE-001", 40, 12);

    assertEquals(
        "The new warehouse stock of 12 does not match the 10 items of the warehouse being replaced.",
        assertThrows(ValidationException.class, () -> replaceWarehouseUseCase.replace(replacement))
            .getMessage());
    verify(createWarehouseOperation, never()).create(any());
  }

  @Test
  void shouldRejectReplacementWithAnInvalidPayload() {
    var replacement = new Warehouse("MWH.001", "ZWOLLE-001", null, 10);

    assertThrows(ValidationException.class, () -> replaceWarehouseUseCase.replace(replacement));
    verify(warehouseStore, never()).findByBusinessUnitCode(any());
  }
}
