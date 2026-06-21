package com.aiorchestration.gateway.model;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

/**
 * Output produced by the intent planner after communicating with Gemini.
 * Designed for deserialization via Spring AI's {@code BeanOutputConverter}.
 *
 * @param confidence    the planner's confidence score for the generated plan (0.0 - 1.0)
 * @param summary       natural language summary of what the plan does
 * @param executionPlan the structured execution plan to run
 */
public record PlanGenerationResult(
    @DecimalMin(value = "0.0", inclusive = true)
    @DecimalMax(value = "1.0", inclusive = true)
    double confidence,
    String summary,
    ExecutionPlan executionPlan
) {}
