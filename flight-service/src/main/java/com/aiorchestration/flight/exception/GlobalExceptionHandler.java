package com.aiorchestration.flight.exception;

import com.aiorchestration.common.model.ErrorResponse;
import com.aiorchestration.common.model.ValidationError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMethodArgumentNotValid(final MethodArgumentNotValidException ex,
                                                       final HttpServletRequest request) {
        log.warn("Validation failed: {}", ex.getMessage());
        var validationErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ValidationError(fe.getField(), fe.getDefaultMessage(), fe.getRejectedValue()))
                .toList();
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Validation failed", request, validationErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleConstraintViolation(final ConstraintViolationException ex,
                                                    final HttpServletRequest request) {
        log.warn("Constraint violation: {}", ex.getMessage());
        var validationErrors = ex.getConstraintViolations().stream()
                .map(cv -> new ValidationError(cv.getPropertyPath().toString(), cv.getMessage(), cv.getInvalidValue()))
                .toList();
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Constraint violation", request, validationErrors);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMissingServletRequestParameter(final MissingServletRequestParameterException ex,
                                                               final HttpServletRequest request) {
        log.warn("Missing request parameter: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMethodArgumentTypeMismatch(final MethodArgumentTypeMismatchException ex,
                                                           final HttpServletRequest request) {
        log.warn("Type mismatch: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid parameter type: " + ex.getName(), request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleHttpMessageNotReadable(final HttpMessageNotReadableException ex,
                                                       final HttpServletRequest request) {
        log.warn("Malformed request body: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Malformed request body", request);
    }

    @ExceptionHandler(HttpMessageConversionException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleHttpMessageConversion(final HttpMessageConversionException ex,
                                                      final HttpServletRequest request) {
        log.warn("Message conversion error: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Request body conversion failed", request);
    }

    @ExceptionHandler(ConversionFailedException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleConversionFailed(final ConversionFailedException ex,
                                                 final HttpServletRequest request) {
        log.warn("Conversion failed: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Parameter conversion failed", request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgument(final IllegalArgumentException ex,
                                                final HttpServletRequest request) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(FlightServiceException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ErrorResponse handleFlightService(final FlightServiceException ex,
                                              final HttpServletRequest request) {
        log.error("Flight service error: {}", ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.BAD_GATEWAY, "Flight search service unavailable", request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNoResourceFound(final NoResourceFoundException ex,
                                                final HttpServletRequest request) {
        log.warn("Resource not found: {} {}", ex.getHttpMethod(), ex.getResourcePath());
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Resource not found", request);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleAll(final Exception ex, final HttpServletRequest request) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request);
    }

    private ErrorResponse buildErrorResponse(final HttpStatus status, final String message,
                                              final HttpServletRequest request) {
        return buildErrorResponse(status, message, request, null);
    }

    private ErrorResponse buildErrorResponse(final HttpStatus status, final String message,
                                              final HttpServletRequest request,
                                              final java.util.List<ValidationError> validationErrors) {
        return ErrorResponse.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .traceId(MDC.get("traceId"))
                .validationErrors(validationErrors)
                .build();
    }
}
