package com.aiorchestration.gateway.service;

import com.aiorchestration.gateway.model.ChatRequest;
import com.aiorchestration.gateway.model.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Orchestrates the end-to-end chat flow:
 * intent planning, plan validation, execution, and response aggregation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    public ChatResponse chat(final ChatRequest request) {
        return null;
    }
}
