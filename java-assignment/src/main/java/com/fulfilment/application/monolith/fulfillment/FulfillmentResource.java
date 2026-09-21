package com.fulfilment.application.monolith.fulfillment;

import com.fulfilment.application.monolith.exceptions.ValidationException;
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

@Path("fulfillment")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
public class FulfillmentResource {

  @Inject FulfillmentService fulfillmentService;

  @GET
  public List<FulfillmentResponse> listAll() {
    return fulfillmentService.listAll().stream().map(FulfillmentResponse::from).toList();
  }

  @GET
  @Path("store/{storeId}")
  public List<FulfillmentResponse> listByStore(@PathParam("storeId") Long storeId) {
    return fulfillmentService.listByStore(storeId).stream()
        .map(FulfillmentResponse::from)
        .toList();
  }

  @GET
  @Path("warehouse/{businessUnitCode}")
  public List<FulfillmentResponse> listByWarehouse(
      @PathParam("businessUnitCode") String businessUnitCode) {
    return fulfillmentService.listByWarehouse(businessUnitCode).stream()
        .map(FulfillmentResponse::from)
        .toList();
  }

  @POST
  public Response associate(FulfillmentRequest request) {
    if (request == null) {
      throw new ValidationException("Fulfillment data is required.");
    }

    var fulfillment =
        fulfillmentService.associate(
            request.storeId(), request.productId(), request.warehouseBusinessUnitCode());

    return Response.status(Response.Status.CREATED)
        .entity(FulfillmentResponse.from(fulfillment))
        .build();
  }

  @DELETE
  @Path("{id}")
  public Response remove(@PathParam("id") Long id) {
    fulfillmentService.remove(id);
    return Response.noContent().build();
  }
}
