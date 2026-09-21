package com.fulfilment.application.monolith.exceptions;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

/**
 * Translates the domain exceptions into HTTP responses. Keeping the mapping here lets the domain
 * layer stay free of JAX-RS types while still producing meaningful status codes.
 */
public final class ExceptionMappers {

  private static final Logger LOGGER = Logger.getLogger(ExceptionMappers.class.getName());

  private ExceptionMappers() {}

  public record ErrorPayload(String exceptionType, int code, String error) {}

  private static Response toResponse(Exception exception, Response.Status status) {
    LOGGER.debugf(exception, "Request rejected with status %s", status.getStatusCode());

    return Response.status(status)
        .entity(
            new ErrorPayload(
                exception.getClass().getName(), status.getStatusCode(), exception.getMessage()))
        .build();
  }

  @Provider
  public static class ValidationExceptionMapper implements ExceptionMapper<ValidationException> {

    @Override
    public Response toResponse(ValidationException exception) {
      return ExceptionMappers.toResponse(exception, Response.Status.BAD_REQUEST);
    }
  }

  @Provider
  public static class ResourceNotFoundExceptionMapper
      implements ExceptionMapper<ResourceNotFoundException> {

    @Override
    public Response toResponse(ResourceNotFoundException exception) {
      return ExceptionMappers.toResponse(exception, Response.Status.NOT_FOUND);
    }
  }
}
