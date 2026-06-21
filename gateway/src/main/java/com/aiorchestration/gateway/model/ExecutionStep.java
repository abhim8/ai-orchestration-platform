package com.aiorchestration.gateway.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A single atomic step within an execution plan.
 *
 * @param stepId    unique identifier for this step within the plan
 * @param tool      the tool (e.g. flight, weather) responsible for executing this step
 * @param arguments parameters to pass to the tool when executing
 * @param dependsOn list of stepIds that must complete before this step can run
 */
public record ExecutionStep(
    String stepId,
    String tool,
    Map<String, Object> arguments,
    List<String> dependsOn
) {

    public ExecutionStep {
        arguments = arguments != null
            ? Collections.unmodifiableMap(arguments)
            : Map.of();
        dependsOn = dependsOn != null
            ? Collections.unmodifiableList(dependsOn)
            : List.of();
    }
}
