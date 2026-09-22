package com.fulfilment.application.monolith.warehouses.domain.ports;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import java.util.List;

public interface WarehouseStore {

  List<Warehouse> getAll();

  void create(Warehouse warehouse);

  void update(Warehouse warehouse);

  void remove(Warehouse warehouse);

  Warehouse findByBusinessUnitCode(String buCode);

  /**
   * Returns the active warehouse with the given technical id, or {@code null} when there is none.
   * Archived units are kept as history and are not part of the operational view.
   */
  Warehouse findActiveById(Long id);
}
