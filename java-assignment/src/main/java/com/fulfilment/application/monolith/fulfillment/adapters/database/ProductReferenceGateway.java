package com.fulfilment.application.monolith.fulfillment.adapters.database;

import com.fulfilment.application.monolith.fulfillment.domain.models.ProductReference;
import com.fulfilment.application.monolith.fulfillment.domain.ports.ProductResolver;
import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.products.ProductRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Resolves products for the fulfilment domain, through the repository the product module already
 * exposes.
 */
@ApplicationScoped
public class ProductReferenceGateway implements ProductResolver {

  @Inject ProductRepository productRepository;

  @Override
  public ProductReference resolveById(Long productId) {
    if (productId == null) {
      return null;
    }

    Product product = productRepository.findById(productId);

    return product == null ? null : new ProductReference(product.id, product.name);
  }
}
