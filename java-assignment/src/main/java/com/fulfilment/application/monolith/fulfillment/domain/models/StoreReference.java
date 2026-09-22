package com.fulfilment.application.monolith.fulfillment.domain.models;

/**
 * The store of an association, reduced to what the fulfilment rules and responses need. The store
 * itself is owned by another module, so the fulfilment domain only keeps a reference to it.
 */
public record StoreReference(Long id, String name) {}
