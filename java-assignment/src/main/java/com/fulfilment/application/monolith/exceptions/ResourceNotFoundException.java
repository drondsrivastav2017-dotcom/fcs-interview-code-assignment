package com.fulfilment.application.monolith.exceptions;

/**
 * Raised when an operation targets an entity that does not exist. Mapped to HTTP 404 by {@link
 * ExceptionMappers}.
 */
public class ResourceNotFoundException extends RuntimeException {

  public ResourceNotFoundException(String message) {
    super(message);
  }
}
