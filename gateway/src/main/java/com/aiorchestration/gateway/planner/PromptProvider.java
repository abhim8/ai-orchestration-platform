package com.aiorchestration.gateway.planner;

import com.aiorchestration.gateway.model.PlanGenerationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Single source of truth for all planner prompts.
 * Owns prompt construction using Spring AI's {@link PromptTemplate}.
 * Does NOT execute AI calls or contain business logic.
 */
@Slf4j
@Component
public class PromptProvider {

    private static final String PLANNING_TEMPLATE = """
        You are an AI planning assistant. Your role is to understand user requests
        and produce structured execution plans. You do NOT execute tools, call APIs,
        or perform business logic.

        Available tools:
        - flight.search: Search flights. Arguments: origin, destination, departureDate
        - weather.forecast: Retrieve weather forecasts. Arguments: location, date

        Rules:
        - Determine which tools are required based on the user request
        - Extract structured parameters for each tool
        - Assign a confidence score between 0.0 and 1.0
        - Generate a concise summary of what the plan does
        - Only plan — never execute, never call APIs, never invent data
        - Do not aggregate responses — that is handled by the execution engine

        User request: {userMessage}

        Respond with the following JSON structure:
        {outputFormat}
        """;

    private final BeanOutputConverter<PlanGenerationResult> outputConverter;

    public PromptProvider(BeanOutputConverter<PlanGenerationResult> outputConverter) {
        this.outputConverter = outputConverter;
    }

    /**
     * Builds the complete planning prompt for a given user message.
     * The prompt includes tool descriptions, behavioral rules, and the
     * required output schema derived from {@link PlanGenerationResult}.
     *
     * @param userMessage the raw user input
     * @return the fully rendered prompt string ready for ChatClient
     */
    public String buildPlanningPrompt(final String userMessage) {
        log.debug("Building planning prompt for user message");

        var promptTemplate = new PromptTemplate(PLANNING_TEMPLATE);
        Map<String, Object> variables = Map.of(
                "userMessage", userMessage,
                "outputFormat", outputConverter.getFormat()
        );

        var rendered = promptTemplate.render(variables);

        log.debug("Planning prompt generated successfully");
        return rendered;
    }
}
