package com.aiorchestration.gateway.controller;

import com.aiorchestration.gateway.model.ChatRequest;
import com.aiorchestration.gateway.model.ChatResponse;
import com.aiorchestration.gateway.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing the chat endpoint.
 * Delegates all processing to {@link ChatService}.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ChatResponse chat(@RequestBody @Valid final ChatRequest request) {
        log.debug("Received chat request: sessionId={}", request.sessionId());
        var response = chatService.chat(request);
        log.debug("Chat delegation completed: clarificationRequired={}", response.clarificationRequired());
        return response;
    }
}
