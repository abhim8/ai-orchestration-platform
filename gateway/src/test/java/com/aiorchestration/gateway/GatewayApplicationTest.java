package com.aiorchestration.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"GEMINI_API_KEY=test-key", "AI_PLANNER_ENABLED=true"})
class GatewayApplicationTest {

    @Test
    void contextLoads() {
    }
}
