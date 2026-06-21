package com.aiorchestration.weather.exception;

public class ForecastRetrievalException extends RuntimeException {

    public ForecastRetrievalException(final String message) {
        super(message);
    }

    public ForecastRetrievalException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
