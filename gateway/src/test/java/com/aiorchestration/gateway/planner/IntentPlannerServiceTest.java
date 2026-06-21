package com.aiorchestration.gateway.planner;

import com.aiorchestration.gateway.exception.PlanGenerationException;
import com.aiorchestration.gateway.model.ChatRequest;
import com.aiorchestration.gateway.model.ExecutionPlan;
import com.aiorchestration.gateway.model.ExecutionStep;
import com.aiorchestration.gateway.model.PlanGenerationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;

import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.any;

@ExtendWith(MockitoExtension.class)
class IntentPlannerServiceTest {

    private static final String TEST_MESSAGE = "book a flight from London to Paris tomorrow";
    private static final String TEST_SESSION_ID = "session-123";
    private static final String TEST_PROMPT = "prompt-text";

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    @Mock
    private PromptProvider promptProvider;

    @Mock
    private BeanOutputConverter<PlanGenerationResult> outputConverter;

    @Captor
    private ArgumentCaptor<Consumer<ChatClient.AdvisorSpec>> advisorCaptor;

    private IntentPlannerService service;

    private PlanGenerationResult expectedResult;

    @BeforeEach
    void setUp() {
        service = new IntentPlannerService(chatClient, promptProvider, outputConverter);

        var steps = List.of(new ExecutionStep("step-1", "flight.search",
                null, null));
        var plan = new ExecutionPlan(steps);
        expectedResult = new PlanGenerationResult(0.95, "Book flight", plan);
    }

    @Test
    @DisplayName("should generate a plan successfully")
    void shouldGeneratePlanSuccessfully() {
        when(promptProvider.buildPlanningPrompt(TEST_MESSAGE)).thenReturn(TEST_PROMPT);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(TEST_PROMPT)).thenReturn(requestSpec);
        when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.entity(outputConverter)).thenReturn(expectedResult);

        var request = new ChatRequest(TEST_SESSION_ID, TEST_MESSAGE);
        var result = service.plan(request);

        assertNotNull(result);
        assertEquals(0.95, result.confidence());
        assertEquals("Book flight", result.summary());
        assertEquals(1, result.executionPlan().steps().size());

        verify(promptProvider).buildPlanningPrompt(TEST_MESSAGE);
        verify(requestSpec).user(TEST_PROMPT);
        verify(requestSpec).call();
        verify(callResponseSpec).entity(outputConverter);
    }

    @Test
    @DisplayName("should use sessionId as conversation ID when provided")
    void shouldUseSessionIdAsConversationId() {
        when(promptProvider.buildPlanningPrompt(TEST_MESSAGE)).thenReturn(TEST_PROMPT);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(TEST_PROMPT)).thenReturn(requestSpec);
        when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.entity(outputConverter)).thenReturn(expectedResult);

        var request = new ChatRequest(TEST_SESSION_ID, TEST_MESSAGE);
        service.plan(request);

        verify(requestSpec).advisors(advisorCaptor.capture());
        var advisorConsumer = advisorCaptor.getValue();

        var advisorSpec = mock(ChatClient.AdvisorSpec.class);
        advisorConsumer.accept(advisorSpec);

        verify(advisorSpec).param("chat_memory_conversation_id", TEST_SESSION_ID);
    }

    @Test
    @DisplayName("should generate random conversation ID when no sessionId")
    void shouldGenerateRandomIdWhenNoSessionId() {
        when(promptProvider.buildPlanningPrompt(TEST_MESSAGE)).thenReturn(TEST_PROMPT);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(TEST_PROMPT)).thenReturn(requestSpec);
        when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.entity(outputConverter)).thenReturn(expectedResult);

        var request = new ChatRequest(null, TEST_MESSAGE);
        service.plan(request);

        verify(requestSpec).advisors(advisorCaptor.capture());
        var advisorConsumer = advisorCaptor.getValue();

        var advisorSpec = mock(ChatClient.AdvisorSpec.class);
        advisorConsumer.accept(advisorSpec);

        verify(advisorSpec).param(eq("chat_memory_conversation_id"), anyString());
    }

    @Test
    @DisplayName("should throw PlanGenerationException when AI call fails")
    void shouldThrowOnAiFailure() {
        when(promptProvider.buildPlanningPrompt(TEST_MESSAGE)).thenReturn(TEST_PROMPT);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(TEST_PROMPT)).thenReturn(requestSpec);
        when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenThrow(new RuntimeException("API error"));

        var request = new ChatRequest(TEST_SESSION_ID, TEST_MESSAGE);

        var exception = assertThrows(PlanGenerationException.class,
                () -> service.plan(request));
        assertTrue(exception.getMessage().contains("Failed to generate execution plan"));
        assertNotNull(exception.getCause());
    }

    @Test
    @DisplayName("should invoke PromptProvider")
    void shouldInvokePromptProvider() {
        when(promptProvider.buildPlanningPrompt(TEST_MESSAGE)).thenReturn(TEST_PROMPT);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(TEST_PROMPT)).thenReturn(requestSpec);
        when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.entity(outputConverter)).thenReturn(expectedResult);

        var request = new ChatRequest(TEST_SESSION_ID, TEST_MESSAGE);
        service.plan(request);

        verify(promptProvider).buildPlanningPrompt(TEST_MESSAGE);
    }
}
