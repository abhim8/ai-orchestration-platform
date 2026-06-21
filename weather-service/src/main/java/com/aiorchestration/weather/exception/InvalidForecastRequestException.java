package com.aiorchestration.weather.exception;

public class InvalidForecastRequestException extends RuntimeException {

    public InvalidForecastRequestException(final String message) {
        super(message);
    }
}
