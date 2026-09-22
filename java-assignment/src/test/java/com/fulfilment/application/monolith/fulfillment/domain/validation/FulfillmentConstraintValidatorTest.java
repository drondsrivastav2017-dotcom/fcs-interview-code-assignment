package com.fulfilment.application.monolith.fulfillment.domain.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.exceptions.ValidationException;
import com.fulfilment.application.monolith.fulfillment.domain.models.ProductReference;
import com.fulfilment.application.monolith.fulfillment.domain.models.StoreReference;
import com.fulfilment.application.monolith.fulfillment.domain.ports.FulfillmentStore;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The three colocation constraints, in isolation against a mocked store. */
public class FulfillmentConstraintValidatorTest {

  private static final StoreReference STORE = new StoreReference(1L, "TONSTAD");
  private static final ProductReference PRODUCT = new ProductReference(7L, "KALLAX");

  private FulfillmentStore fulfillmentStore;
  private FulfillmentConstraintValidator constraintValidator;

  @BeforeEach
  void setUp() {
    fulfillmentStore = mock(FulfillmentStore.class);
    constraintValidator = new FulfillmentConstraintValidator(fulfillmentStore);

    when(fulfillmentStore.exists(anyLong(), anyLong(), anyString())).thenReturn(false);
    when(fulfillmentStore.countWarehousesFulfillingProductInStore(anyLong(), anyLong()))
        .thenReturn(0L);
    when(fulfillmentStore.findWarehouseCodesFulfillingStore(anyLong())).thenReturn(List.of());
    when(fulfillmentStore.findProductIdsFulfilledByWarehouse(anyString())).thenReturn(List.of());
  }

  @Test
  void shouldAcceptAnAssociationThatBreaksNoConstraint() {
    assertDoesNotThrow(() -> constraintValidator.validate(STORE, PRODUCT, "MWH.001"));
  }

  @Test
  void shouldRejectAnAssociationThatAlreadyExists() {
    when(fulfillmentStore.exists(1L, 7L, "MWH.001")).thenReturn(true);

    assertEquals(
        "Product KALLAX is already fulfilled by warehouse MWH.001 for store TONSTAD.",
        assertThrows(
                ValidationException.class,
                () -> constraintValidator.validate(STORE, PRODUCT, "MWH.001"))
            .getMessage());
  }

  @Test
  void shouldRejectMoreThanTwoWarehousesPerProductInAStore() {
    when(fulfillmentStore.countWarehousesFulfillingProductInStore(1L, 7L)).thenReturn(2L);

    assertEquals(
        "Product KALLAX is already fulfilled by the maximum of 2 warehouses in store TONSTAD.",
        assertThrows(
                ValidationException.class,
                () -> constraintValidator.validate(STORE, PRODUCT, "MWH.001"))
            .getMessage());
  }

  @Test
  void shouldRejectMoreThanThreeWarehousesPerStore() {
    when(fulfillmentStore.findWarehouseCodesFulfillingStore(1L))
        .thenReturn(List.of("MWH.001", "MWH.012", "MWH.023"));

    assertEquals(
        "Store TONSTAD is already fulfilled by the maximum of 3 warehouses.",
        assertThrows(
                ValidationException.class,
                () -> constraintValidator.validate(STORE, PRODUCT, "MWH.500"))
            .getMessage());
  }

  @Test
  void shouldNotCountAWarehouseThatAlreadyFulfilsTheStoreTwice() {
    when(fulfillmentStore.findWarehouseCodesFulfillingStore(1L))
        .thenReturn(List.of("MWH.001", "MWH.012", "MWH.023"));

    // MWH.023 is already one of the three, so fulfilling another product from it stays within
    assertDoesNotThrow(() -> constraintValidator.validate(STORE, PRODUCT, "MWH.023"));
  }

  @Test
  void shouldRejectMoreThanFiveProductTypesPerWarehouse() {
    when(fulfillmentStore.findProductIdsFulfilledByWarehouse("MWH.001"))
        .thenReturn(List.of(1L, 2L, 3L, 4L, 5L));

    assertEquals(
        "Warehouse MWH.001 already stores the maximum of 5 product types.",
        assertThrows(
                ValidationException.class,
                () -> constraintValidator.validate(STORE, PRODUCT, "MWH.001"))
            .getMessage());
  }

  @Test
  void shouldNotCountAProductTypeTheWarehouseAlreadyStoresTwice() {
    when(fulfillmentStore.findProductIdsFulfilledByWarehouse("MWH.001"))
        .thenReturn(List.of(1L, 2L, 3L, 4L, PRODUCT.id()));

    // the same product type, fulfilled for another store, is not a sixth type
    assertDoesNotThrow(() -> constraintValidator.validate(STORE, PRODUCT, "MWH.001"));
  }
}
