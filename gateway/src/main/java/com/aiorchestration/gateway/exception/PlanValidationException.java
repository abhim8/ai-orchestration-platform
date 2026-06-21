package com.aiorchestration.gateway.exception;

/**
 * Thrown when an execution plan fails validation.
 */
public class PlanValidationException extends RuntimeException {

    public PlanValidationException(final String message) {
        super(message);
    }

    public PlanValidationException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
