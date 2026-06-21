package com.aiorchestration.gateway.planner;

import com.aiorchestration.gateway.exception.*;
import com.aiorchestration.gateway.model.PlanGenerationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

/**
 * Sole service responsible for interacting with Gemini via Spring AI.
 * Transforms a natural-language user request into a validated
 * {@link PlanGenerationResult}.
 *
 * <p>Planning is intentionally isolated from execution. This service:
 * <ul>
 *   <li>generates a structured prompt via {@link PromptProvider}</li>
 *   <li>sends it to Gemini via {@link ChatClient}</li>
 *   <li>deserializes the response into {@link PlanGenerationResult}</li>
 * </ul>
 *
 * <p>It does NOT execute tools, call downstream services, perform business
 * logic, or aggregate responses. Those responsibilities belong to
 * {@code ExecutionEngineService} and {@code ResponseAggregatorService}.
 *
 * <p>Spring AI is intentionally confined to this layer. No code outside
 * the {@code planner} package or {@code config} package depends on Spring AI
 * types, keeping business logic provider-agnostic.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntentPlannerService {

    private static final String CONVERSATION_ID_KEY = "chat_memory_conversation_id";

    private final ChatClient chatClient;
    private final PromptProvider promptProvider;
    private final BeanOutputConverter<PlanGenerationResult> outputConverter;

    /**
     * Generates an execution plan from a user's natural-language request.
     *
     * @param conversationId the resolved conversation identifier (always a valid UUID)
     * @param message        the user's natural language message
     * @return the generated plan with confidence, summary, and execution steps
     * @throws PlanGenerationException if the AI planner fails to produce a plan
     */
    public PlanGenerationResult plan(final String conversationId, final String message) {
        log.debug("Planning for user message");

        var prompt = promptProvider.buildPlanningPrompt(message);

        try {
            var result = chatClient.prompt()
                    .user(prompt)
                    .advisors(a -> a.param(CONVERSATION_ID_KEY, conversationId))
                    .call()
                    .entity(outputConverter);

            log.debug("Plan generated successfully with confidence: {}", result.confidence());
            return result;
        } catch (HttpClientErrorException e) {
            var status = e.getStatusCode();
            if (HttpStatus.TOO_MANY_REQUESTS.equals(status)) {
                log.warn("AI planning quota exceeded: {}", e.getMessage());
                throw new PlannerQuotaExceededException("AI planning quota exceeded", e);
            } else if (HttpStatus.UNAUTHORIZED.equals(status) || HttpStatus.FORBIDDEN.equals(status)) {
                log.warn("AI planning authentication failed: {}", e.getMessage());
                throw new PlannerAuthenticationException("AI planning authentication failed", e);
            } else {
                log.warn("AI planning request rejected ({}): {}", status, e.getMessage());
                throw new PlannerBadRequestException("AI planning request rejected", e);
            }
        } catch (HttpServerErrorException e) {
            log.warn("AI planning service error ({}): {}", e.getStatusCode(), e.getMessage());
            throw new PlannerUnavailableException("AI planning service unavailable", e);
        } catch (ResourceAccessException e) {
            log.warn("AI planning connectivity failure: {}", e.getMessage());
            throw new PlannerUnavailableException("AI planning service unavailable", e);
        } catch (Exception e) {
            log.error("Unexpected AI planning failure: {}", e.getMessage(), e);
            throw new PlanGenerationException("Failed to generate execution plan", e);
        }
    }
}
