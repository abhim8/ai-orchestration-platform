package com.aiorchestration.gateway.exception;

/**
 * Thrown when the AI planner fails to generate a valid plan.
 * Wraps underlying Spring AI or framework exceptions.
 */
public class PlanGenerationException extends RuntimeException {

    public PlanGenerationException(final String message) {
        super(message);
    }

    public PlanGenerationException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
