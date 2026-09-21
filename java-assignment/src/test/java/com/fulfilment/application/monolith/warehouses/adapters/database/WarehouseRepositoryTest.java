package com.fulfilment.application.monolith.warehouses.adapters.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fulfilment.application.monolith.exceptions.ResourceNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class WarehouseRepositoryTest {

  @Inject WarehouseRepository warehouseRepository;

  @Test
  @TestTransaction
  public void shouldCreateAndFindAWarehouseByItsBusinessUnitCode() {
    var warehouse = new Warehouse("REPO.001", "EINDHOVEN-001", 20, 5);

    warehouseRepository.create(warehouse);

    assertNotNull(warehouse.id);
    assertNotNull(warehouse.createdAt);

    var stored = warehouseRepository.findByBusinessUnitCode("REPO.001");
    assertEquals("EINDHOVEN-001", stored.location);
    assertEquals(20, stored.capacity);
    assertEquals(warehouse.id, stored.id);
    assertEquals(stored.id, warehouseRepository.findActiveById(stored.id).id);
  }

  @Test
  @TestTransaction
  public void shouldUpdateAnActiveWarehouse() {
    var warehouse = new Warehouse("REPO.002", "EINDHOVEN-001", 20, 5);
    warehouseRepository.create(warehouse);

    warehouse.capacity = 25;
    warehouse.stock = 7;
    warehouseRepository.update(warehouse);

    var stored = warehouseRepository.findByBusinessUnitCode("REPO.002");
    assertEquals(25, stored.capacity);
    assertEquals(7, stored.stock);
  }

  @Test
  @TestTransaction
  public void shouldArchiveInsteadOfDeletingAWarehouse() {
    var warehouse = new Warehouse("REPO.003", "EINDHOVEN-001", 20, 5);
    warehouseRepository.create(warehouse);

    warehouseRepository.remove(warehouse);

    assertNotNull(warehouse.archivedAt);
    assertNull(warehouseRepository.findByBusinessUnitCode("REPO.003"));
    assertNull(warehouseRepository.findActiveById(warehouse.id));
    // the row is kept as history
    assertNotNull(warehouseRepository.findById(warehouse.id));
    assertTrue(
        warehouseRepository.getAll().stream()
            .noneMatch(w -> "REPO.003".equals(w.businessUnitCode)));
  }

  @Test
  public void shouldFailWhenUpdatingAWarehouseThatIsNotActive() {
    var unknown = new Warehouse("REPO.404", "EINDHOVEN-001", 20, 5);

    assertThrows(ResourceNotFoundException.class, () -> warehouseRepository.update(unknown));
    assertThrows(ResourceNotFoundException.class, () -> warehouseRepository.remove(unknown));
  }

  @Test
  public void shouldReturnNullForBlankOrUnknownIdentifiers() {
    assertNull(warehouseRepository.findByBusinessUnitCode(null));
    assertNull(warehouseRepository.findByBusinessUnitCode("  "));
    assertNull(warehouseRepository.findByBusinessUnitCode("REPO.404"));
    assertNull(warehouseRepository.findActiveById(999999L));
  }
}
