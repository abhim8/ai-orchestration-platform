package com.aiorchestration.gateway.model;

import java.util.Collections;
import java.util.List;

/**
 * An ordered collection of steps to be executed by the execution engine.
 * Execution order is derived from each step's {@code dependsOn} references.
 *
 * @param steps the steps that make up this plan
 */
public record ExecutionPlan(
    List<ExecutionStep> steps
) {

    public ExecutionPlan {
        steps = steps != null
            ? Collections.unmodifiableList(steps)
            : List.of();
    }
}
