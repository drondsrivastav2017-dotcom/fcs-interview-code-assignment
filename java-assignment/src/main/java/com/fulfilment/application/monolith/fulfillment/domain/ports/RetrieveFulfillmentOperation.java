package com.fulfilment.application.monolith.fulfillment.domain.ports;

import com.fulfilment.application.monolith.fulfillment.domain.models.Fulfillment;
import java.util.List;

/** Driving port: read the fulfilment associations, as a whole or per store or warehouse. */
public interface RetrieveFulfillmentOperation {

  List<Fulfillment> listAll();

  List<Fulfillment> listByStore(Long storeId);

  List<Fulfillment> listByWarehouse(String businessUnitCode);
}
