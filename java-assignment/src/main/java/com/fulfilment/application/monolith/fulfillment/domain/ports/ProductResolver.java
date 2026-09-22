package com.fulfilment.application.monolith.fulfillment.domain.ports;

import com.fulfilment.application.monolith.fulfillment.domain.models.ProductReference;

/** Driven port resolving the product an association refers to. */
public interface ProductResolver {

  /** Returns the product with the given id, or {@code null} when it does not exist. */
  ProductReference resolveById(Long productId);
}
