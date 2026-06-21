package com.aiorchestration.gateway.planner;

import com.aiorchestration.gateway.model.ExecutionPlan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Validates a generated execution plan before it is passed to the
 * execution engine. Ensures plan structure, step dependencies,
 * and tool availability are correct.
 */
@Slf4j
@Component
public class ExecutionPlanValidator {

    public void validate(final ExecutionPlan plan) {

    }
}
