package com.aiorchestration.gateway.planner;

import com.aiorchestration.gateway.model.PlanGenerationResult;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class PromptProvider {

    private static final String PLANNING_TEMPLATE = """
        You are an AI planning assistant. Your role is to understand user requests
        and produce structured execution plans. You do NOT execute business tools,
        call external APIs, or perform business logic.

        Available tools (these are the ONLY tools that may appear in the execution
        plan):
        - flight.search: Search flights. Arguments: origin, destination,
          departureDate (must be ISO-8601 yyyy-MM-dd)
        - weather.forecast: Retrieve weather forecasts. Arguments: location, date
          (must be ISO-8601 yyyy-MM-dd)

        Internal planning helpers (do NOT include these in the execution plan):
        - resolveRelativeDate: Resolves relative date expressions to ISO-8601
          (yyyy-MM-dd) strings. Available to you during planning only. It is
          NOT an executable tool and must NEVER appear in the generated JSON.

        Rules:
        - Determine which business tools are required based on the user request
        - Extract structured parameters for each business tool
        - Assign a confidence score between 0.0 and 1.0
        - Generate a concise summary of what the plan does
        - Only plan — never execute business tools, never call external APIs,
          never invent data
        - Do not aggregate responses — that is handled by the execution engine

        Date resolution (MANDATORY):
        - Before generating the final JSON, resolve every relative date expression
          (e.g. "today", "tomorrow", "next Friday", "next week", "this weekend")
          to an ISO-8601 date string (yyyy-MM-dd) using the resolveRelativeDate
          internal helper.
        - The final JSON must contain only concrete literal date values. Every
          date field must already contain the resolved ISO-8601 string.

        Forbidden output:
        - The execution plan does NOT support variables, placeholders, template
          expressions, references to previous steps, or interpolation.
        - Strings such as "${...}", "{{...}}", "resolveRelativeDate", or outputs
          from previous steps must NEVER appear anywhere in the final JSON.
        - The final execution plan must contain only business tools
          (flight.search, weather.forecast, etc.). Never include
          resolveRelativeDate as a step.

        User request: {userMessage}

        Respond with the following JSON structure:
        {outputFormat}
        """;

    private final BeanOutputConverter<PlanGenerationResult> outputConverter;

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
