package com.aiorchestration.gateway.planner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DeterministicExecutionPlanFactoryTest {

    private DeterministicExecutionPlanFactory factory;

    @BeforeEach
    void setUp() {
        factory = new DeterministicExecutionPlanFactory();
    }

    @Test
    @DisplayName("should create flight search step when message contains 'flight'")
    void shouldCreateFlightStep() {
        var result = factory.createPlan("book a flight to tokyo");

        assertEquals(1.0, result.confidence());
        var steps = result.executionPlan().steps();
        assertEquals(1, steps.size());
        assertEquals("step-1", steps.getFirst().stepId());
        assertEquals("flight.search", steps.getFirst().tool());
        assertEquals("BLR", steps.getFirst().arguments().get("origin"));
        assertEquals("NRT", steps.getFirst().arguments().get("destination"));
        assertNotNull(steps.getFirst().arguments().get("departureDate"));
        assertTrue(result.summary().contains("flight"));
    }

    @Test
    @DisplayName("should create weather forecast step when message contains 'weather'")
    void shouldCreateWeatherStep() {
        var result = factory.createPlan("what is the weather in tokyo");

        assertEquals(1.0, result.confidence());
        var steps = result.executionPlan().steps();
        assertEquals(1, steps.size());
        assertEquals("step-1", steps.getFirst().stepId());
        assertEquals("weather.forecast", steps.getFirst().tool());
        assertEquals("Tokyo", steps.getFirst().arguments().get("location"));
        assertNotNull(steps.getFirst().arguments().get("date"));
        assertTrue(result.summary().contains("weather"));
    }

    @Test
    @DisplayName("should create both steps when message contains 'flight' and 'weather'")
    void shouldCreateBothSteps() {
        var result = factory.createPlan("book a flight and check weather");

        assertEquals(1.0, result.confidence());
        var steps = result.executionPlan().steps();
        assertEquals(2, steps.size());
        assertEquals("flight.search", steps.get(0).tool());
        assertEquals("weather.forecast", steps.get(1).tool());
        assertEquals(1, steps.get(1).dependsOn().size());
        assertEquals("step-1", steps.get(1).dependsOn().getFirst());
        assertTrue(result.summary().contains("flight"));
        assertTrue(result.summary().contains("weather"));
    }

    @Test
    @DisplayName("should return empty plan when no keywords detected")
    void shouldReturnEmptyPlanForUnknownMessage() {
        var result = factory.createPlan("hello world");

        assertEquals(1.0, result.confidence());
        assertTrue(result.executionPlan().steps().isEmpty());
        assertEquals("No actions detected", result.summary());
    }

    @Test
    @DisplayName("should be case insensitive")
    void shouldBeCaseInsensitive() {
        var result = factory.createPlan("FLIGHT to NRT and WEATHER in Tokyo");

        assertEquals(2, result.executionPlan().steps().size());
        assertEquals("flight.search", result.executionPlan().steps().get(0).tool());
        assertEquals("weather.forecast", result.executionPlan().steps().get(1).tool());
    }

    @Test
    @DisplayName("should always return confidence 1.0")
    void shouldAlwaysReturnMaxConfidence() {
        var result = factory.createPlan("some random text");
        assertEquals(1.0, result.confidence());
    }

    @Test
    @DisplayName("should produce plans that pass validation with ToolRegistry")
    void shouldProduceValidatablePlan() {
        var result = factory.createPlan("book a flight and check weather");
        var registry = new com.aiorchestration.gateway.registry.ToolRegistry();
        var validator = new ExecutionPlanValidator(registry);

        validator.validate(result);
    }
}
