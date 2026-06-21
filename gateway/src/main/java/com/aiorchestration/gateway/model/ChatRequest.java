package com.aiorchestration.gateway.model;

import jakarta.validation.constraints.NotBlank;

/**
 * Incoming chat request from the user.
 *
 * @param sessionId optional session identifier for conversation continuity
 * @param message   the natural language message from the user, must not be blank
 */
public record ChatRequest(
    String sessionId,
    @NotBlank String message
) {}
