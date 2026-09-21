package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

/**
 * The warehouse endpoints are generated from the OpenAPI specification, so the handler cannot
 * return a {@link Response} to set the 201 documented for the creation endpoint. This filter
 * restores it without diverging from the generated contract.
 */
@Provider
public class WarehouseCreationStatusFilter implements ContainerResponseFilter {

  private static final String WAREHOUSE_COLLECTION_PATH = "warehouse";

  @Override
  public void filter(
      ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
    if (HttpMethod.POST.equals(requestContext.getMethod())
        && responseContext.getStatus() == Response.Status.OK.getStatusCode()
        && WAREHOUSE_COLLECTION_PATH.equals(trimSlashes(requestContext.getUriInfo().getPath()))) {
      responseContext.setStatus(Response.Status.CREATED.getStatusCode());
    }
  }

  private static String trimSlashes(String path) {
    return path == null ? "" : path.replaceAll("^/+|/+$", "");
  }
}
