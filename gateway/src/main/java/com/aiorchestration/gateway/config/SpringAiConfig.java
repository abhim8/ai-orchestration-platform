package com.aiorchestration.gateway.config;

import com.aiorchestration.gateway.model.PlanGenerationResult;
import com.aiorchestration.gateway.planner.tool.ResolveRelativeDateTool;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Configuration
@EnableScheduling
public class SpringAiConfig {

    @Value("${spring.ai.google.genai.api-key}")
    private String apiKey;

    @Value("${spring.ai.google.genai.chat.model}")
    private String modelName;

    private final Map<String, Instant> conversationAccessTimes = new ConcurrentHashMap<>();
    private ChatMemory chatMemoryDelegate;

    @Value("${chat.memory.ttl-minutes}")
    private int ttlMinutes;

    @Value("${chat.memory.max-messages}")
    private int maxMessages;

    @PostConstruct
    void logStartupDiagnostics() {

        if (apiKey != null && !apiKey.isBlank())
            log.info("GEMINI_API_KEY is configured. AI planning is enabled. Model={}", modelName);
        else
            log.warn("GEMINI_API_KEY is not configured. Chat planning requests will fail until a valid API key is provided.");

        log.info("Using Google GenAI SDK built-in retry policy:" +
                "Google SDK retry active: maxAttempts=5, " +
                "retryable=408,429,500,502,503,504, backoff=1s*2^n max=60s jitter=full");
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, MessageChatMemoryAdvisor advisor,
                                 ResolveRelativeDateTool resolveRelativeDateTool) {
        log.info("Creating ChatClient bean with default advisors and resolveRelativeDate tool");
        return builder.defaultAdvisors(advisor)
                .defaultTools(resolveRelativeDateTool)
                .build();
    }

    @Bean
    public BeanOutputConverter<PlanGenerationResult> planGenerationResultConverter() {
        log.info("Creating BeanOutputConverter for PlanGenerationResult");
        return new BeanOutputConverter<>(PlanGenerationResult.class);
    }

    @Bean
    public ChatMemory chatMemory() {
        chatMemoryDelegate = MessageWindowChatMemory.builder()
                .maxMessages(maxMessages)
                .build();

        log.info("Creating ChatMemory with maxMessages={}, ttl={}min (JVM-local, stale conversations evicted every 5min)",
                maxMessages, ttlMinutes);

        return new ChatMemory() {
            @Override
            public void add(final @NonNull String conversationId, final org.springframework.ai.chat.messages.@NonNull Message message) {
                conversationAccessTimes.put(conversationId, Instant.now());
                chatMemoryDelegate.add(conversationId, message);
            }

            @Override
            public void add(final @NonNull String conversationId, final java.util.@NonNull List<org.springframework.ai.chat.messages.Message> messages) {
                conversationAccessTimes.put(conversationId, Instant.now());
                chatMemoryDelegate.add(conversationId, messages);
            }

            @Override
            public java.util.@NonNull List<org.springframework.ai.chat.messages.Message> get(final @NonNull String conversationId) {
                conversationAccessTimes.put(conversationId, Instant.now());
                return chatMemoryDelegate.get(conversationId);
            }

            @Override
            public void clear(final @NonNull String conversationId) {
                conversationAccessTimes.remove(conversationId);
                chatMemoryDelegate.clear(conversationId);
            }
        };
    }

    @Scheduled(fixedRateString = "${chat.memory.cleanup-interval-ms}")
    void evictStaleConversations() {
        var cutoff = Instant.now().minus(Duration.ofMinutes(ttlMinutes));
        var evicted = 0;
        var idsToEvict = new java.util.ArrayList<String>();

        for (var entry : conversationAccessTimes.entrySet()) {
            if (entry.getValue().isBefore(cutoff)) {
                idsToEvict.add(entry.getKey());
            }
        }

        for (var conversationId : idsToEvict) {
            conversationAccessTimes.remove(conversationId);
            if (chatMemoryDelegate != null) {
                chatMemoryDelegate.clear(conversationId);
            }
            evicted++;
        }

        if (evicted > 0) {
            log.debug("Evicted {} stale conversations", evicted);
        }
    }

    @Bean
    public MessageChatMemoryAdvisor messageChatMemoryAdvisor(ChatMemory chatMemory) {
        log.info("Creating MessageChatMemoryAdvisor");
        return MessageChatMemoryAdvisor.builder(chatMemory)
                .build();
    }
}
