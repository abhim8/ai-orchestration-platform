package com.aiorchestration.gateway.controller;

import com.aiorchestration.common.constant.Headers;
import com.aiorchestration.gateway.model.ChatRequest;
import com.aiorchestration.gateway.model.ChatResponse;
import com.aiorchestration.gateway.service.ChatService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    private static final String VALID_UUID = "550e8400-e29b-41d4-a716-446655440000";

    @Mock
    private ChatService chatService;

    @Mock
    private HttpServletResponse servletResponse;

    @Captor
    private ArgumentCaptor<String> conversationIdCaptor;

    @Captor
    private ArgumentCaptor<String> headerCaptor;

    private ChatController controller;

    private Validator validator;

    @BeforeEach
    void setUp() {
        controller = new ChatController(chatService);
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    @DisplayName("should delegate to ChatService with generated conversationId when header missing")
    void shouldGenerateConversationIdWhenHeaderMissing() {
        var request = new ChatRequest("book a flight");
        var response = new ChatResponse(false, List.of("step-1"), List.of(),
                List.of(), null, "Done", false, null);

        when(chatService.chat(anyString(), eq("book a flight"))).thenReturn(response);

        var result = controller.chat(null, request, servletResponse);

        assertNotNull(result);
        assertEquals("Done", result.summary());

        verify(servletResponse).setHeader(eq(Headers.X_CONVERSATION_ID), conversationIdCaptor.capture());
        assertNotNull(UUID.fromString(conversationIdCaptor.getValue()));

        verify(chatService).chat(conversationIdCaptor.getValue(), "book a flight");
    }

    @Test
    @DisplayName("should generate conversationId when header is blank")
    void shouldGenerateConversationIdWhenHeaderBlank() {
        var request = new ChatRequest("book a flight");
        var response = new ChatResponse(false, List.of("step-1"), List.of(),
                List.of(), null, "Done", false, null);

        when(chatService.chat(anyString(), eq("book a flight"))).thenReturn(response);

        var result = controller.chat("   ", request, servletResponse);

        assertNotNull(result);

        verify(servletResponse).setHeader(eq(Headers.X_CONVERSATION_ID), conversationIdCaptor.capture());
        var generatedId = conversationIdCaptor.getValue();
        assertNotNull(UUID.fromString(generatedId));

        verify(chatService).chat(generatedId, "book a flight");
    }

    @Test
    @DisplayName("should generate conversationId when header is invalid")
    void shouldGenerateConversationIdWhenHeaderInvalid() {
        var request = new ChatRequest("book a flight");
        var response = new ChatResponse(false, List.of("step-1"), List.of(),
                List.of(), null, "Done", false, null);

        when(chatService.chat(anyString(), eq("book a flight"))).thenReturn(response);

        var result = controller.chat("not-a-uuid", request, servletResponse);

        assertNotNull(result);

        verify(servletResponse).setHeader(eq(Headers.X_CONVERSATION_ID), conversationIdCaptor.capture());
        var generatedId = conversationIdCaptor.getValue();
        assertNotNull(UUID.fromString(generatedId));

        verify(chatService).chat(generatedId, "book a flight");
    }

    @Test
    @DisplayName("should use provided conversationId when valid UUID header")
    void shouldUseProvidedConversationIdWhenValid() {
        var request = new ChatRequest("book a flight");
        var response = new ChatResponse(false, List.of("step-1"), List.of(),
                List.of(), null, "Done", false, null);

        when(chatService.chat(VALID_UUID, "book a flight")).thenReturn(response);

        var result = controller.chat(VALID_UUID, request, servletResponse);

        assertNotNull(result);
        assertEquals("Done", result.summary());

        verify(servletResponse).setHeader(Headers.X_CONVERSATION_ID, VALID_UUID);
        verify(chatService).chat(VALID_UUID, "book a flight");
    }

    @Test
    @DisplayName("should echo back the provided conversationId in response header")
    void shouldEchoConversationIdInHeader() {
        var request = new ChatRequest("book a flight");
        var response = new ChatResponse(false, List.of("step-1"), List.of(),
                List.of(), null, "Done", false, null);

        when(chatService.chat(VALID_UUID, "book a flight")).thenReturn(response);

        controller.chat(VALID_UUID, request, servletResponse);

        verify(servletResponse).setHeader(Headers.X_CONVERSATION_ID, VALID_UUID);
    }

    @Test
    @DisplayName("should return clarification response from ChatService")
    void shouldReturnClarificationResponse() {
        var request = new ChatRequest("vague request");
        var response = new ChatResponse(false, List.of(), List.of(),
                List.of(), null, "Need more details", true, "Need more details");

        when(chatService.chat(anyString(), eq("vague request"))).thenReturn(response);

        var result = controller.chat(VALID_UUID, request, servletResponse);

        assertTrue(result.clarificationRequired());
        assertEquals("Need more details", result.clarificationMessage());
    }

    @Test
    @DisplayName("should reject blank message validation")
    void shouldRejectBlankMessage() {
        var request = new ChatRequest("");

        var violations = validator.validate(request);

        assertFalse(violations.isEmpty());
    }

    @Test
    @DisplayName("should accept valid request")
    void shouldAcceptValidRequest() {
        var request = new ChatRequest("book a flight");

        var violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("should populate ConversationContext and clear after processing")
    void shouldPopulateAndClearConversationContext() throws Exception {
        var request = new ChatRequest("book a flight");
        var response = new ChatResponse(false, List.of("step-1"), List.of(),
                List.of(), null, "Done", false, null);

        when(chatService.chat(anyString(), eq("book a flight"))).thenReturn(response);

        assertNull(MDC.get("conversationId"));

        controller.chat(VALID_UUID, request, servletResponse);

        assertNull(MDC.get("conversationId"), "ConversationContext should be cleared after processing");
    }

    @Test
    @DisplayName("should clear ConversationContext even when service throws")
    void shouldClearConversationContextOnException() {
        var request = new ChatRequest("book a flight");

        when(chatService.chat(anyString(), eq("book a flight"))).thenThrow(new RuntimeException("fail"));

        assertNull(MDC.get("conversationId"));

        try {
            controller.chat(VALID_UUID, request, servletResponse);
        } catch (RuntimeException ignored) {
        }

        assertNull(MDC.get("conversationId"), "ConversationContext should be cleared on exception");
    }
}
