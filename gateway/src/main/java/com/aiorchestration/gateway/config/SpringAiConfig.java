package com.aiorchestration.gateway.config;

import com.aiorchestration.gateway.model.PlanGenerationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI infrastructure configuration.
 *
 * Creates singleton beans for ChatClient, BeanOutputConverter, ChatMemory,
 * and MessageChatMemoryAdvisor. All beans are provider-agnostic and rely
 * on auto-configured Spring AI components. Gemini-specific configuration
 * is externalized to application.yml.
 */
@Slf4j
@Configuration
public class SpringAiConfig {

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
