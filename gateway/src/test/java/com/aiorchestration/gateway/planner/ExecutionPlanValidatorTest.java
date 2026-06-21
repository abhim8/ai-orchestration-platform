package com.aiorchestration.gateway.planner;

import com.aiorchestration.gateway.exception.PlanValidationException;
import com.aiorchestration.gateway.model.ExecutionPlan;
import com.aiorchestration.gateway.model.ExecutionStep;
import com.aiorchestration.gateway.model.PlanGenerationResult;
import com.aiorchestration.gateway.registry.ToolRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExecutionPlanValidatorTest {

    private ToolRegistry toolRegistry;
    private ExecutionPlanValidator validator;

    @BeforeEach
    void setUp() {
        toolRegistry = new ToolRegistry();
        validator = new ExecutionPlanValidator(toolRegistry);
    }

    private static PlanGenerationResult validResult(final ExecutionPlan plan) {
        return new PlanGenerationResult(0.95, "Test plan", plan);
    }

    private static ExecutionStep step(final String stepId, final String tool,
                                      final Map<String, Object> arguments,
                                      final List<String> dependsOn) {
        return new ExecutionStep(stepId, tool, arguments, dependsOn);
    }

    // ---- Valid plan ----

    @Test
    @DisplayName("should accept a valid plan")
    void shouldAcceptValidPlan() {
        var steps = List.of(
            step("1", "flight.search", Map.of("origin", "LHR", "destination", "CDG", "departureDate", "2026-06-22"), List.of()),
            step("2", "weather.forecast", Map.of("location", "Paris", "date", "2026-06-22"), List.of())
        );
        var plan = validResult(new ExecutionPlan(steps));

        assertDoesNotThrow(() -> validator.validate(plan));
    }

    // ---- Low confidence ----

    @Test
    @DisplayName("should reject plan with confidence below threshold")
    void shouldRejectLowConfidence() {
        var steps = List.of(
            step("1", "flight.search", Map.of("origin", "LHR", "destination", "CDG", "departureDate", "2026-06-22"), List.of())
        );
        var result = new PlanGenerationResult(0.34, "Low confidence plan", new ExecutionPlan(steps));

        var ex = assertThrows(PlanValidationException.class, () -> validator.validate(result));
        assertEquals("Planner confidence 0.34 is below minimum threshold 0.5", ex.getMessage());
    }

    @Test
    @DisplayName("should accept plan with confidence exactly at threshold")
    void shouldAcceptConfidenceAtThreshold() {
        var steps = List.of(
            step("1", "flight.search", Map.of("origin", "LHR", "destination", "CDG", "departureDate", "2026-06-22"), List.of())
        );
        var result = new PlanGenerationResult(0.5, "Borderline plan", new ExecutionPlan(steps));

        assertDoesNotThrow(() -> validator.validate(result));
    }

    // ---- Null / empty checks ----

    @Test
    @DisplayName("should reject null PlanGenerationResult")
    void shouldRejectNullResult() {
        var ex = assertThrows(PlanValidationException.class, () -> validator.validate(null));
        assertEquals("PlanGenerationResult must not be null", ex.getMessage());
    }

    @Test
    @DisplayName("should reject null ExecutionPlan")
    void shouldRejectNullExecutionPlan() {
        var result = new PlanGenerationResult(0.95, "No plan", null);

        var ex = assertThrows(PlanValidationException.class, () -> validator.validate(result));
        assertEquals("ExecutionPlan must not be null", ex.getMessage());
    }

    @Test
    @DisplayName("should reject empty step list")
    void shouldRejectEmptySteps() {
        var plan = new ExecutionPlan(List.of());
        var result = validResult(plan);

        var ex = assertThrows(PlanValidationException.class, () -> validator.validate(result));
        assertEquals("Step list must not be empty", ex.getMessage());
    }

    @Test
    @DisplayName("should reject null step list")
    void shouldRejectNullSteps() {
        var plan = new ExecutionPlan(null);
        var result = validResult(plan);

        // ExecutionPlan normalizes null to empty at construction, resulting in
        // the "empty" message. The "null" check exists defensively in case the
        // normalization path is bypassed (e.g. direct field deserialization).
        var ex = assertThrows(PlanValidationException.class, () -> validator.validate(result));
        assertEquals("Step list must not be empty", ex.getMessage());
    }

    // ---- Duplicate step IDs ----

    @Test
    @DisplayName("should reject duplicate step IDs")
    void shouldRejectDuplicateStepIds() {
        var steps = List.of(
            step("1", "flight.search", Map.of("origin", "LHR", "destination", "CDG", "departureDate", "2026-06-22"), List.of()),
            step("1", "weather.forecast", Map.of("location", "Paris", "date", "2026-06-22"), List.of())
        );
        var result = validResult(new ExecutionPlan(steps));

        var ex = assertThrows(PlanValidationException.class, () -> validator.validate(result));
        assertEquals("Duplicate stepId detected: 1", ex.getMessage());
    }

    // ---- Unknown tool ----

    @Test
    @DisplayName("should reject unknown tool")
    void shouldRejectUnknownTool() {
        var steps = List.of(
            step("1", "hotel.search", Map.of("location", "Paris"), List.of())
        );
        var result = validResult(new ExecutionPlan(steps));

        var ex = assertThrows(PlanValidationException.class, () -> validator.validate(result));
        assertEquals("Unknown tool: hotel.search", ex.getMessage());
    }

    // ---- Missing required arguments ----

    @Test
    @DisplayName("should reject missing required argument")
    void shouldRejectMissingRequiredArgument() {
        var steps = List.of(
            step("1", "flight.search", Map.of("origin", "LHR"), List.of())
        );
        var result = validResult(new ExecutionPlan(steps));

        var ex = assertThrows(PlanValidationException.class, () -> validator.validate(result));
        assertEquals("Missing required argument 'destination' for tool 'flight.search'", ex.getMessage());
    }

    @Test
    @DisplayName("should reject missing all required arguments")
    void shouldRejectMissingAllRequiredArguments() {
        var steps = List.of(
            step("1", "flight.search", Map.of(), List.of())
        );
        var result = validResult(new ExecutionPlan(steps));

        var ex = assertThrows(PlanValidationException.class, () -> validator.validate(result));
        // Should fail on the first missing argument: origin
        assertEquals("Missing required argument 'origin' for tool 'flight.search'", ex.getMessage());
    }

    @Test
    @DisplayName("should reject null arguments map when arguments are required")
    void shouldRejectNullArguments() {
        var steps = List.of(
            step("1", "flight.search", null, List.of())
        );
        var result = validResult(new ExecutionPlan(steps));

        var ex = assertThrows(PlanValidationException.class, () -> validator.validate(result));
        assertEquals("Missing required argument 'origin' for tool 'flight.search'", ex.getMessage());
    }

    @Test
    @DisplayName("should accept extra arguments beyond required")
    void shouldAcceptExtraArguments() {
        var steps = List.of(
            step("1", "flight.search", Map.of("origin", "LHR", "destination", "CDG", "departureDate", "2026-06-22", "preference", "window"), List.of())
        );
        var result = validResult(new ExecutionPlan(steps));

        assertDoesNotThrow(() -> validator.validate(result));
    }

    // ---- Invalid dependsOn references ----

    @Test
    @DisplayName("should reject dependsOn referencing non-existent step")
    void shouldRejectInvalidDependsOnReference() {
        var steps = List.of(
            step("1", "flight.search", Map.of("origin", "LHR", "destination", "CDG", "departureDate", "2026-06-22"), List.of("nonexistent"))
        );
        var result = validResult(new ExecutionPlan(steps));

        var ex = assertThrows(PlanValidationException.class, () -> validator.validate(result));
        assertEquals("Execution step '1' depends on unknown step 'nonexistent'", ex.getMessage());
    }

    // ---- Cycle detection ----

    @Test
    @DisplayName("should reject simple cycle of two nodes")
    void shouldRejectSimpleCycle() {
        var steps = List.of(
            step("1", "flight.search", Map.of("origin", "LHR", "destination", "CDG", "departureDate", "2026-06-22"), List.of("2")),
            step("2", "weather.forecast", Map.of("location", "Paris", "date", "2026-06-22"), List.of("1"))
        );
        var result = validResult(new ExecutionPlan(steps));

        var ex = assertThrows(PlanValidationException.class, () -> validator.validate(result));
        assertEquals("Cycle detected in execution plan", ex.getMessage());
    }

    @Test
    @DisplayName("should reject larger multi-node cycle")
    void shouldRejectMultiNodeCycle() {
        // 1 -> 2 -> 3 -> 1
        var steps = List.of(
            step("1", "flight.search", Map.of("origin", "LHR", "destination", "CDG", "departureDate", "2026-06-22"), List.of("3")),
            step("2", "weather.forecast", Map.of("location", "Paris", "date", "2026-06-22"), List.of("1")),
            step("3", "flight.search", Map.of("origin", "CDG", "destination", "LHR", "departureDate", "2026-06-24"), List.of("2"))
        );
        var result = validResult(new ExecutionPlan(steps));

        var ex = assertThrows(PlanValidationException.class, () -> validator.validate(result));
        assertEquals("Cycle detected in execution plan", ex.getMessage());
    }

    @Test
    @DisplayName("should reject self-loop")
    void shouldRejectSelfLoop() {
        var steps = List.of(
            step("1", "flight.search", Map.of("origin", "LHR", "destination", "CDG", "departureDate", "2026-06-22"), List.of("1"))
        );
        var result = validResult(new ExecutionPlan(steps));

        var ex = assertThrows(PlanValidationException.class, () -> validator.validate(result));
        assertEquals("Cycle detected in execution plan", ex.getMessage());
    }

    @Test
    @DisplayName("should reject diamond with conflicting dependency cycle")
    void shouldRejectDiamondCycle() {
        //   1
        //  / \
        // 2   3
        //  \ /
        //   4 -> 1 (cycle)
        var steps = List.of(
            step("1", "flight.search", Map.of("origin", "LHR", "destination", "CDG", "departureDate", "2026-06-22"), List.of("4")),
            step("2", "weather.forecast", Map.of("location", "Paris", "date", "2026-06-22"), List.of("1")),
            step("3", "flight.search", Map.of("origin", "CDG", "destination", "LHR", "departureDate", "2026-06-24"), List.of("1")),
            step("4", "weather.forecast", Map.of("location", "London", "date", "2026-06-22"), List.of("2", "3"))
        );
        var result = validResult(new ExecutionPlan(steps));

        var ex = assertThrows(PlanValidationException.class, () -> validator.validate(result));
        assertEquals("Cycle detected in execution plan", ex.getMessage());
    }

    // ---- Valid DAGs ----

    @Test
    @DisplayName("should accept valid DAG with branching")
    void shouldAcceptValidDagWithBranching() {
        //   1
        //  / \
        // 2   3
        //  \ /
        //   4
        var steps = List.of(
            step("1", "flight.search", Map.of("origin", "LHR", "destination", "CDG", "departureDate", "2026-06-22"), List.of()),
            step("2", "weather.forecast", Map.of("location", "Paris", "date", "2026-06-22"), List.of("1")),
            step("3", "flight.search", Map.of("origin", "CDG", "destination", "LHR", "departureDate", "2026-06-24"), List.of("1")),
            step("4", "weather.forecast", Map.of("location", "London", "date", "2026-06-24"), List.of("2", "3"))
        );
        var result = validResult(new ExecutionPlan(steps));

        assertDoesNotThrow(() -> validator.validate(result));
    }

    @Test
    @DisplayName("should accept valid DAG with parallel roots")
    void shouldAcceptValidDagWithParallelRoots() {
        // 1   2
        //  \ /
        //   3
        var steps = List.of(
            step("1", "flight.search", Map.of("origin", "LHR", "destination", "CDG", "departureDate", "2026-06-22"), List.of()),
            step("2", "weather.forecast", Map.of("location", "Paris", "date", "2026-06-22"), List.of()),
            step("3", "flight.search", Map.of("origin", "CDG", "destination", "LHR", "departureDate", "2026-06-24"), List.of("1", "2"))
        );
        var result = validResult(new ExecutionPlan(steps));

        assertDoesNotThrow(() -> validator.validate(result));
    }

    @Test
    @DisplayName("should accept linear chain without cycle")
    void shouldAcceptLinearChain() {
        // 1 -> 2 -> 3
        var steps = List.of(
            step("1", "flight.search", Map.of("origin", "LHR", "destination", "CDG", "departureDate", "2026-06-22"), List.of()),
            step("2", "weather.forecast", Map.of("location", "Paris", "date", "2026-06-22"), List.of("1")),
            step("3", "flight.search", Map.of("origin", "CDG", "destination", "LHR", "departureDate", "2026-06-24"), List.of("2"))
        );
        var result = validResult(new ExecutionPlan(steps));

        assertDoesNotThrow(() -> validator.validate(result));
    }

    @Test
    @DisplayName("should accept single step plan")
    void shouldAcceptSingleStepPlan() {
        var steps = List.of(
            step("1", "flight.search", Map.of("origin", "LHR", "destination", "CDG", "departureDate", "2026-06-22"), List.of())
        );
        var result = validResult(new ExecutionPlan(steps));

        assertDoesNotThrow(() -> validator.validate(result));
    }

    // ---- Validation ordering: tool check before argument check ----

    @Test
    @DisplayName("should report unknown tool before missing arguments")
    void shouldReportUnknownToolBeforeMissingArgs() {
        var steps = List.of(
            step("1", "unknown.tool", Map.of(), List.of())
        );
        var result = validResult(new ExecutionPlan(steps));

        var ex = assertThrows(PlanValidationException.class, () -> validator.validate(result));
        assertEquals("Unknown tool: unknown.tool", ex.getMessage());
    }

    // ---- Custom ToolRegistry ----

    @Test
    @DisplayName("should work with custom tool registry")
    void shouldWorkWithCustomToolRegistry() {
        var customTools = Map.of("custom.tool", Set.of("arg1"));
        var customRegistry = new ToolRegistry(customTools);
        var customValidator = new ExecutionPlanValidator(customRegistry);

        var steps = List.of(
            step("1", "custom.tool", Map.of("arg1", "value1"), List.of())
        );
        var result = validResult(new ExecutionPlan(steps));

        assertDoesNotThrow(() -> customValidator.validate(result));
    }

    @Test
    @DisplayName("should reject missing argument from custom tool registry")
    void shouldRejectMissingArgInCustomRegistry() {
        var customTools = Map.of("custom.tool", Set.of("requiredArg"));
        var customRegistry = new ToolRegistry(customTools);
        var customValidator = new ExecutionPlanValidator(customRegistry);

        var steps = List.of(
            step("1", "custom.tool", Map.of(), List.of())
        );
        var result = validResult(new ExecutionPlan(steps));

        var ex = assertThrows(PlanValidationException.class, () -> customValidator.validate(result));
        assertEquals("Missing required argument 'requiredArg' for tool 'custom.tool'", ex.getMessage());
    }

    // ---- Edge cases with null dependsOn ----

    @Test
    @DisplayName("should accept step with null dependsOn")
    void shouldAcceptNullDependsOn() {
        var steps = List.of(
            step("1", "flight.search", Map.of("origin", "LHR", "destination", "CDG", "departureDate", "2026-06-22"), null)
        );
        var result = validResult(new ExecutionPlan(steps));

        assertDoesNotThrow(() -> validator.validate(result));
    }
}
