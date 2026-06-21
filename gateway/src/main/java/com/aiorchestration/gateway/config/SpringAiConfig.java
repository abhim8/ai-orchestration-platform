package com.aiorchestration.gateway.config;

import com.aiorchestration.gateway.model.PlanGenerationResult;
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

    @Value("${spring.ai.google.genai.chat.model}")
    private String modelName;

    @PostConstruct
    void logStartupDiagnostics() {
        log.info("Gemini planner configured: model={} | Google SDK retry active: maxAttempts=5, "
                + "retryable=408,429,500,502,503,504, backoff=1s*2^n max=60s jitter=full",
                modelName);
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, MessageChatMemoryAdvisor advisor) {
        log.info("Creating ChatClient bean with default advisors");
        return builder.defaultAdvisors(advisor).build();
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
