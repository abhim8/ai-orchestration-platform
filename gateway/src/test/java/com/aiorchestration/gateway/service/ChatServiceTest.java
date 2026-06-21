package com.aiorchestration.gateway.service;

import com.aiorchestration.gateway.model.*;
import com.aiorchestration.gateway.planner.ExecutionPlanValidator;
import com.aiorchestration.gateway.planner.IntentPlannerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    private static final String CONVERSATION_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String MESSAGE = "book a flight and check weather";

    @Mock
    private IntentPlannerService plannerService;

    @Mock
    private ExecutionPlanValidator planValidator;

    @Mock
    private ExecutionEngineService executionEngine;

    @Mock
    private ResponseAggregatorService aggregator;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(plannerService, planValidator, executionEngine, aggregator);
        ReflectionTestUtils.setField(chatService, "clarificationThreshold", 0.5);
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
}
