package com.fulfilment.application.monolith.fulfillment.adapters.warehouses;

import com.fulfilment.application.monolith.fulfillment.domain.ports.WarehouseResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Adapts the warehouse module to the {@link WarehouseResolver} port. Keeping the dependency in an
 * adapter means the fulfilment domain does not have to know how warehouses are stored, or that
 * "active" is expressed there as a warehouse that has not been archived.
 */
@ApplicationScoped
public class ActiveWarehouseGateway implements WarehouseResolver {

  @Inject WarehouseStore warehouseStore;

  @Override
  public boolean isActive(String businessUnitCode) {
    return warehouseStore.findByBusinessUnitCode(businessUnitCode) != null;
  }
}
