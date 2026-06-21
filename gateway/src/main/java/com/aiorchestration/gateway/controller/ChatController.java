package com.aiorchestration.gateway.controller;

import com.aiorchestration.common.constant.Headers;
import com.aiorchestration.common.context.ConversationContext;
import com.aiorchestration.gateway.model.ChatRequest;
import com.aiorchestration.gateway.model.ChatResponse;
import com.aiorchestration.gateway.service.ChatService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller exposing the chat endpoint.
 * Resolves the conversation identifier from the X-Conversation-Id header
 * and delegates all processing to {@link ChatService}.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ChatResponse chat(
            @RequestHeader(value = Headers.X_CONVERSATION_ID, required = false) final String conversationIdHeader,
            @RequestBody @Valid final ChatRequest request,
            final HttpServletResponse servletResponse) {

        var conversationId = resolveConversationId(conversationIdHeader);

        ConversationContext.setConversationId(conversationId);
        servletResponse.setHeader(Headers.X_CONVERSATION_ID, conversationId);

        log.debug("Received chat request: conversationId={}", conversationId);

        try {
            return chatService.chat(conversationId, request.message());
        } finally {
            ConversationContext.clear();
        }
    }

    static String resolveConversationId(final String headerValue) {
        if (headerValue != null && !headerValue.isBlank()) {
            try {
                UUID.fromString(headerValue.trim());
                return headerValue.trim();
            } catch (IllegalArgumentException ignored) {
            }
        }
        return UUID.randomUUID().toString();
    }
}
