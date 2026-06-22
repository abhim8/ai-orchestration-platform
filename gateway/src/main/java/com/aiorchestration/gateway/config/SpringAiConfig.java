package com.aiorchestration.gateway.config;

import com.aiorchestration.gateway.model.PlanGenerationResult;
import com.aiorchestration.gateway.planner.tool.ResolveRelativeDateTool;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class SpringAiConfig {

    @Value("${spring.ai.google.genai.api-key}")
    private String apiKey;

    @Value("${spring.ai.google.genai.chat.model}")
    private String modelName;

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

    @Value("${chat.memory.max-messages}")
    private int maxMessages;

    @Bean
    public ChatMemory chatMemory() {
        log.info("Creating in-memory ChatMemory with maxMessages={} (JVM-local, lost on restart)", maxMessages);
        return MessageWindowChatMemory.builder()
                .maxMessages(maxMessages)
                .build();
    }

    @Bean
    public MessageChatMemoryAdvisor messageChatMemoryAdvisor(ChatMemory chatMemory) {
        log.info("Creating MessageChatMemoryAdvisor");
        return MessageChatMemoryAdvisor.builder(chatMemory)
                .build();
    }
}
