package com.aiorchestration.gateway.service;

import com.aiorchestration.gateway.model.*;
import com.aiorchestration.gateway.planner.DeterministicExecutionPlanFactory;
import com.aiorchestration.gateway.planner.ExecutionPlanValidator;
import com.aiorchestration.gateway.planner.IntentPlannerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    private static final String CONVERSATION_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String MESSAGE = "book a flight and check weather";

    @Mock
    private IntentPlannerService plannerService;

    @Mock
    private DeterministicExecutionPlanFactory fallbackPlanner;

    @Mock
    private ExecutionPlanValidator planValidator;

    @Mock
    private ExecutionEngineService executionEngine;

    @Mock
    private ResponseAggregatorService aggregator;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(plannerService, planValidator, executionEngine, aggregator, fallbackPlanner);
        ReflectionTestUtils.setField(chatService, "clarificationThreshold", 0.5);
        ReflectionTestUtils.setField(chatService, "plannerEnabled", true);
    }

    @Test
    @DisplayName("should execute happy path: plan, validate, execute, aggregate")
    void shouldExecuteHappyPath() {
        var steps = List.of(new ExecutionStep("step-1", "flight.search", null, null));
        var plan = new ExecutionPlan(steps);
        var planResult = new PlanGenerationResult(0.95, "Plan summary", plan);
        var stepResults = List.of(
                new StepResult("step-1", "flight.search", StepStatus.SUCCESS, List.of(), null, 100)
        );
        var expectedResponse = new ChatResponse(false, List.of("step-1"), List.of(),
                stepResults, null, "Plan summary", false, null);

        when(plannerService.plan(CONVERSATION_ID, MESSAGE)).thenReturn(planResult);
        when(executionEngine.execute(plan)).thenReturn(stepResults);
        when(aggregator.aggregate(planResult, stepResults)).thenReturn(expectedResponse);

        var response = chatService.chat(CONVERSATION_ID, MESSAGE);

        assertNotNull(response);
        assertEquals(expectedResponse, response);
        verify(planValidator).validate(planResult);
        verify(executionEngine).execute(plan);
        verify(aggregator).aggregate(planResult, stepResults);
    }

    @Test
    @DisplayName("should return clarification response when confidence is low")
    void shouldReturnClarificationOnLowConfidence() {
        var steps = List.of(new ExecutionStep("step-1", "flight.search", null, null));
        var plan = new ExecutionPlan(steps);
        var planResult = new PlanGenerationResult(0.3, "I need more details", plan);

        when(plannerService.plan(CONVERSATION_ID, MESSAGE)).thenReturn(planResult);

        var response = chatService.chat(CONVERSATION_ID, MESSAGE);

        assertTrue(response.clarificationRequired());
        assertEquals("I need more details", response.clarificationMessage());
        assertEquals("I need more details", response.summary());
        assertTrue(response.completedSteps().isEmpty());
        assertTrue(response.failedSteps().isEmpty());
        assertTrue(response.executionTrace().isEmpty());
        assertFalse(response.partialSuccess());
    }

    @Test
    @DisplayName("should execute normally when confidence equals threshold")
    void shouldExecuteWhenConfidenceEqualsThreshold() {
        var steps = List.of(new ExecutionStep("step-1", "flight.search", null, null));
        var plan = new ExecutionPlan(steps);
        var planResult = new PlanGenerationResult(0.5, "Plan summary", plan);
        var stepResults = List.of(
                new StepResult("step-1", "flight.search", StepStatus.SUCCESS, List.of(), null, 100)
        );
        var expectedResponse = new ChatResponse(false, List.of("step-1"), List.of(),
                stepResults, null, "Plan summary", false, null);

        when(plannerService.plan(CONVERSATION_ID, MESSAGE)).thenReturn(planResult);
        when(executionEngine.execute(plan)).thenReturn(stepResults);
        when(aggregator.aggregate(planResult, stepResults)).thenReturn(expectedResponse);

        var response = chatService.chat(CONVERSATION_ID, MESSAGE);

        assertFalse(response.clarificationRequired());
        verify(planValidator).validate(planResult);
    }

    @Test
    @DisplayName("should propagate PlanGenerationException from planner")
    void shouldPropagatePlannerException() {
        when(plannerService.plan(CONVERSATION_ID, MESSAGE)).thenThrow(new RuntimeException("AI failure"));

        try {
            chatService.chat(CONVERSATION_ID, MESSAGE);
        } catch (RuntimeException e) {
            assertEquals("AI failure", e.getMessage());
        }
    }

    @Nested
    @DisplayName("when AI planner enabled")
    class WhenPlannerEnabled {

        @Test
        @DisplayName("should invoke IntentPlannerService")
        void shouldInvokeIntentPlanner() {
            var steps = List.of(new ExecutionStep("step-1", "flight.search", null, null));
            var plan = new ExecutionPlan(steps);
            var planResult = new PlanGenerationResult(0.95, "Plan summary", plan);
            var stepResults = List.of(
                    new StepResult("step-1", "flight.search", StepStatus.SUCCESS, List.of(), null, 100)
            );
            var expectedResponse = new ChatResponse(false, List.of("step-1"), List.of(),
                    stepResults, null, "Plan summary", false, null);

            when(plannerService.plan(CONVERSATION_ID, MESSAGE)).thenReturn(planResult);
            when(executionEngine.execute(plan)).thenReturn(stepResults);
            when(aggregator.aggregate(planResult, stepResults)).thenReturn(expectedResponse);

            chatService.chat(CONVERSATION_ID, MESSAGE);

            verify(plannerService).plan(CONVERSATION_ID, MESSAGE);
            verify(fallbackPlanner, never()).createPlan(any());
            verify(planValidator).validate(planResult);
            verify(executionEngine).execute(plan);
            verify(aggregator).aggregate(planResult, stepResults);
        }
    }

    @Nested
    @DisplayName("when AI planner disabled")
    class WhenPlannerDisabled {

        @BeforeEach
        void setUp() {
            ReflectionTestUtils.setField(chatService, "plannerEnabled", false);
        }

        @Test
        @DisplayName("should invoke DeterministicExecutionPlanFactory")
        void shouldInvokeFallbackPlanner() {
            var flightStep = new ExecutionStep("step-1", "flight.search",
                    Map.of("origin", "BLR", "destination", "NRT", "departureDate", "2026-06-22"), List.of());
            var weatherStep = new ExecutionStep("step-2", "weather.forecast",
                    Map.of("location", "Tokyo", "date", "2026-06-22"), List.of("step-1"));
            var plan = new ExecutionPlan(List.of(flightStep, weatherStep));
            var planResult = new PlanGenerationResult(1.0, "Deterministic plan", plan);
            var stepResults = List.of(
                    new StepResult("step-1", "flight.search", StepStatus.SUCCESS, List.of(), null, 100),
                    new StepResult("step-2", "weather.forecast", StepStatus.SUCCESS, List.of(), null, 200)
            );
            var expectedResponse = new ChatResponse(false, List.of("step-1", "step-2"), List.of(),
                    stepResults, null, "Deterministic plan", false, null);

            when(fallbackPlanner.createPlan(MESSAGE)).thenReturn(planResult);
            when(executionEngine.execute(plan)).thenReturn(stepResults);
            when(aggregator.aggregate(planResult, stepResults)).thenReturn(expectedResponse);

            var response = chatService.chat(CONVERSATION_ID, MESSAGE);

            assertNotNull(response);
            assertEquals(expectedResponse, response);
            verify(plannerService, never()).plan(anyString(), anyString());
            verify(fallbackPlanner).createPlan(MESSAGE);
            verify(planValidator).validate(planResult);
            verify(executionEngine).execute(plan);
            verify(aggregator).aggregate(planResult, stepResults);
        }

        @Test
        @DisplayName("should not trigger clarification path (confidence is always 1.0)")
        void shouldNotTriggerClarification() {
            var flightStep = new ExecutionStep("step-1", "flight.search",
                    Map.of("origin", "BLR", "destination", "NRT", "departureDate", "2026-06-22"), List.of());
            var plan = new ExecutionPlan(List.of(flightStep));
            var planResult = new PlanGenerationResult(1.0, "Deterministic plan", plan);
            var stepResults = List.of(
                    new StepResult("step-1", "flight.search", StepStatus.SUCCESS, List.of(), null, 100)
            );

            when(fallbackPlanner.createPlan(MESSAGE)).thenReturn(planResult);
            when(executionEngine.execute(plan)).thenReturn(stepResults);
            when(aggregator.aggregate(planResult, stepResults)).thenReturn(
                    new ChatResponse(false, List.of("step-1"), List.of(), stepResults, null,
                            "Deterministic plan", false, null));

            var response = chatService.chat(CONVERSATION_ID, MESSAGE);

            assertFalse(response.clarificationRequired());
            verify(plannerService, never()).plan(anyString(), anyString());
            verify(fallbackPlanner).createPlan(MESSAGE);
            verify(planValidator).validate(planResult);
            verify(executionEngine).execute(plan);
            verify(aggregator).aggregate(planResult, stepResults);
        }

        @Test
        @DisplayName("should pass validation and reach execution engine")
        void shouldReachExecutionEngine() {
            var flightStep = new ExecutionStep("step-1", "flight.search",
                    Map.of("origin", "BLR", "destination", "NRT", "departureDate", "2026-06-22"), List.of());
            var plan = new ExecutionPlan(List.of(flightStep));
            var planResult = new PlanGenerationResult(1.0, "Deterministic plan", plan);
            var stepResults = List.of(
                    new StepResult("step-1", "flight.search", StepStatus.SUCCESS, List.of(), null, 100)
            );

            when(fallbackPlanner.createPlan(MESSAGE)).thenReturn(planResult);
            when(executionEngine.execute(plan)).thenReturn(stepResults);
            when(aggregator.aggregate(planResult, stepResults)).thenReturn(
                    new ChatResponse(false, List.of("step-1"), List.of(), stepResults, null,
                            "Deterministic plan", false, null));

            chatService.chat(CONVERSATION_ID, MESSAGE);

            verify(planValidator).validate(planResult);
            verify(executionEngine).execute(plan);
            verify(aggregator).aggregate(planResult, stepResults);
        }
    }
}
