package com.aiorchestration.flight.exception;

import com.aiorchestration.flight.controller.FlightController;
import com.aiorchestration.flight.model.FlightSearchRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        MDC.put("traceId", "test-trace-id");
        when(request.getRequestURI()).thenReturn("/api/v1/flights/search");
    }

    @Test
    @DisplayName("should handle MethodArgumentNotValidException")
    void shouldHandleMethodArgumentNotValid() throws NoSuchMethodException {
        var method = FlightController.class.getMethod("search", FlightSearchRequest.class);
        var parameter = new MethodParameter(method, 0);
        var target = new FlightSearchRequest("", "NRT", LocalDate.of(2026, 7, 15));
        var bindingResult = new BeanPropertyBindingResult(target, "request");
        bindingResult.addError(new FieldError("request", "origin", "", false,
                new String[]{"NotBlank"}, null, "must not be blank"));
        var ex = new MethodArgumentNotValidException(parameter, bindingResult);

        var response = handler.handleMethodArgumentNotValid(ex, request);

        assertEquals(400, response.getStatus());
        assertEquals("Validation failed", response.getMessage());
        assertEquals("test-trace-id", response.getTraceId());
        assertNotNull(response.getValidationErrors());
        assertEquals(1, response.getValidationErrors().size());
        assertEquals("origin", response.getValidationErrors().getFirst().field());
    }

    @Test
    @DisplayName("should handle ConstraintViolationException")
    void shouldHandleConstraintViolation() {
        var ex = new ConstraintViolationException("Validation failed", Set.of());

        var response = handler.handleConstraintViolation(ex, request);

        assertEquals(400, response.getStatus());
        assertEquals("Constraint violation", response.getMessage());
    }

    @Test
    @DisplayName("should handle MissingServletRequestParameterException")
    void shouldHandleMissingParameter() {
        var ex = new MissingServletRequestParameterException("origin", "String");

        var response = handler.handleMissingServletRequestParameter(ex, request);

        assertEquals(400, response.getStatus());
    }

    @Test
    @DisplayName("should handle HttpMessageNotReadableException")
    void shouldHandleMessageNotReadable() {
        var ex = new HttpMessageNotReadableException("Required request body is missing",
                mock(HttpInputMessage.class));

        var response = handler.handleHttpMessageNotReadable(ex, request);

        assertEquals(400, response.getStatus());
        assertEquals("Malformed request body", response.getMessage());
    }

    @Test
    @DisplayName("should handle FlightServiceException with 502")
    void shouldHandleFlightService() {
        var ex = new FlightServiceException("Amadeus API timeout");

        var response = handler.handleFlightService(ex, request);

        assertEquals(502, response.getStatus());
        assertEquals("Flight search service unavailable", response.getMessage());
    }

    @Test
    @DisplayName("should handle NoResourceFoundException with 404")
    void shouldHandleNoResourceFound() {
        var ex = new NoResourceFoundException(HttpMethod.GET, "/api/flights/search", "");

        var response = handler.handleNoResourceFound(ex, request);

        assertEquals(404, response.getStatus());
        assertEquals("Resource not found", response.getMessage());
    }

    @Test
    @DisplayName("should handle generic Exception with 500")
    void shouldHandleGenericException() {
        var ex = new RuntimeException("Unexpected failure");

        var response = handler.handleAll(ex, request);

        assertEquals(500, response.getStatus());
        assertEquals("An unexpected error occurred", response.getMessage());
    }

    @Test
    @DisplayName("should include traceId from MDC")
    void shouldIncludeTraceId() {
        var ex = new MissingServletRequestParameterException("origin", "String");

        var response = handler.handleMissingServletRequestParameter(ex, request);

        assertEquals("test-trace-id", response.getTraceId());
    }

    @Test
    @DisplayName("should include request path")
    void shouldIncludePath() {
        var ex = new MissingServletRequestParameterException("origin", "String");

        var response = handler.handleMissingServletRequestParameter(ex, request);

        assertEquals("/api/v1/flights/search", response.getPath());
    }
}
