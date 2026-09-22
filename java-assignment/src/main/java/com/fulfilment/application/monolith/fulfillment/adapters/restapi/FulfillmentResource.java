package com.fulfilment.application.monolith.fulfillment.adapters.restapi;

import com.fulfilment.application.monolith.exceptions.ValidationException;
import com.fulfilment.application.monolith.fulfillment.domain.models.Fulfillment;
import com.fulfilment.application.monolith.fulfillment.domain.ports.AssociateFulfillmentOperation;
import com.fulfilment.application.monolith.fulfillment.domain.ports.RemoveFulfillmentOperation;
import com.fulfilment.application.monolith.fulfillment.domain.ports.RetrieveFulfillmentOperation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.util.List;

/**
 * REST adapter of the fulfilment API. It maps the request and response beans to the driving ports
 * and holds no rule of its own.
 */
@Path("fulfillment")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
public class FulfillmentResource {

  @Inject RetrieveFulfillmentOperation retrieveFulfillmentOperation;

  @Inject AssociateFulfillmentOperation associateFulfillmentOperation;

  @Inject RemoveFulfillmentOperation removeFulfillmentOperation;

  @GET
  public List<FulfillmentResponse> listAll() {
    return toResponses(retrieveFulfillmentOperation.listAll());
  }

  @GET
  @Path("store/{storeId}")
  public List<FulfillmentResponse> listByStore(@PathParam("storeId") Long storeId) {
    return toResponses(retrieveFulfillmentOperation.listByStore(storeId));
  }

  @GET
  @Path("warehouse/{businessUnitCode}")
  public List<FulfillmentResponse> listByWarehouse(
      @PathParam("businessUnitCode") String businessUnitCode) {
    return toResponses(retrieveFulfillmentOperation.listByWarehouse(businessUnitCode));
  }

  @POST
  public Response associate(FulfillmentRequest request) {
    if (request == null) {
      throw new ValidationException("Fulfillment data is required.");
    }

    Fulfillment fulfillment =
        associateFulfillmentOperation.associate(
            request.storeId(), request.productId(), request.warehouseBusinessUnitCode());

    return Response.status(Response.Status.CREATED)
        .entity(FulfillmentResponse.from(fulfillment))
        .build();
  }

  @DELETE
  @Path("{id}")
  public Response remove(@PathParam("id") Long id) {
    removeFulfillmentOperation.remove(id);
    return Response.noContent().build();
  }

  private static List<FulfillmentResponse> toResponses(List<Fulfillment> fulfillments) {
    return fulfillments.stream().map(FulfillmentResponse::from).toList();
  }
}
