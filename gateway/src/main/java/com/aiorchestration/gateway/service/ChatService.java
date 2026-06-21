package com.aiorchestration.gateway.service;

import com.aiorchestration.gateway.model.ChatResponse;
import com.aiorchestration.gateway.planner.ExecutionPlanValidator;
import com.aiorchestration.gateway.planner.IntentPlannerService;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class ChatService {

    @Value("${planning.clarification-threshold}")
    private double clarificationThreshold;

    private final IntentPlannerService plannerService;
    private final ExecutionPlanValidator planValidator;
    private final ExecutionEngineService executionEngine;
    private final ResponseAggregatorService aggregator;

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

        var result = plannerService.plan(conversationId, message);

        if (result.confidence() < clarificationThreshold) {
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
