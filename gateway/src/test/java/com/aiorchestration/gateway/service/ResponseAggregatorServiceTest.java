package com.aiorchestration.gateway.service;

import com.aiorchestration.gateway.model.ChatResponse;
import com.aiorchestration.gateway.model.ExecutionPlan;
import com.aiorchestration.gateway.model.ExecutionStep;
import com.aiorchestration.gateway.model.PlanGenerationResult;
import com.aiorchestration.gateway.model.StepResult;
import com.aiorchestration.gateway.model.StepStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResponseAggregatorServiceTest {

    private static final String STEP_1 = "step-1";
    private static final String STEP_2 = "step-2";
    private static final String SUMMARY = "Flight and weather plan";

    private ResponseAggregatorService service;

    @BeforeEach
    void setUp() {
        service = new ResponseAggregatorService();
    }

    private PlanGenerationResult createPlan() {
        var steps = List.of(
                new ExecutionStep(STEP_1, "flight.search", Map.of("origin", "LHR"), null),
                new ExecutionStep(STEP_2, "weather.forecast", Map.of("location", "Paris"), null)
        );
        return new PlanGenerationResult(0.95, SUMMARY, new ExecutionPlan(steps));
    }

    @Test
    @DisplayName("should return ChatResponse with all steps completed")
    void shouldAggregateAllSuccessful() {
        var plan = createPlan();
        var stepResults = List.of(
                new StepResult(STEP_1, "flight.search", StepStatus.SUCCESS,
                        Map.of("flights", List.of()), null, 100),
                new StepResult(STEP_2, "weather.forecast", StepStatus.SUCCESS,
                        Map.of("temp", 22), null, 50)
        );

        var response = service.aggregate(plan, stepResults);

        assertFalse(response.clarificationRequired());
        assertFalse(response.partialSuccess());
        assertEquals(List.of(STEP_1, STEP_2), response.completedSteps());
        assertTrue(response.failedSteps().isEmpty());
        assertEquals(SUMMARY, response.summary());
        assertEquals(2, response.executionTrace().size());
        assertTrue(response.response().containsKey(STEP_1));
        assertTrue(response.response().containsKey(STEP_2));
    }

    @Test
    @DisplayName("should return ChatResponse with all steps failed")
    void shouldAggregateAllFailed() {
        var plan = createPlan();
        var stepResults = List.of(
                new StepResult(STEP_1, "flight.search", StepStatus.FAILED,
                        null, "API error", 100),
                new StepResult(STEP_2, "weather.forecast", StepStatus.FAILED,
                        null, "Timeout", 50)
        );

        var response = service.aggregate(plan, stepResults);

        assertFalse(response.clarificationRequired());
        assertFalse(response.partialSuccess());
        assertTrue(response.completedSteps().isEmpty());
        assertEquals(List.of(STEP_1, STEP_2), response.failedSteps());
        assertTrue(response.response().isEmpty());
    }

    @Test
    @DisplayName("should return ChatResponse with partial success")
    void shouldAggregatePartialSuccess() {
        var plan = createPlan();
        var stepResults = List.of(
                new StepResult(STEP_1, "flight.search", StepStatus.SUCCESS,
                        Map.of("flights", List.of()), null, 100),
                new StepResult(STEP_2, "weather.forecast", StepStatus.FAILED,
                        null, "Timeout", 50)
        );

        var response = service.aggregate(plan, stepResults);

        assertTrue(response.partialSuccess());
        assertEquals(List.of(STEP_1), response.completedSteps());
        assertEquals(List.of(STEP_2), response.failedSteps());
        assertTrue(response.response().containsKey(STEP_1));
        assertFalse(response.response().containsKey(STEP_2));
    }

    @Test
    @DisplayName("should handle empty step results")
    void shouldHandleEmptyStepResults() {
        var plan = createPlan();
        var stepResults = List.<StepResult>of();

        var response = service.aggregate(plan, stepResults);

        assertFalse(response.partialSuccess());
        assertTrue(response.completedSteps().isEmpty());
        assertTrue(response.failedSteps().isEmpty());
        assertTrue(response.response().isEmpty());
        assertEquals(SUMMARY, response.summary());
    }

    @Test
    @DisplayName("should skip null data in response map")
    void shouldSkipNullData() {
        var plan = createPlan();
        var stepResults = List.of(
                new StepResult(STEP_1, "flight.search", StepStatus.SUCCESS,
                        null, null, 100)
        );

        var response = service.aggregate(plan, stepResults);

        assertEquals(List.of(STEP_1), response.completedSteps());
        assertTrue(response.response().isEmpty());
    }

    @Test
    @DisplayName("should set clarificationRequired to false")
    void shouldSetClarificationRequiredToFalse() {
        var plan = createPlan();
        var stepResults = List.<StepResult>of();

        var response = service.aggregate(plan, stepResults);

        assertFalse(response.clarificationRequired());
    }

    @Test
    @DisplayName("should preserve execution trace order")
    void shouldPreserveExecutionTraceOrder() {
        var plan = createPlan();
        var stepResults = List.of(
                new StepResult(STEP_1, "flight.search", StepStatus.SUCCESS,
                        Map.of(), null, 100),
                new StepResult(STEP_2, "weather.forecast", StepStatus.SUCCESS,
                        Map.of(), null, 50)
        );

        var response = service.aggregate(plan, stepResults);

        assertEquals(STEP_1, response.executionTrace().get(0).stepId());
        assertEquals(STEP_2, response.executionTrace().get(1).stepId());
    }
}