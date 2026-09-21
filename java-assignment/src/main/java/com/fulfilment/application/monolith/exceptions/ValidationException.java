package com.fulfilment.application.monolith.exceptions;

/** Raised when a request breaks a business rule. Mapped to HTTP 400 by {@link ExceptionMappers}. */
public class ValidationException extends RuntimeException {

  public ValidationException(String message) {
    super(message);
  }
}
