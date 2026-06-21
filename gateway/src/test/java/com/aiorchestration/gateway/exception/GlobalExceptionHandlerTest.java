package com.aiorchestration.gateway.exception;

import com.aiorchestration.gateway.controller.ChatController;
import com.aiorchestration.gateway.model.ChatRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
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
        when(request.getRequestURI()).thenReturn("/api/v1/chat");
    }

    @Test
    @DisplayName("should handle MethodArgumentNotValidException with field errors")
    void shouldHandleMethodArgumentNotValid() throws NoSuchMethodException {
        var method = ChatController.class.getMethod("chat", String.class, ChatRequest.class, HttpServletResponse.class);
        var parameter = new MethodParameter(method, 0);
        var target = new ChatRequest("");
        var bindingResult = new BeanPropertyBindingResult(target, "chatRequest");
        bindingResult.addError(new FieldError("chatRequest", "message", "", false,
                new String[]{"NotBlank"}, null, "must not be blank"));
        var ex = new MethodArgumentNotValidException(parameter, bindingResult);

        var response = handler.handleMethodArgumentNotValid(ex, request);

        assertEquals(400, response.getStatus());
        assertEquals("Validation failed", response.getMessage());
        assertEquals("test-trace-id", response.getTraceId());
        assertEquals("/api/v1/chat", response.getPath());
        assertNotNull(response.getValidationErrors());
        assertEquals(1, response.getValidationErrors().size());
        assertEquals("message", response.getValidationErrors().getFirst().field());
        assertEquals("must not be blank", response.getValidationErrors().getFirst().message());
    }

    @Test
    @DisplayName("should handle ConstraintViolationException")
    void shouldHandleConstraintViolation() {
        var ex = new ConstraintViolationException("Validation failed", Set.of());

        var response = handler.handleConstraintViolation(ex, request);

        assertEquals(400, response.getStatus());
        assertEquals("Constraint violation", response.getMessage());
        assertNotNull(response.getValidationErrors());
    }

    @Test
    @DisplayName("should handle MissingServletRequestParameterException")
    void shouldHandleMissingParameter() {
        var ex = new MissingServletRequestParameterException("message", "String");

        var response = handler.handleMissingServletRequestParameter(ex, request);

        assertEquals(400, response.getStatus());
        assertEquals("test-trace-id", response.getTraceId());
    }

    @Test
    @DisplayName("should handle MethodArgumentTypeMismatchException")
    void shouldHandleTypeMismatch() {
        var ex = new MethodArgumentTypeMismatchException("abc", Integer.class, "page", null, null);

        var response = handler.handleMethodArgumentTypeMismatch(ex, request);

        assertEquals(400, response.getStatus());
        assertEquals("Invalid parameter type: page", response.getMessage());
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
    @DisplayName("should handle HttpMessageConversionException")
    void shouldHandleMessageConversion() {
        var ex = new HttpMessageConversionException("Conversion error");

        var response = handler.handleHttpMessageConversion(ex, request);

        assertEquals(400, response.getStatus());
        assertEquals("Request body conversion failed", response.getMessage());
    }

    @Test
    @DisplayName("should handle ConversionFailedException")
    void shouldHandleConversionFailed() {
        var ex = new ConversionFailedException(
                TypeDescriptor.valueOf(String.class),
                TypeDescriptor.valueOf(Integer.class),
                "abc", null);

        var response = handler.handleConversionFailed(ex, request);

        assertEquals(400, response.getStatus());
        assertEquals("Parameter conversion failed", response.getMessage());
    }

    @Test
    @DisplayName("should handle IllegalArgumentException")
    void shouldHandleIllegalArgument() {
        var ex = new IllegalArgumentException("Invalid argument");

        var response = handler.handleIllegalArgument(ex, request);

        assertEquals(400, response.getStatus());
        assertEquals("Invalid argument", response.getMessage());
    }

    @Test
    @DisplayName("should handle PlanValidationException with 400")
    void shouldHandlePlanValidation() {
        var ex = new PlanValidationException("Confidence too low");

        var response = handler.handlePlanValidation(ex, request);

        assertEquals(400, response.getStatus());
        assertEquals("Bad Request", response.getError());
        assertEquals("Confidence too low", response.getMessage());
    }

    @Test
    @DisplayName("should handle PlannerBadRequestException with 400")
    void shouldHandlePlannerBadRequest() {
        var ex = new PlannerBadRequestException("Invalid request", new RuntimeException());

        var response = handler.handlePlannerBadRequest(ex, request);

        assertEquals(400, response.getStatus());
        assertEquals("Bad Request", response.getError());
        assertEquals("Invalid request", response.getMessage());
    }

    @Test
    @DisplayName("should handle PlannerQuotaExceededException with 429")
    void shouldHandlePlannerQuotaExceeded() {
        var ex = new PlannerQuotaExceededException("Quota exceeded", new RuntimeException());

        var response = handler.handlePlannerQuotaExceeded(ex, request);

        assertEquals(429, response.getStatus());
        assertEquals("Too Many Requests", response.getError());
        assertEquals("Quota exceeded", response.getMessage());
    }

    @Test
    @DisplayName("should handle PlannerAuthenticationException with 502")
    void shouldHandlePlannerAuthentication() {
        var ex = new PlannerAuthenticationException("Auth failed", new RuntimeException());

        var response = handler.handlePlannerAuthentication(ex, request);

        assertEquals(502, response.getStatus());
        assertEquals("Bad Gateway", response.getError());
        assertEquals("Auth failed", response.getMessage());
    }

    @Test
    @DisplayName("should handle PlannerUnavailableException with 503")
    void shouldHandlePlannerUnavailable() {
        var ex = new PlannerUnavailableException("Service down", new RuntimeException());

        var response = handler.handlePlannerUnavailable(ex, request);

        assertEquals(503, response.getStatus());
        assertEquals("Service Unavailable", response.getError());
        assertEquals("Service down", response.getMessage());
    }

    @Test
    @DisplayName("should handle PlanGenerationException with 500")
    void shouldHandlePlanGeneration() {
        var ex = new PlanGenerationException("AI service unavailable");

        var response = handler.handlePlanGeneration(ex, request);

        assertEquals(500, response.getStatus());
        assertEquals("Plan generation failed", response.getMessage());
    }

    @Test
    @DisplayName("should handle DownstreamServiceException with 502")
    void shouldHandleDownstreamService() {
        var ex = new DownstreamServiceException("Flight service timeout");

        var response = handler.handleDownstreamService(ex, request);

        assertEquals(502, response.getStatus());
        assertEquals("Downstream service error", response.getMessage());
    }

    @Test
    @DisplayName("should handle generic Exception with 500")
    void shouldHandleGenericException() {
        var ex = new RuntimeException("Unexpected failure");

        var response = handler.handleAll(ex, request);

        assertEquals(500, response.getStatus());
        assertEquals("An unexpected error occurred", response.getMessage());
        assertNull(response.getValidationErrors());
    }

    @Test
    @DisplayName("should include traceId from MDC")
    void shouldIncludeTraceId() {
        var ex = new IllegalArgumentException("test");

        var response = handler.handleIllegalArgument(ex, request);

        assertEquals("test-trace-id", response.getTraceId());
    }

    @Test
    @DisplayName("should include request path")
    void shouldIncludePath() {
        var ex = new IllegalArgumentException("test");

        var response = handler.handleIllegalArgument(ex, request);

        assertEquals("/api/v1/chat", response.getPath());
    }
}
