package com.fulfilment.application.monolith.fulfillment.domain.models;

/**
 * The product of an association, reduced to what the fulfilment rules and responses need. The
 * product itself is owned by another module, so the fulfilment domain only keeps a reference to it.
 */
public record ProductReference(Long id, String name) {}
