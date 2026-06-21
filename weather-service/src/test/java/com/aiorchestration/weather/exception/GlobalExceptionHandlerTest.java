package com.aiorchestration.weather.exception;

import com.aiorchestration.weather.controller.WeatherController;
import com.aiorchestration.weather.model.WeatherForecastRequest;
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
        when(request.getRequestURI()).thenReturn("/api/v1/weather/forecast");
    }

    @Test
    @DisplayName("should handle MethodArgumentNotValidException")
    void shouldHandleMethodArgumentNotValid() throws NoSuchMethodException {
        var method = WeatherController.class.getMethod("getForecast", WeatherForecastRequest.class);
        var parameter = new MethodParameter(method, 0);
        var target = new WeatherForecastRequest("", LocalDate.of(2026, 7, 15));
        var bindingResult = new BeanPropertyBindingResult(target, "request");
        bindingResult.addError(new FieldError("request", "location", "", false,
                new String[]{"NotBlank"}, null, "must not be blank"));
        var ex = new MethodArgumentNotValidException(parameter, bindingResult);

        var response = handler.handleMethodArgumentNotValid(ex, request);

        assertEquals(400, response.getStatus());
        assertEquals("Validation failed", response.getMessage());
        assertEquals("test-trace-id", response.getTraceId());
        assertNotNull(response.getValidationErrors());
        assertEquals(1, response.getValidationErrors().size());
        assertEquals("location", response.getValidationErrors().getFirst().field());
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
        var ex = new MissingServletRequestParameterException("location", "String");

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
    @DisplayName("should handle LocationNotFoundException with 404")
    void shouldHandleLocationNotFound() {
        var ex = new LocationNotFoundException("Location not found: Nowhere");

        var response = handler.handleLocationNotFound(ex, request);

        assertEquals(404, response.getStatus());
        assertEquals("Location not found: Nowhere", response.getMessage());
    }

    @Test
    @DisplayName("should handle InvalidForecastRequestException with 400")
    void shouldHandleInvalidForecastRequest() {
        var ex = new InvalidForecastRequestException("Past date not supported");

        var response = handler.handleInvalidForecastRequest(ex, request);

        assertEquals(400, response.getStatus());
        assertEquals("Past date not supported", response.getMessage());
    }

    @Test
    @DisplayName("should handle ForecastRateLimitException with 429")
    void shouldHandleForecastRateLimit() {
        var ex = new ForecastRateLimitException("Rate limit exceeded");

        var response = handler.handleForecastRateLimit(ex, request);

        assertEquals(429, response.getStatus());
        assertEquals("Rate limit exceeded", response.getMessage());
    }

    @Test
    @DisplayName("should handle ForecastRetrievalException with 503")
    void shouldHandleForecastRetrieval() {
        var ex = new ForecastRetrievalException("Open-Meteo API timeout");

        var response = handler.handleForecastRetrieval(ex, request);

        assertEquals(503, response.getStatus());
        assertEquals("Weather service is temporarily unavailable", response.getMessage());
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
        var ex = new LocationNotFoundException("Not found");

        var response = handler.handleLocationNotFound(ex, request);

        assertEquals("test-trace-id", response.getTraceId());
    }

    @Test
    @DisplayName("should include request path")
    void shouldIncludePath() {
        var ex = new LocationNotFoundException("Not found");

        var response = handler.handleLocationNotFound(ex, request);

        assertEquals("/api/v1/weather/forecast", response.getPath());
    }
}
