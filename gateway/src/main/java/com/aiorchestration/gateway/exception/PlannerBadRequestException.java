package com.aiorchestration.gateway.exception;

public class PlannerBadRequestException extends RuntimeException {

    public PlannerBadRequestException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
