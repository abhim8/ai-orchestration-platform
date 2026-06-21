package com.aiorchestration.gateway.planner;

import com.aiorchestration.gateway.exception.*;
import com.aiorchestration.gateway.model.ExecutionPlan;
import com.aiorchestration.gateway.model.ExecutionStep;
import com.aiorchestration.gateway.model.PlanGenerationResult;
import com.google.genai.errors.ClientException;
import com.google.genai.errors.GenAiIOException;
import com.google.genai.errors.ServerException;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IntentPlannerServiceTest {

    private static final String CONVERSATION_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String TEST_MESSAGE = "book a flight from London to Paris tomorrow";
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

        var result = service.plan(CONVERSATION_ID, TEST_MESSAGE);

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
    @DisplayName("should use provided conversationId for Spring AI advisor")
    void shouldUseProvidedConversationId() {
        when(promptProvider.buildPlanningPrompt(TEST_MESSAGE)).thenReturn(TEST_PROMPT);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(TEST_PROMPT)).thenReturn(requestSpec);
        when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.entity(outputConverter)).thenReturn(expectedResult);

        service.plan(CONVERSATION_ID, TEST_MESSAGE);

        verify(requestSpec).advisors(advisorCaptor.capture());
        var advisorConsumer = advisorCaptor.getValue();

        var advisorSpec = mock(ChatClient.AdvisorSpec.class);
        advisorConsumer.accept(advisorSpec);

        verify(advisorSpec).param("chat_memory_conversation_id", CONVERSATION_ID);
    }

    @Test
    @DisplayName("should throw PlanGenerationException for unexpected failures")
    void shouldThrowPlanGenerationOnUnexpectedFailure() {
        when(promptProvider.buildPlanningPrompt(TEST_MESSAGE)).thenReturn(TEST_PROMPT);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(TEST_PROMPT)).thenReturn(requestSpec);
        when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenThrow(new RuntimeException("API error"));

        var exception = assertThrows(PlanGenerationException.class,
                () -> service.plan(CONVERSATION_ID, TEST_MESSAGE));
        assertTrue(exception.getMessage().contains("Failed to generate execution plan"));
        assertNotNull(exception.getCause());
    }

    @Test
    @DisplayName("should throw PlannerBadRequestException for 4xx ClientException")
    void shouldThrowPlannerBadRequestForClientException() {
        when(promptProvider.buildPlanningPrompt(TEST_MESSAGE)).thenReturn(TEST_PROMPT);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(TEST_PROMPT)).thenReturn(requestSpec);
        when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenThrow(
                new RuntimeException("Failed to generate content", new ClientException(400, "BAD_REQUEST", "Bad request")));

        var exception = assertThrows(PlannerBadRequestException.class,
                () -> service.plan(CONVERSATION_ID, TEST_MESSAGE));
        assertTrue(exception.getMessage().contains("AI planning request rejected"));
        assertTrue(exception.getCause() instanceof RuntimeException);
        assertNotNull(exception.getCause().getCause());
        assertInstanceOf(ClientException.class, exception.getCause().getCause());
    }

    @Test
    @DisplayName("should throw PlannerAuthenticationException for 401 ClientException")
    void shouldThrowPlannerAuthenticationFor401ClientException() {
        when(promptProvider.buildPlanningPrompt(TEST_MESSAGE)).thenReturn(TEST_PROMPT);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(TEST_PROMPT)).thenReturn(requestSpec);
        when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenThrow(
                new RuntimeException("Failed to generate content", new ClientException(401, "UNAUTHENTICATED", "Unauthenticated")));

        var exception = assertThrows(PlannerAuthenticationException.class,
                () -> service.plan(CONVERSATION_ID, TEST_MESSAGE));
        assertTrue(exception.getMessage().contains("AI planning authentication failed"));
    }

    @Test
    @DisplayName("should throw PlannerAuthenticationException for 403 ClientException")
    void shouldThrowPlannerAuthenticationFor403ClientException() {
        when(promptProvider.buildPlanningPrompt(TEST_MESSAGE)).thenReturn(TEST_PROMPT);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(TEST_PROMPT)).thenReturn(requestSpec);
        when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenThrow(
                new RuntimeException("Failed to generate content", new ClientException(403, "FORBIDDEN", "Forbidden")));

        var exception = assertThrows(PlannerAuthenticationException.class,
                () -> service.plan(CONVERSATION_ID, TEST_MESSAGE));
        assertTrue(exception.getMessage().contains("AI planning authentication failed"));
    }

    @Test
    @DisplayName("should throw PlannerQuotaExceededException for 429 ClientException")
    void shouldThrowPlannerQuotaExceededFor429ClientException() {
        when(promptProvider.buildPlanningPrompt(TEST_MESSAGE)).thenReturn(TEST_PROMPT);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(TEST_PROMPT)).thenReturn(requestSpec);
        when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenThrow(
                new RuntimeException("Failed to generate content", new ClientException(429, "RATE_LIMITED", "Rate limited")));

        var exception = assertThrows(PlannerQuotaExceededException.class,
                () -> service.plan(CONVERSATION_ID, TEST_MESSAGE));
        assertTrue(exception.getMessage().contains("AI planning quota exceeded"));
    }

    @Test
    @DisplayName("should throw PlannerUnavailableException for 5xx ServerException")
    void shouldThrowPlannerUnavailableForServerException() {
        when(promptProvider.buildPlanningPrompt(TEST_MESSAGE)).thenReturn(TEST_PROMPT);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(TEST_PROMPT)).thenReturn(requestSpec);
        when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenThrow(
                new RuntimeException("Failed to generate content", new ServerException(503, "UNAVAILABLE", "Service unavailable")));

        var exception = assertThrows(PlannerUnavailableException.class,
                () -> service.plan(CONVERSATION_ID, TEST_MESSAGE));
        assertTrue(exception.getMessage().contains("AI planning service unavailable"));
    }

    @Test
    @DisplayName("should throw PlannerUnavailableException for GenAiIOException")
    void shouldThrowPlannerUnavailableForGenAiIOException() {
        when(promptProvider.buildPlanningPrompt(TEST_MESSAGE)).thenReturn(TEST_PROMPT);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(TEST_PROMPT)).thenReturn(requestSpec);
        when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenThrow(
                new RuntimeException("Failed to generate content", new GenAiIOException("Connection reset")));

        var exception = assertThrows(PlannerUnavailableException.class,
                () -> service.plan(CONVERSATION_ID, TEST_MESSAGE));
        assertTrue(exception.getMessage().contains("AI planning service unavailable"));
    }

    @Test
    @DisplayName("should throw PlanGenerationException for non-Google RuntimeException without API exception cause")
    void shouldThrowPlanGenerationForUnknownGoogleWrapper() {
        when(promptProvider.buildPlanningPrompt(TEST_MESSAGE)).thenReturn(TEST_PROMPT);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(TEST_PROMPT)).thenReturn(requestSpec);
        when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenThrow(
                new RuntimeException("Failed to generate content", new RuntimeException("Unknown cause")));

        var exception = assertThrows(PlanGenerationException.class,
                () -> service.plan(CONVERSATION_ID, TEST_MESSAGE));
        assertTrue(exception.getMessage().contains("Failed to generate execution plan"));
    }

    @Test
    @DisplayName("should throw PlannerBadRequestException for 4xx Spring HTTP errors")
    void shouldThrowPlannerBadRequestFor4xx() {
        when(promptProvider.buildPlanningPrompt(TEST_MESSAGE)).thenReturn(TEST_PROMPT);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(TEST_PROMPT)).thenReturn(requestSpec);
        when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        var exception = assertThrows(PlannerBadRequestException.class,
                () -> service.plan(CONVERSATION_ID, TEST_MESSAGE));
        assertTrue(exception.getMessage().contains("AI planning request rejected"));
        assertNotNull(exception.getCause());
    }

    @Test
    @DisplayName("should throw PlannerAuthenticationException for 401 Spring HTTP errors")
    void shouldThrowPlannerAuthenticationFor401() {
        when(promptProvider.buildPlanningPrompt(TEST_MESSAGE)).thenReturn(TEST_PROMPT);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(TEST_PROMPT)).thenReturn(requestSpec);
        when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED));

        var exception = assertThrows(PlannerAuthenticationException.class,
                () -> service.plan(CONVERSATION_ID, TEST_MESSAGE));
        assertTrue(exception.getMessage().contains("AI planning authentication failed"));
        assertNotNull(exception.getCause());
    }

    @Test
    @DisplayName("should throw PlannerAuthenticationException for 403 Spring HTTP errors")
    void shouldThrowPlannerAuthenticationFor403() {
        when(promptProvider.buildPlanningPrompt(TEST_MESSAGE)).thenReturn(TEST_PROMPT);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(TEST_PROMPT)).thenReturn(requestSpec);
        when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenThrow(new HttpClientErrorException(HttpStatus.FORBIDDEN));

        var exception = assertThrows(PlannerAuthenticationException.class,
                () -> service.plan(CONVERSATION_ID, TEST_MESSAGE));
        assertTrue(exception.getMessage().contains("AI planning authentication failed"));
        assertNotNull(exception.getCause());
    }

    @Test
    @DisplayName("should throw PlannerQuotaExceededException for 429 Spring HTTP errors")
    void shouldThrowPlannerQuotaExceededFor429() {
        when(promptProvider.buildPlanningPrompt(TEST_MESSAGE)).thenReturn(TEST_PROMPT);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(TEST_PROMPT)).thenReturn(requestSpec);
        when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenThrow(new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS));

        var exception = assertThrows(PlannerQuotaExceededException.class,
                () -> service.plan(CONVERSATION_ID, TEST_MESSAGE));
        assertTrue(exception.getMessage().contains("AI planning quota exceeded"));
        assertNotNull(exception.getCause());
    }

    @Test
    @DisplayName("should throw PlannerUnavailableException for 5xx Spring HTTP errors")
    void shouldThrowPlannerUnavailableFor5xx() {
        when(promptProvider.buildPlanningPrompt(TEST_MESSAGE)).thenReturn(TEST_PROMPT);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(TEST_PROMPT)).thenReturn(requestSpec);
        when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

        var exception = assertThrows(PlannerUnavailableException.class,
                () -> service.plan(CONVERSATION_ID, TEST_MESSAGE));
        assertTrue(exception.getMessage().contains("AI planning service unavailable"));
        assertNotNull(exception.getCause());
    }

    @Test
    @DisplayName("should throw PlannerUnavailableException for connectivity failures")
    void shouldThrowPlannerUnavailableForConnectivity() {
        when(promptProvider.buildPlanningPrompt(TEST_MESSAGE)).thenReturn(TEST_PROMPT);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(TEST_PROMPT)).thenReturn(requestSpec);
        when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenThrow(new ResourceAccessException("Connection refused"));

        var exception = assertThrows(PlannerUnavailableException.class,
                () -> service.plan(CONVERSATION_ID, TEST_MESSAGE));
        assertTrue(exception.getMessage().contains("AI planning service unavailable"));
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

        service.plan(CONVERSATION_ID, TEST_MESSAGE);

        verify(promptProvider).buildPlanningPrompt(TEST_MESSAGE);
    }
}
