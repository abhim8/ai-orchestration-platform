package com.aiorchestration.gateway.planner;

import com.aiorchestration.gateway.exception.*;
import com.aiorchestration.gateway.model.PlanGenerationResult;
import com.google.genai.errors.ApiException;
import com.google.genai.errors.GenAiIOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntentPlannerService {

    private static final String CONVERSATION_ID_KEY = "chat_memory_conversation_id";
    private static final int INITIAL_ATTEMPT = 1;

    @Value("${spring.ai.google.genai.chat.model}")
    private String modelName;

    private final ChatClient chatClient;
    private final PromptProvider promptProvider;
    private final BeanOutputConverter<PlanGenerationResult> outputConverter;

    public PlanGenerationResult plan(final String conversationId, final String message) {
        var prompt = promptProvider.buildPlanningPrompt(message);

        log.debug("Calling Gemini planner: conversationId={}, model={}, operation=planning, attempt={}, reason=Generate execution plan",
                conversationId, modelName, INITIAL_ATTEMPT);

        var startNanos = System.nanoTime();

        try {
            var callResponse = chatClient.prompt()
                    .user(prompt)
                    .advisors(a -> a.param(CONVERSATION_ID_KEY, conversationId))
                    .call();

            var rawContent = Objects.requireNonNull(callResponse.content());
            log.debug("Raw planner output: conversationId={}, rawContent={}", conversationId, rawContent);

            var result = outputConverter.convert(rawContent);
            log.debug("Final ExecutionPlan after conversion: conversationId={}, executionPlan={}",
                    conversationId, result.executionPlan());

            var latencyMs = elapsedMillis(startNanos);
            log.debug("Gemini planner call completed successfully: conversationId={}, attempt={}, latencyMs={}",
                    conversationId, INITIAL_ATTEMPT, latencyMs);

            return result;
        } catch (Exception e) {
            var latencyMs = elapsedMillis(startNanos);
            throw translateAndLog(e, conversationId, latencyMs);
        }
    }

    private static long elapsedMillis(final long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private RuntimeException translateAndLog(final Exception e, final String conversationId,
                                              final long latencyMs) {
        var cause = unwrapGoogleGenAiException(e);

        if (cause instanceof ApiException apiEx) {
            return handleApiException(apiEx, e, conversationId, latencyMs);
        }
        if (cause instanceof GenAiIOException) {
            log.warn("Gemini planner call failed: conversationId={}, attempt={}, exception=GenAiIOException, latencyMs={}",
                    conversationId, INITIAL_ATTEMPT, latencyMs);
            return new PlannerUnavailableException("AI planning service unavailable", e);
        }
        if (cause instanceof HttpClientErrorException hce) {
            return handleHttpClientError(hce, e, conversationId, latencyMs);
        }
        if (cause instanceof HttpServerErrorException) {
            log.warn("Gemini planner call failed: conversationId={}, attempt={}, status=500, exception=HttpServerErrorException, latencyMs={}",
                    conversationId, INITIAL_ATTEMPT, latencyMs);
            return new PlannerUnavailableException("AI planning service unavailable", e);
        }
        if (cause instanceof ResourceAccessException) {
            log.warn("Gemini planner call failed: conversationId={}, attempt={}, exception=ResourceAccessException, latencyMs={}",
                    conversationId, INITIAL_ATTEMPT, latencyMs);
            return new PlannerUnavailableException("AI planning service unavailable", e);
        }

        log.error("Gemini planner call failed unexpectedly: conversationId={}, latencyMs={}", conversationId, latencyMs, e);
        return new PlanGenerationException("Failed to generate execution plan", e);
    }

    private static Throwable unwrapGoogleGenAiException(final Exception e) {
        if (e instanceof RuntimeException re
                && "Failed to generate content".equals(re.getMessage())
                && re.getCause() != null) {
            return re.getCause();
        }
        return e;
    }

    private RuntimeException handleApiException(final ApiException apiEx, final Exception original,
                                                 final String conversationId, final long latencyMs) {
        var code = apiEx.code();
        var simpleName = apiEx.getClass().getSimpleName();

        log.warn("Gemini planner call failed: conversationId={}, attempt={}, status={}, exception={}, latencyMs={}",
                conversationId, INITIAL_ATTEMPT, code, simpleName, latencyMs);

        if (code == 429) {
            return new PlannerQuotaExceededException("AI planning quota exceeded", original);
        }
        if (code == 401 || code == 403) {
            return new PlannerAuthenticationException("AI planning authentication failed", original);
        }
        if (code >= 400 && code < 500) {
            return new PlannerBadRequestException("AI planning request rejected", original);
        }
        return new PlannerUnavailableException("AI planning service unavailable", original);
    }

    private RuntimeException handleHttpClientError(final HttpClientErrorException hce,
                                                    final Exception original,
                                                    final String conversationId, final long latencyMs) {
        var status = hce.getStatusCode();

        log.warn("Gemini planner call failed: conversationId={}, attempt={}, status={}, exception=HttpClientErrorException, latencyMs={}",
                conversationId, INITIAL_ATTEMPT, status.value(), latencyMs);

        if (HttpStatus.TOO_MANY_REQUESTS.equals(status)) {
            return new PlannerQuotaExceededException("AI planning quota exceeded", original);
        }
        if (HttpStatus.UNAUTHORIZED.equals(status) || HttpStatus.FORBIDDEN.equals(status)) {
            return new PlannerAuthenticationException("AI planning authentication failed", original);
        }
        return new PlannerBadRequestException("AI planning request rejected", original);
    }
}
