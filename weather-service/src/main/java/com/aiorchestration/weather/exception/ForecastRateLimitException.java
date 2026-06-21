package com.aiorchestration.weather.exception;

public class ForecastRateLimitException extends RuntimeException {

    public ForecastRateLimitException(final String message) {
        super(message);
    }
}
