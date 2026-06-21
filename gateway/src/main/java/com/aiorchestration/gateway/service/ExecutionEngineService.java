package com.aiorchestration.gateway.service;

import com.aiorchestration.gateway.model.ExecutionPlan;
import com.aiorchestration.gateway.model.ExecutionStep;
import com.aiorchestration.gateway.model.StepResult;
import com.aiorchestration.gateway.model.StepStatus;
import com.aiorchestration.gateway.registry.ToolRegistry;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Executes validated execution plans deterministically.
 *
 * <p>Execution model:
 * <ol>
 *   <li>Compute a topological ordering of steps using Kahn's algorithm</li>
 *   <li>Build futures for each step in that order — steps with no
 *       unsatisfied dependencies produce futures immediately; steps
 *       with dependencies produce futures chained on their ancestors</li>
 *   <li>Collect all results by joining the futures</li>
 * </ol>
 *
 * <p>Parallelism is derived solely from {@code dependsOn}:
 * independent steps execute concurrently via {@link CompletableFuture},
 * while dependent steps automatically wait for their prerequisites.
 * No {@code executionMode} field, no Reactor, no virtual threads.
 *
 * <p>Time complexity: O(V + E) for graph traversal, excluding downstream
 * network calls made by tool executors.
 */
@Slf4j
@Service
public class ExecutionEngineService {

    private static final long STEP_NOT_EXECUTED_LATENCY = 0;

    private final ToolRegistry toolRegistry;
    private final Map<String, ToolExecutor> toolExecutors;

    public ExecutionEngineService(final ToolRegistry toolRegistry,
                                  final Map<String, ToolExecutor> toolExecutors) {
        this.toolRegistry = toolRegistry;
        this.toolExecutors = Collections.unmodifiableMap(toolExecutors);
    }

    /**
     * Execute every step in {@code plan}, respecting dependencies and
     * running independent steps concurrently.
     *
     * @param plan the validated execution plan
     * @return results in the same order as {@code plan.steps()}
     */
    public List<StepResult> execute(final ExecutionPlan plan) {
        log.debug("Starting execution of {} steps", plan.steps().size());

        var sorted = topologicalSort(plan.steps());

        var futures = new HashMap<String, CompletableFuture<StepResult>>();

        for (var step : sorted) {
            if (step.dependsOn().isEmpty()) {
                futures.put(step.stepId(), CompletableFuture.supplyAsync(() -> executeSingleStep(step)));
            } else {
                var depFutures = step.dependsOn().stream()
                        .map(futures::get)
                        .toArray(CompletableFuture[]::new);
                futures.put(step.stepId(),
                        CompletableFuture.allOf(depFutures)
                                .thenApplyAsync(v -> executeAfterDependencies(step, futures)));
            }
        }

        var results = plan.steps().stream()
                .map(s -> futures.get(s.stepId()).join())
                .toList();

        long successCount = results.stream().filter(r -> r.status() == StepStatus.SUCCESS).count();
        long failureCount = results.stream().filter(r -> r.status() == StepStatus.FAILED).count();
        log.debug("Execution completed: {} succeeded, {} failed", successCount, failureCount);

        return results;
    }

    private StepResult executeAfterDependencies(final ExecutionStep step,
                                                final Map<String, CompletableFuture<StepResult>> futures) {
        for (var depId : step.dependsOn()) {
            var depResult = futures.get(depId).join();
            if (depResult.status() == StepStatus.FAILED) {
                log.warn("Skipping step '{}' because dependency '{}' failed", step.stepId(), depId);
                return new StepResult(step.stepId(), step.tool(), StepStatus.FAILED,
                        null, "Skipped because dependency '" + depId + "' failed.",
                        STEP_NOT_EXECUTED_LATENCY);
            }
        }
        return executeSingleStep(step);
    }

    private StepResult executeSingleStep(final ExecutionStep step) {
        log.debug("Executing step '{}' with tool '{}'", step.stepId(), step.tool());

        if (!toolRegistry.hasTool(step.tool())) {
            return new StepResult(step.stepId(), step.tool(), StepStatus.FAILED,
                    null, "Unknown tool: " + step.tool(), STEP_NOT_EXECUTED_LATENCY);
        }

        var executor = toolExecutors.get(step.tool());
        if (executor == null) {
            return new StepResult(step.stepId(), step.tool(), StepStatus.FAILED,
                    null, "No executor registered for tool: " + step.tool(),
                    STEP_NOT_EXECUTED_LATENCY);
        }

        var start = System.currentTimeMillis();
        try {
            var data = executor.execute(step.arguments());
            var latency = System.currentTimeMillis() - start;
            log.debug("Step '{}' completed successfully in {}ms", step.stepId(), latency);
            return new StepResult(step.stepId(), step.tool(), StepStatus.SUCCESS, data, null, latency);
        } catch (Exception e) {
            var latency = System.currentTimeMillis() - start;
            log.warn("Step '{}' failed: {}", step.stepId(), e.getMessage());
            return new StepResult(step.stepId(), step.tool(), StepStatus.FAILED, null, e.getMessage(), latency);
        }
    }

    /**
     * Kahn's algorithm. Returns steps in a topological order such that
     * every step appears after all of its dependencies. The plan is
     * already validated as acyclic so no cycle check is needed here.
     */
    static List<ExecutionStep> topologicalSort(final List<ExecutionStep> steps) {
        var inDegree = new HashMap<String, Integer>();
        var stepMap   = new HashMap<String, ExecutionStep>();
        var dependents = new HashMap<String, List<String>>();

        for (var step : steps) {
            inDegree.put(step.stepId(), 0);
            stepMap.put(step.stepId(), step);
        }

        for (var step : steps) {
            for (var dep : step.dependsOn()) {
                dependents.computeIfAbsent(dep, k -> new ArrayList<>()).add(step.stepId());
            }
            inDegree.merge(step.stepId(), step.dependsOn().size(), Integer::sum);
        }

        var queue = new ArrayDeque<String>();
        for (var entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        var sorted = new ArrayList<ExecutionStep>();
        while (!queue.isEmpty()) {
            var node = queue.poll();
            sorted.add(stepMap.get(node));
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

        return Collections.unmodifiableList(sorted);
    }
}
