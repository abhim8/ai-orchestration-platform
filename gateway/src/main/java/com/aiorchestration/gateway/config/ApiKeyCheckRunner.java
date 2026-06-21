package com.aiorchestration.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(0)
public class ApiKeyCheckRunner implements ApplicationRunner {

    @Override
    public void run(final @NonNull ApplicationArguments args) {
        var apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey != null && !apiKey.isBlank()) {
            log.info("GEMINI_API_KEY is configured. AI planning is enabled.");
        } else {
            log.warn("GEMINI_API_KEY is not configured. Chat planning requests will fail until a valid API key is provided.");
        }
    }
}
