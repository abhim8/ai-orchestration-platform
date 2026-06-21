package com.aiorchestration.gateway.exception;

/**
 * Thrown when a downstream service request fails (network error,
 * non-2xx response, or deserialization failure). Wraps the
 * underlying exception so callers do not depend on RestClient-specific
 * exception types.
 */
public class DownstreamServiceException extends RuntimeException {

    public DownstreamServiceException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public DownstreamServiceException(final String message) {
        super(message);
    }
}
