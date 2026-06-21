package com.aiorchestration.gateway.planner;

import com.aiorchestration.gateway.model.PlanGenerationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Communicates with Gemini via Spring AI to determine user intent
 * and generate a structured execution plan ({@link PlanGenerationResult}).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntentPlannerService {

    public PlanGenerationResult plan(final String userMessage) {
        return null;
    }
}
