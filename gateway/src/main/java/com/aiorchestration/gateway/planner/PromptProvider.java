package com.aiorchestration.gateway.planner;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Constructs structured prompts used by the {@link IntentPlannerService}
 * when communicating with Gemini via Spring AI's {@code PromptTemplate}.
 */
@Slf4j
@Component
public class PromptProvider {

    public String createPrompt(final String userMessage) {
        return "";
    }
}
