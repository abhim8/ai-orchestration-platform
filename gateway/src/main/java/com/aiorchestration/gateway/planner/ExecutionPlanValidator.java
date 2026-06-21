package com.aiorchestration.gateway.planner;

import com.aiorchestration.gateway.exception.PlanValidationException;
import com.aiorchestration.gateway.model.ExecutionPlan;
import com.aiorchestration.gateway.model.ExecutionStep;
import com.aiorchestration.gateway.model.PlanGenerationResult;
import com.aiorchestration.gateway.registry.ToolRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Validates a {@link PlanGenerationResult} before execution begins.
 *
 * <p>Performs deterministic validation only — no tool execution, no
 * Spring AI calls, no downstream services, no plan modification.
 *
 * <p>Cycle detection uses <b>Kahn's algorithm</b> (topological sort):
 * <ol>
 *   <li>Compute in-degree (number of incoming edges) for each node</li>
 *   <li>Enqueue all nodes with in-degree 0 (no dependencies)</li>
 *   <li>While queue is non-empty, dequeue a node, decrement in-degree
 *       of its dependents; enqueue any that reach 0</li>
 *   <li>If all nodes are processed the graph is acyclic; otherwise a
 *       cycle exists</li>
 * </ol>
 * Time complexity: O(V + E) where V = steps, E = dependency edges.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExecutionPlanValidator {

    private static final double MINIMUM_CONFIDENCE = 0.5;

    private final ToolRegistry toolRegistry;

    /**
     * Validates the given plan, throwing {@link PlanValidationException}
     * if any validation rule is violated.
     */
    public void validate(final PlanGenerationResult result) {
        log.debug("Starting execution plan validation");

        validateNotNull(result);
        validateConfidence(result.confidence());
        validateExecutionPlan(result.executionPlan());

        var steps = result.executionPlan().steps();
        validateStepsNonEmpty(steps);
        validateNoDuplicateStepIds(steps);
        validateToolsExist(steps);
        validateRequiredArguments(steps);
        validateDependsOnReferences(steps);
        validateNoCycles(steps);

        log.debug("Execution plan validation passed successfully");
    }

    // ---- 1. Confidence threshold ----

    private void validateConfidence(final double confidence) {
        if (confidence < MINIMUM_CONFIDENCE) {
            log.warn("Validation failed: planner confidence {} is below minimum threshold {}", confidence, MINIMUM_CONFIDENCE);
            throw new PlanValidationException(
                "Planner confidence " + confidence + " is below minimum threshold " + MINIMUM_CONFIDENCE
            );
        }
    }

    // ---- 2. Null / empty checks ----

    private void validateNotNull(final PlanGenerationResult result) {
        if (result == null) {
            log.warn("Validation failed: PlanGenerationResult is null");
            throw new PlanValidationException("PlanGenerationResult must not be null");
        }
    }

    private void validateExecutionPlan(final ExecutionPlan executionPlan) {
        if (executionPlan == null) {
            log.warn("Validation failed: executionPlan is null");
            throw new PlanValidationException("ExecutionPlan must not be null");
        }
    }

    private static void validateStepsNonEmpty(final List<ExecutionStep> steps) {
        if (steps == null) {
            log.warn("Validation failed: step list is null");
            throw new PlanValidationException("Step list must not be null");
        }
        if (steps.isEmpty()) {
            log.warn("Validation failed: step list is empty");
            throw new PlanValidationException("Step list must not be empty");
        }
    }

    // ---- 3. Duplicate step IDs ----

    private static void validateNoDuplicateStepIds(final List<ExecutionStep> steps) {
        var seen = new HashSet<String>();
        for (var step : steps) {
            if (!seen.add(step.stepId())) {
                log.warn("Validation failed: duplicate stepId '{}'", step.stepId());
                throw new PlanValidationException("Duplicate stepId detected: " + step.stepId());
            }
        }
    }

    // ---- 4. Tool existence ----

    private void validateToolsExist(final List<ExecutionStep> steps) {
        for (var step : steps) {
            if (!toolRegistry.hasTool(step.tool())) {
                log.warn("Validation failed: unknown tool '{}'", step.tool());
                throw new PlanValidationException("Unknown tool: " + step.tool());
            }
        }
    }

    // ---- 5. Required arguments ----

    private void validateRequiredArguments(final List<ExecutionStep> steps) {
        for (var step : steps) {
            var required = toolRegistry.getRequiredArguments(step.tool());
            for (var arg : required) {
                if (step.arguments() == null || !step.arguments().containsKey(arg)) {
                    log.warn("Validation failed: missing required argument '{}' for tool '{}'", arg, step.tool());
                    throw new PlanValidationException(
                        "Missing required argument '" + arg + "' for tool '" + step.tool() + "'"
                    );
                }
            }
        }
    }

    // ---- 6. dependsOn references ----

    private static void validateDependsOnReferences(final List<ExecutionStep> steps) {
        var allStepIds = new HashSet<String>();
        for (var step : steps) {
            allStepIds.add(step.stepId());
        }
        for (var step : steps) {
            if (step.dependsOn() != null) {
                for (var dep : step.dependsOn()) {
                    if (!allStepIds.contains(dep)) {
                        log.warn("Validation failed: step '{}' depends on unknown step '{}'", step.stepId(), dep);
                        throw new PlanValidationException(
                            "Execution step '" + step.stepId() + "' depends on unknown step '" + dep + "'"
                        );
                    }
                }
            }
        }
    }

    // ---- 7. Cycle detection (Kahn's algorithm) ----

    private static void validateNoCycles(final List<ExecutionStep> steps) {
        /*
         * Kahn's algorithm for topological sort:
         *
         * 1. Compute in-degree for each node (number of steps that depend on it)
         * 2. Enqueue all nodes with in-degree 0
         * 3. Dequeue each node, decrement in-degree of its dependents,
         *    enqueue any that reach in-degree 0
         * 4. If processed count < total nodes, a cycle exists
         *
         * Building the adjacency list in reverse (dependent -> dependency)
         * lets us efficiently traverse dependents of a node.
         */

        var inDegree = new HashMap<String, Integer>();
        var dependents = new HashMap<String, Set<String>>();

        for (var step : steps) {
            inDegree.putIfAbsent(step.stepId(), 0);
            if (step.dependsOn() != null) {
                for (var dep : step.dependsOn()) {
                    dependents.computeIfAbsent(dep, k -> new HashSet<>()).add(step.stepId());
                }
            }
        }

        for (var step : steps) {
            if (step.dependsOn() != null) {
                inDegree.merge(step.stepId(), step.dependsOn().size(), Integer::sum);
            }
        }

        var queue = new ArrayDeque<String>();
        for (var entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        var processed = 0;
        while (!queue.isEmpty()) {
            var node = queue.poll();
            processed++;
            var deps = dependents.get(node);
            if (deps != null) {
                for (var dependent : deps) {
                    var remaining = inDegree.merge(dependent, -1, Integer::sum);
                    if (remaining == 0) {
                        queue.add(dependent);
                    }
                }
            }
        }

        if (processed != inDegree.size()) {
            log.warn("Validation failed: cycle detected in execution plan");
            throw new PlanValidationException("Cycle detected in execution plan");
        }
    }
}
