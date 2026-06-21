package com.aiorchestration.gateway.service;

import com.aiorchestration.gateway.model.ChatResponse;
import com.aiorchestration.gateway.planner.DeterministicExecutionPlanFactory;
import com.aiorchestration.gateway.planner.ExecutionPlanValidator;
import com.aiorchestration.gateway.planner.IntentPlannerService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Orchestrates the end-to-end chat flow:
 * intent planning, plan validation, execution, and response aggregation.
 */
@Slf4j
@Service
public class ChatService {

    @Value("${planning.clarification-threshold}")
    private double clarificationThreshold;

    @Value("${ai.planner.enabled}")
    private boolean plannerEnabled;

    private final IntentPlannerService plannerService;
    private final ExecutionPlanValidator planValidator;
    private final ExecutionEngineService executionEngine;
    private final ResponseAggregatorService aggregator;
    private final DeterministicExecutionPlanFactory fallbackPlanner;

    public ChatService(final IntentPlannerService plannerService,
                       final ExecutionPlanValidator planValidator,
                       final ExecutionEngineService executionEngine,
                       final ResponseAggregatorService aggregator,
                       final DeterministicExecutionPlanFactory fallbackPlanner) {
        this.plannerService = plannerService;
        this.planValidator = planValidator;
        this.executionEngine = executionEngine;
        this.aggregator = aggregator;
        this.fallbackPlanner = fallbackPlanner;
    }

    @PostConstruct
    void logPlannerMode() {
        if (plannerEnabled) {
            log.info("AI planner enabled: true");
        } else {
            log.info("AI planner enabled: false (deterministic fallback mode)");
        }
    }

    /**
     * Processes a user chat request through the full pipeline:
     * plan → (clarification if low confidence) → validate → execute → aggregate.
     *
     * @param conversationId the resolved conversation identifier (always a valid UUID)
     * @param message        the user's natural language message
     * @return the chat response with execution results or clarification
     */
    public ChatResponse chat(final String conversationId, final String message) {
        log.debug("Processing chat request for conversationId={}", conversationId);

        var result = plannerEnabled
                ? plannerService.plan(conversationId, message)
                : fallbackPlanner.createPlan(message);

        if (plannerEnabled && result.confidence() < clarificationThreshold) {
            log.debug("Clarification needed: confidence={}", result.confidence());
            return new ChatResponse(
                    false,
                    List.of(),
                    List.of(),
                    List.of(),
                    Map.of(),
                    result.summary(),
                    true,
                    result.summary()
            );
        }

        planValidator.validate(result);

        var stepResults = executionEngine.execute(result.executionPlan());

        return aggregator.aggregate(result, stepResults);
    }
}
