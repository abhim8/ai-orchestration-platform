package com.aiorchestration.gateway.planner;

import com.aiorchestration.gateway.model.ExecutionPlan;
import com.aiorchestration.gateway.model.ExecutionStep;
import com.aiorchestration.gateway.model.PlanGenerationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class DeterministicExecutionPlanFactory {

    private static final double FIXED_CONFIDENCE = 1.0;

    public PlanGenerationResult createPlan(final String message) {
        log.debug("Generating deterministic execution plan for message: {}", message);

        var lower = message.toLowerCase();
        var steps = new ArrayList<ExecutionStep>();
        var descriptions = new ArrayList<String>();

        if (lower.contains("flight")) {
            steps.add(new ExecutionStep(
                    "step-1",
                    "flight.search",
                    Map.of(
                            "origin", "BLR",
                            "destination", "NRT",
                            "departureDate", LocalDate.now().plusDays(1).toString()
                    ),
                    List.of()
            ));
            descriptions.add("flight search");
        }

        if (lower.contains("weather")) {
            var dependsOn = steps.isEmpty() ? List.<String>of() : List.of(steps.getLast().stepId());
            steps.add(new ExecutionStep(
                    "step-" + (steps.size() + 1),
                    "weather.forecast",
                    Map.of(
                            "location", "Tokyo",
                            "date", LocalDate.now().plusDays(1).toString()
                    ),
                    dependsOn
            ));
            descriptions.add("weather forecast");
        }

        if (steps.isEmpty()) {
            log.warn("No known keywords found in message, returning empty plan");
        }

        var summary = descriptions.isEmpty()
                ? "No actions detected"
                : "Deterministic plan: " + String.join(" and ", descriptions);

        log.info("Skipping Gemini planning because AI planner is disabled. Generating deterministic execution plan.");
        return new PlanGenerationResult(FIXED_CONFIDENCE, summary, new ExecutionPlan(steps));
    }
}
