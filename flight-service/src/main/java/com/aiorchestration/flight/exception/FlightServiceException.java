package com.aiorchestration.flight.exception;

public class FlightServiceException extends RuntimeException {

    public FlightServiceException(final String message) {
        super(message);
    }

    public FlightServiceException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
