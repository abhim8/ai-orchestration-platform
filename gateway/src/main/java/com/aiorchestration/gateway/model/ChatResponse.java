package com.aiorchestration.gateway.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Response produced by the chat flow, containing execution results
 * and any clarification messages from the AI planner.
 *
 * @param partialSuccess       indicates whether any steps failed while others succeeded
 * @param completedSteps       ordered list of successfully completed step identifiers
 * @param failedSteps          list of step identifiers that failed during execution
 * @param executionTrace       detailed result for each executed step
 * @param response             structured response data keyed by tool or step
 * @param summary              human-readable summary of what was accomplished
 * @param clarificationRequired whether the AI needs more information before it can plan
 * @param clarificationMessage message describing what clarification is needed
 */
public record ChatResponse(
    boolean partialSuccess,
    List<String> completedSteps,
    List<String> failedSteps,
    List<StepResult> executionTrace,
    Map<String, Object> response,
    String summary,
    boolean clarificationRequired,
    String clarificationMessage
) {

    public ChatResponse {
        completedSteps = completedSteps != null
            ? Collections.unmodifiableList(completedSteps)
            : List.of();
        failedSteps = failedSteps != null
            ? Collections.unmodifiableList(failedSteps)
            : List.of();
        executionTrace = executionTrace != null
            ? Collections.unmodifiableList(executionTrace)
            : List.of();
        response = response != null
            ? Collections.unmodifiableMap(response)
            : Map.of();
    }
}
