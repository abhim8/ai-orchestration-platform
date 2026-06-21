package com.aiorchestration.gateway.exception;

public class PlannerUnavailableException extends RuntimeException {

    public PlannerUnavailableException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
