package com.aiorchestration.gateway.controller;

import com.aiorchestration.gateway.model.ChatRequest;
import com.aiorchestration.gateway.model.ChatResponse;
import com.aiorchestration.gateway.service.ChatService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private ChatService chatService;

    private ChatController controller;

    private Validator validator;

    @BeforeEach
    void setUp() {
        controller = new ChatController(chatService);
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    @DisplayName("should delegate to ChatService and return response")
    void shouldDelegateToChatService() {
        var request = new ChatRequest("session-1", "book a flight");
        var response = new ChatResponse(false, List.of("step-1"), List.of(),
                List.of(), null, "Done", false, null);

        when(chatService.chat(any(ChatRequest.class))).thenReturn(response);

        var result = controller.chat(request);

        assertNotNull(result);
        assertEquals("Done", result.summary());
        verify(chatService).chat(request);
    }

    @Test
    @DisplayName("should return clarification response from ChatService")
    void shouldReturnClarificationResponse() {
        var request = new ChatRequest("session-1", "vague request");
        var response = new ChatResponse(false, List.of(), List.of(),
                List.of(), null, "Need more details", true, "Need more details");

        when(chatService.chat(any(ChatRequest.class))).thenReturn(response);

        var result = controller.chat(request);

        assertTrue(result.clarificationRequired());
        assertEquals("Need more details", result.clarificationMessage());
    }

    @Test
    @DisplayName("should reject blank message validation")
    void shouldRejectBlankMessage() {
        var request = new ChatRequest("session-1", "");

        var violations = validator.validate(request);

        assertFalse(violations.isEmpty());
    }

    @Test
    @DisplayName("should accept valid request")
    void shouldAcceptValidRequest() {
        var request = new ChatRequest("session-1", "book a flight");

        var violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }
}