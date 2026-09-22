package com.fulfilment.application.monolith.fulfillment.domain.ports;

import com.fulfilment.application.monolith.fulfillment.domain.models.Fulfillment;
import java.util.List;

/**
 * Driven port towards the persistence of the fulfilment associations. The counting operations are
 * part of the port because the constraints they support are counted much more cheaply by the
 * database than by loading every association into memory.
 */
public interface FulfillmentStore {

  List<Fulfillment> getAll();

  List<Fulfillment> listByStore(Long storeId);

  List<Fulfillment> listByWarehouse(String businessUnitCode);

  /** Returns the association with the given technical id, or {@code null} when there is none. */
  Fulfillment findByIdentifier(Long id);

  void create(Fulfillment fulfillment);

  void remove(Fulfillment fulfillment);

  boolean exists(Long storeId, Long productId, String businessUnitCode);

  long countWarehousesFulfillingProductInStore(Long storeId, Long productId);

  List<String> findWarehouseCodesFulfillingStore(Long storeId);

  List<Long> findProductIdsFulfilledByWarehouse(String businessUnitCode);
}
