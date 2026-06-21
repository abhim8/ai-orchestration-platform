package com.aiorchestration.gateway.service;

import com.aiorchestration.gateway.model.ChatResponse;
import com.aiorchestration.gateway.model.PlanGenerationResult;
import com.aiorchestration.gateway.model.StepResult;
import com.aiorchestration.gateway.model.StepStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregates execution results into a coherent response.
 */
@Slf4j
@Service
public class ResponseAggregatorService {

    /**
     * Builds a {@link ChatResponse} from the plan and its execution results.
     *
     * @param result      the plan generation result containing the AI summary
     * @param stepResults the results of executing each step
     * @return a complete chat response
     */
    public ChatResponse aggregate(final PlanGenerationResult result,
                                  final List<StepResult> stepResults) {
        var completed = new ArrayList<String>();
        var failed = new ArrayList<String>();
        var response = new HashMap<String, Object>();

        for (var stepResult : stepResults) {
            if (stepResult.status() == StepStatus.SUCCESS) {
                completed.add(stepResult.stepId());
                if (stepResult.data() != null) {
                    response.put(stepResult.stepId(), stepResult.data());
                }
            } else {
                failed.add(stepResult.stepId());
            }
        }

        var partialSuccess = !completed.isEmpty() && !failed.isEmpty();

        log.debug("Aggregated {} steps: {} completed, {} failed",
                stepResults.size(), completed.size(), failed.size());

        return new ChatResponse(
                partialSuccess,
                completed,
                failed,
                stepResults,
                response,
                result.summary(),
                false,
                null
        );
    }
}
