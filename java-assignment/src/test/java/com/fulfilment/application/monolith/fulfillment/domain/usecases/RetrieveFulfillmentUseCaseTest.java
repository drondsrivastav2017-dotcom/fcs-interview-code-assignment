package com.fulfilment.application.monolith.fulfillment.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.exceptions.ResourceNotFoundException;
import com.fulfilment.application.monolith.fulfillment.domain.models.Fulfillment;
import com.fulfilment.application.monolith.fulfillment.domain.models.ProductReference;
import com.fulfilment.application.monolith.fulfillment.domain.models.StoreReference;
import com.fulfilment.application.monolith.fulfillment.domain.ports.FulfillmentStore;
import com.fulfilment.application.monolith.fulfillment.domain.ports.ProductResolver;
import com.fulfilment.application.monolith.fulfillment.domain.ports.StoreResolver;
import com.fulfilment.application.monolith.fulfillment.domain.ports.WarehouseResolver;
import com.fulfilment.application.monolith.fulfillment.domain.validation.FulfillmentReferenceValidator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RetrieveFulfillmentUseCaseTest {

  private static final Fulfillment FULFILLMENT =
      new Fulfillment(
          new StoreReference(1L, "TONSTAD"), new ProductReference(7L, "KALLAX"), "MWH.001");

  private FulfillmentStore fulfillmentStore;
  private StoreResolver storeResolver;
  private WarehouseResolver warehouseResolver;
  private RetrieveFulfillmentUseCase retrieveFulfillmentUseCase;

  @BeforeEach
  void setUp() {
    fulfillmentStore = mock(FulfillmentStore.class);
    storeResolver = mock(StoreResolver.class);
    warehouseResolver = mock(WarehouseResolver.class);

    retrieveFulfillmentUseCase =
        new RetrieveFulfillmentUseCase(
            fulfillmentStore,
            new FulfillmentReferenceValidator(
                storeResolver, mock(ProductResolver.class), warehouseResolver));
  }

  @Test
  void shouldListEveryAssociation() {
    when(fulfillmentStore.getAll()).thenReturn(List.of(FULFILLMENT));

    assertEquals(List.of(FULFILLMENT), retrieveFulfillmentUseCase.listAll());
  }

  @Test
  void shouldListTheAssociationsOfAKnownStore() {
    when(storeResolver.resolveById(1L)).thenReturn(FULFILLMENT.store);
    when(fulfillmentStore.listByStore(1L)).thenReturn(List.of(FULFILLMENT));

    assertEquals(List.of(FULFILLMENT), retrieveFulfillmentUseCase.listByStore(1L));
  }

  @Test
  void shouldListTheAssociationsOfAnActiveWarehouse() {
    when(warehouseResolver.isActive("MWH.001")).thenReturn(true);
    when(fulfillmentStore.listByWarehouse("MWH.001")).thenReturn(List.of(FULFILLMENT));

    assertEquals(List.of(FULFILLMENT), retrieveFulfillmentUseCase.listByWarehouse("MWH.001"));
  }

  @Test
  void shouldReportAnUnknownStoreInsteadOfReturningAnEmptyList() {
    when(storeResolver.resolveById(999L)).thenReturn(null);

    assertThrows(
        ResourceNotFoundException.class, () -> retrieveFulfillmentUseCase.listByStore(999L));
    verify(fulfillmentStore, never()).listByStore(any());
  }

  @Test
  void shouldReportAWarehouseThatIsNotActiveInsteadOfReturningAnEmptyList() {
    when(warehouseResolver.isActive("MWH.999")).thenReturn(false);

    assertThrows(
        ResourceNotFoundException.class,
        () -> retrieveFulfillmentUseCase.listByWarehouse("MWH.999"));
    verify(fulfillmentStore, never()).listByWarehouse(anyString());
  }
}
