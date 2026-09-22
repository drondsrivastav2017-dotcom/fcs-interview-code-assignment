package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.exceptions.ResourceNotFoundException;
import com.fulfilment.application.monolith.exceptions.ValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import com.fulfilment.application.monolith.warehouses.domain.validation.WarehouseArchiveValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class ArchiveWarehouseUseCaseTest {

  private WarehouseStore warehouseStore;
  private ArchiveWarehouseUseCase archiveWarehouseUseCase;

  @BeforeEach
  void setUp() {
    warehouseStore = mock(WarehouseStore.class);
    archiveWarehouseUseCase =
        new ArchiveWarehouseUseCase(warehouseStore, new WarehouseArchiveValidator(warehouseStore));
  }

  @Test
  void shouldArchiveAnActiveWarehouse() {
    var existing = new Warehouse("MWH.001", "ZWOLLE-001", 30, 10);
    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(existing);

    archiveWarehouseUseCase.archive(new Warehouse("MWH.001", null, null, null));

    var archived = ArgumentCaptor.forClass(Warehouse.class);
    verify(warehouseStore).remove(archived.capture());
    assertNotNull(archived.getValue().archivedAt);
    assertEquals("MWH.001", archived.getValue().businessUnitCode);
  }

  @Test
  void shouldRejectArchivingWithoutABusinessUnitCode() {
    assertEquals(
        "Business unit code is required to archive a warehouse.",
        assertThrows(ValidationException.class, () -> archiveWarehouseUseCase.archive(null))
            .getMessage());

    assertThrows(
        ValidationException.class,
        () -> archiveWarehouseUseCase.archive(new Warehouse(" ", null, null, null)));

    verify(warehouseStore, never()).remove(any());
  }

  @Test
  void shouldRejectArchivingAWarehouseThatIsNotActive() {
    when(warehouseStore.findByBusinessUnitCode("MWH.999")).thenReturn(null);

    assertEquals(
        "No active warehouse found with business unit code MWH.999.",
        assertThrows(
                ResourceNotFoundException.class,
                () -> archiveWarehouseUseCase.archive(new Warehouse("MWH.999", null, null, null)))
            .getMessage());
    verify(warehouseStore, never()).remove(any());
  }
}
