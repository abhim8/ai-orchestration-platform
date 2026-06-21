package com.aiorchestration.gateway.exception;

public class PlannerQuotaExceededException extends RuntimeException {

    public PlannerQuotaExceededException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
