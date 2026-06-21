package com.aiorchestration.gateway.model;

import jakarta.validation.constraints.NotBlank;

/**
 * Incoming chat request from the user.
 *
 * @param message the natural language message from the user, must not be blank
 */
public record ChatRequest(
    @NotBlank String message
) {}
