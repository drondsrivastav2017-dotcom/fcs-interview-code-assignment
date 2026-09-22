package com.fulfilment.application.monolith.fulfillment.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.exceptions.ResourceNotFoundException;
import com.fulfilment.application.monolith.exceptions.ValidationException;
import com.fulfilment.application.monolith.fulfillment.domain.events.FulfillmentChangedEvent;
import com.fulfilment.application.monolith.fulfillment.domain.models.Fulfillment;
import com.fulfilment.application.monolith.fulfillment.domain.models.ProductReference;
import com.fulfilment.application.monolith.fulfillment.domain.models.StoreReference;
import com.fulfilment.application.monolith.fulfillment.domain.ports.FulfillmentStore;
import com.fulfilment.application.monolith.fulfillment.domain.ports.ProductResolver;
import com.fulfilment.application.monolith.fulfillment.domain.ports.StoreResolver;
import com.fulfilment.application.monolith.fulfillment.domain.ports.WarehouseResolver;
import com.fulfilment.application.monolith.fulfillment.domain.validation.FulfillmentConstraintValidator;
import com.fulfilment.application.monolith.fulfillment.domain.validation.FulfillmentReferenceValidator;
import com.fulfilment.application.monolith.fulfillment.domain.validation.FulfillmentRequestValidator;
import jakarta.enterprise.event.Event;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** The association flow, against mocked ports: no database and no HTTP layer involved. */
public class AssociateFulfillmentUseCaseTest {

  private static final StoreReference STORE = new StoreReference(1L, "TONSTAD");
  private static final ProductReference PRODUCT = new ProductReference(7L, "KALLAX");

  private FulfillmentStore fulfillmentStore;
  private StoreResolver storeResolver;
  private ProductResolver productResolver;
  private WarehouseResolver warehouseResolver;

  @SuppressWarnings("unchecked")
  private final Event<FulfillmentChangedEvent> fulfillmentChanges = mock(Event.class);

  private AssociateFulfillmentUseCase associateFulfillmentUseCase;

  @BeforeEach
  void setUp() {
    fulfillmentStore = mock(FulfillmentStore.class);
    storeResolver = mock(StoreResolver.class);
    productResolver = mock(ProductResolver.class);
    warehouseResolver = mock(WarehouseResolver.class);

    associateFulfillmentUseCase =
        new AssociateFulfillmentUseCase(
            fulfillmentStore,
            new FulfillmentRequestValidator(),
            new FulfillmentReferenceValidator(storeResolver, productResolver, warehouseResolver),
            new FulfillmentConstraintValidator(fulfillmentStore),
            fulfillmentChanges);

    when(storeResolver.resolveById(1L)).thenReturn(STORE);
    when(productResolver.resolveById(7L)).thenReturn(PRODUCT);
    when(warehouseResolver.isActive("MWH.001")).thenReturn(true);
    when(fulfillmentStore.findWarehouseCodesFulfillingStore(1L)).thenReturn(List.of());
    when(fulfillmentStore.findProductIdsFulfilledByWarehouse("MWH.001")).thenReturn(List.of());
  }

  @Test
  void shouldAssociateAWarehouseAsFulfilmentUnit() {
    var fulfillment = associateFulfillmentUseCase.associate(1L, 7L, "MWH.001");

    assertEquals(STORE, fulfillment.store);
    assertEquals(PRODUCT, fulfillment.product);
    assertEquals("MWH.001", fulfillment.warehouseBusinessUnitCode);
    verify(fulfillmentStore).create(fulfillment);
  }

  @Test
  void shouldPublishTheAssociationOnlyOnceItIsStored() {
    var fulfillment = associateFulfillmentUseCase.associate(1L, 7L, "MWH.001");

    var published = ArgumentCaptor.forClass(FulfillmentChangedEvent.class);
    verify(fulfillmentChanges).fire(published.capture());

    assertEquals(FulfillmentChangedEvent.Operation.ASSOCIATED, published.getValue().operation());
    assertEquals(fulfillment, published.getValue().fulfillment());
  }

  @Test
  void shouldRejectAnIncompleteRequestBeforeLookingAnythingUp() {
    assertEquals(
        "storeId, productId and warehouseBusinessUnitCode are required.",
        assertThrows(
                ValidationException.class,
                () -> associateFulfillmentUseCase.associate(null, 7L, "MWH.001"))
            .getMessage());

    assertThrows(
        ValidationException.class, () -> associateFulfillmentUseCase.associate(1L, null, "MWH.001"));
    assertThrows(
        ValidationException.class, () -> associateFulfillmentUseCase.associate(1L, 7L, "  "));

    verify(storeResolver, never()).resolveById(any());
    verify(fulfillmentStore, never()).create(any());
  }

  @Test
  void shouldRejectAnUnknownStoreProductOrWarehouse() {
    assertEquals(
        "Store with id of 999 does not exist.",
        assertThrows(
                ResourceNotFoundException.class,
                () -> associateFulfillmentUseCase.associate(999L, 7L, "MWH.001"))
            .getMessage());

    assertEquals(
        "Product with id of 999 does not exist.",
        assertThrows(
                ResourceNotFoundException.class,
                () -> associateFulfillmentUseCase.associate(1L, 999L, "MWH.001"))
            .getMessage());

    assertEquals(
        "No active warehouse found with business unit code MWH.999.",
        assertThrows(
                ResourceNotFoundException.class,
                () -> associateFulfillmentUseCase.associate(1L, 7L, "MWH.999"))
            .getMessage());

    verify(fulfillmentStore, never()).create(any());
    verify(fulfillmentChanges, never()).fire(any());
  }

  @Test
  void shouldNotStoreNorPublishAnAssociationThatBreaksAConstraint() {
    when(fulfillmentStore.countWarehousesFulfillingProductInStore(1L, 7L)).thenReturn(2L);

    assertThrows(
        ValidationException.class, () -> associateFulfillmentUseCase.associate(1L, 7L, "MWH.001"));

    verify(fulfillmentStore, never()).create(any());
    verify(fulfillmentChanges, never()).fire(any());
  }

  @Test
  void shouldCarryTheIdentifierAssignedByTheStore() {
    doAnswer(
            invocation -> {
              invocation.getArgument(0, Fulfillment.class).id = 42L;
              return null;
            })
        .when(fulfillmentStore)
        .create(any());

    assertEquals(42L, associateFulfillmentUseCase.associate(1L, 7L, "MWH.001").id);
  }
}
