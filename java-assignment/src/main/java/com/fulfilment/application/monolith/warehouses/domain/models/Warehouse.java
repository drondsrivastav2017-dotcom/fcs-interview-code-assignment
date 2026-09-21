package com.fulfilment.application.monolith.warehouses.domain.models;

import java.time.LocalDateTime;

public class Warehouse {

  // technical identifier, assigned by the store when the warehouse is persisted
  public Long id;

  // unique identifier
  public String businessUnitCode;

  public String location;

  public Integer capacity;

  public Integer stock;

  public LocalDateTime createdAt;

  public LocalDateTime archivedAt;

  public Warehouse() {}

  public Warehouse(String businessUnitCode, String location, Integer capacity, Integer stock) {
    this.businessUnitCode = businessUnitCode;
    this.location = location;
    this.capacity = capacity;
    this.stock = stock;
  }

  public boolean isArchived() {
    return archivedAt != null;
  }
}
