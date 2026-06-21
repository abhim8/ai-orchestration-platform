package com.aiorchestration.gateway.service;

import com.aiorchestration.gateway.model.ExecutionPlan;
import com.aiorchestration.gateway.model.ExecutionStep;
import com.aiorchestration.gateway.model.StepStatus;
import com.aiorchestration.gateway.registry.ToolRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ExecutionEngineServiceTest {

    private ToolRegistry toolRegistry;
    private ExecutionEngineService service;

    @BeforeEach
    void setUp() {
        toolRegistry = new ToolRegistry();
    }

    private static ExecutionStep step(final String stepId, final String tool,
                                      final Map<String, Object> arguments,
                                      final List<String> dependsOn) {
        return new ExecutionStep(stepId, tool, arguments, dependsOn);
    }

    // ---- 1. Single independent step ----

    @Test
    @DisplayName("should execute a single independent step successfully")
    void shouldExecuteSingleIndependentStep() {
        var executors = Map.<String, ToolExecutor>of("flight.search", args -> Map.of("status", "ok"));
        service = new ExecutionEngineService(toolRegistry, executors);

        var steps = List.of(step("1", "flight.search", Map.of("origin", "LHR"), List.of()));
        var results = service.execute(new ExecutionPlan(steps));

        assertEquals(1, results.size());
        assertEquals("1", results.getFirst().stepId());
        assertEquals(StepStatus.SUCCESS, results.getFirst().status());
        assertNull(results.getFirst().error());
        assertTrue(results.getFirst().latencyMs() >= 0);
    }

    // ---- 2. Two independent steps executing successfully ----

    @Test
    @DisplayName("should execute two independent steps")
    void shouldExecuteTwoIndependentSteps() {
        var executors = Map.<String, ToolExecutor>of(
                "flight.search", args -> Map.of("status", "ok"),
                "weather.forecast", args -> Map.of("temp", 25)
        );
        service = new ExecutionEngineService(toolRegistry, executors);

        var steps = List.of(
                step("1", "flight.search", Map.of(), List.of()),
                step("2", "weather.forecast", Map.of(), List.of())
        );
        var results = service.execute(new ExecutionPlan(steps));

        assertEquals(2, results.size());
        assertAll(
                () -> assertEquals(StepStatus.SUCCESS, results.get(0).status()),
                () -> assertEquals(StepStatus.SUCCESS, results.get(1).status())
        );
    }

    // ---- 3. Multiple parallel root steps ----

    @Test
    @DisplayName("should execute parallel root steps concurrently")
    void shouldExecuteParallelRootStepsConcurrently() throws Exception {
        var latch = new CountDownLatch(1);
        var started = new AtomicInteger(0);

        var executors = Map.<String, ToolExecutor>of(
                "flight.search", args -> {
                    started.incrementAndGet();
                    latch.await();
                    return "done";
                },
                "weather.forecast", args -> {
                    started.incrementAndGet();
                    latch.await();
                    return "done";
                }
        );
        service = new ExecutionEngineService(toolRegistry, executors);

        var steps = List.of(
                step("1", "flight.search", Map.of(), List.of()),
                step("2", "weather.forecast", Map.of(), List.of())
            );

        var future = java.util.concurrent.CompletableFuture.supplyAsync(() -> service.execute(new ExecutionPlan(steps)));
        Thread.sleep(100); // allow both tasks to reach the latch
        assertEquals(2, started.get(), "Both steps should have started concurrently");
        latch.countDown();
        var results = future.get();
        assertEquals(2, results.size());
    }

    // ---- 4. Simple dependency chain ----

    @Test
    @DisplayName("should execute steps in dependency order")
    void shouldExecuteDependencyChain() {
        var order = Collections.synchronizedList(new ArrayList<String>());

        var executors = Map.<String, ToolExecutor>of(
                "flight.search", args -> {
                    order.add("flight");
                    return "done";
                },
                "weather.forecast", args -> {
                    order.add("weather");
                    return "done";
                }
        );
        service = new ExecutionEngineService(toolRegistry, executors);

        // 1 -> 2
        var steps = List.of(
                step("1", "flight.search", Map.of(), List.of()),
                step("2", "weather.forecast", Map.of(), List.of("1"))
        );
        var results = service.execute(new ExecutionPlan(steps));

        assertEquals(2, results.size());
        assertEquals(StepStatus.SUCCESS, results.get(0).status());
        assertEquals(StepStatus.SUCCESS, results.get(1).status());
        assertEquals(List.of("flight", "weather"), order);
    }

    // ---- 5. Branching DAG ----

    @Test
    @DisplayName("should execute branching DAG correctly")
    void shouldExecuteBranchingDag() {
        var order = Collections.synchronizedList(new ArrayList<String>());

        var executors = Map.<String, ToolExecutor>of(
                "flight.search", args -> {
                    order.add((String) args.get("id"));
                    return "done";
                },
                "weather.forecast", args -> {
                    order.add((String) args.get("id"));
                    return "done";
                }
        );
        service = new ExecutionEngineService(toolRegistry, executors);

        //   1
        //  / \
        // 2   3
        //  \ /
        //   4
        var steps = List.of(
                step("1", "flight.search", Map.of("id", "1"), List.of()),
                step("2", "weather.forecast", Map.of("id", "2"), List.of("1")),
                step("3", "flight.search", Map.of("id", "3"), List.of("1")),
                step("4", "weather.forecast", Map.of("id", "4"), List.of("2", "3"))
        );
        var results = service.execute(new ExecutionPlan(steps));

        assertEquals(4, results.size());
        results.forEach(r -> assertEquals(StepStatus.SUCCESS, r.status()));
        assertEquals("1", order.get(0));
        // 2 and 3 can appear in either order (concurrent)
        assertTrue(order.indexOf("2") < order.indexOf("4"));
        assertTrue(order.indexOf("3") < order.indexOf("4"));
    }

    // ---- 6. One branch fails, another succeeds ----

    @Test
    @DisplayName("should continue executing when one branch fails")
    void shouldContinueOnBranchFailure() {
        var executors = Map.<String, ToolExecutor>of(
                "flight.search", args -> {
                    if ("1".equals(args.get("id"))) {
                        throw new RuntimeException("Flight search failed");
                    }
                    return "done";
                },
                "weather.forecast", args -> "done"
        );
        service = new ExecutionEngineService(toolRegistry, executors);

        // 1 (fails)    2 (succeeds)
        // no dependency between them
        var steps = List.of(
                step("1", "flight.search", Map.of("id", "1"), List.of()),
                step("2", "weather.forecast", Map.of(), List.of())
        );
        var results = service.execute(new ExecutionPlan(steps));

        assertEquals(2, results.size());
        assertEquals(StepStatus.FAILED, results.get(0).status());
        assertTrue(results.get(0).error().contains("Flight search failed"));
        assertEquals(StepStatus.SUCCESS, results.get(1).status());
    }

    // ---- 7. Dependency failure causes downstream skip ----

    @Test
    @DisplayName("should skip dependent step when dependency fails")
    void shouldSkipOnDependencyFailure() {
        var executors = Map.<String, ToolExecutor>of(
                "flight.search", args -> { throw new RuntimeException("Search error"); },
                "weather.forecast", args -> "done"
        );
        service = new ExecutionEngineService(toolRegistry, executors);

        // 1 (fails) -> 2 (should be skipped)
        var steps = List.of(
                step("1", "flight.search", Map.of(), List.of()),
                step("2", "weather.forecast", Map.of(), List.of("1"))
        );
        var results = service.execute(new ExecutionPlan(steps));

        assertEquals(2, results.size());
        assertEquals(StepStatus.FAILED, results.get(0).status());
        assertEquals(StepStatus.FAILED, results.get(1).status());
        assertTrue(results.get(1).error().contains("Skipped because dependency '1' failed"));
        assertEquals(0, results.get(1).latencyMs());
    }

    @Test
    @DisplayName("should skip transitive dependents when dependency fails")
    void shouldSkipTransitiveDependents() {
        var executors = Map.<String, ToolExecutor>of(
                "flight.search", args -> { throw new RuntimeException("Error"); },
                "weather.forecast", args -> "done"
        );
        service = new ExecutionEngineService(toolRegistry, executors);

        // 1 (fails) -> 2 -> 3
        var steps = List.of(
                step("1", "flight.search", Map.of(), List.of()),
                step("2", "weather.forecast", Map.of(), List.of("1")),
                step("3", "weather.forecast", Map.of(), List.of("2"))
        );
        var results = service.execute(new ExecutionPlan(steps));

        assertAll(
                () -> assertEquals(StepStatus.FAILED, results.get(0).status()),
                () -> assertTrue(results.get(1).error().contains("Skipped because dependency '1' failed")),
                () -> assertTrue(results.get(2).error().contains("Skipped because dependency '2' failed"))
        );
    }

    // ---- 8. Latency populated ----

    @Test
    @DisplayName("should populate latency for executed steps")
    void shouldPopulateLatency() {
        var executors = Map.<String, ToolExecutor>of(
                "flight.search", args -> {
                    Thread.sleep(50);
                    return "done";
                }
        );
        service = new ExecutionEngineService(toolRegistry, executors);

        var steps = List.of(step("1", "flight.search", Map.of(), List.of()));
        var results = service.execute(new ExecutionPlan(steps));

        assertEquals(StepStatus.SUCCESS, results.getFirst().status());
        assertTrue(results.getFirst().latencyMs() >= 50, "Latency should be at least 50ms");
    }

    @Test
    @DisplayName("should report zero latency for skipped steps")
    void shouldReportZeroLatencyForSkipped() {
        var executors = Map.<String, ToolExecutor>of(
                "flight.search", args -> { throw new RuntimeException("fail"); },
                "weather.forecast", args -> "done"
        );
        service = new ExecutionEngineService(toolRegistry, executors);

        var steps = List.of(
                step("1", "flight.search", Map.of(), List.of()),
                step("2", "weather.forecast", Map.of(), List.of("1"))
        );
        var results = service.execute(new ExecutionPlan(steps));

        assertEquals(0, results.get(1).latencyMs());
    }

    // ---- 9. Execution order respects dependencies ----

    @Test
    @DisplayName("should produce results in original plan order")
    void shouldReturnResultsInOriginalOrder() {
        var executors = Map.<String, ToolExecutor>of(
                "flight.search", args -> {
                    Thread.sleep(30);
                    return "done";
                },
                "weather.forecast", args -> "done"
        );
        service = new ExecutionEngineService(toolRegistry, executors);

        // Reverse order of declaration but 2 depends on 1
        var steps = List.of(
                step("2", "weather.forecast", Map.of(), List.of("1")),
                step("1", "flight.search", Map.of(), List.of())
        );
        var results = service.execute(new ExecutionPlan(steps));

        // Results returned in original order: 2, 1
        assertEquals("2", results.get(0).stepId());
        assertEquals("1", results.get(1).stepId());
        // But step 1 must have executed before step 2
        assertEquals(StepStatus.SUCCESS, results.get(0).status());
        assertEquals(StepStatus.SUCCESS, results.get(1).status());
    }

    // ---- 10. Large DAG execution ----

    @Test
    @DisplayName("should execute a large DAG without issues")
    void shouldExecuteLargeDag() {
        var counter = new AtomicInteger(0);
        ToolExecutor exec = args -> {
            counter.incrementAndGet();
            return "done";
        };

        var executors = Map.<String, ToolExecutor>of("flight.search", exec);
        service = new ExecutionEngineService(toolRegistry, executors);

        // Linear chain of 100 steps: 1 -> 2 -> 3 -> ... -> 100
        var steps = new ArrayList<ExecutionStep>();
        for (int i = 1; i <= 100; i++) {
            var deps = i == 1 ? List.<String>of() : List.of(String.valueOf(i - 1));
            steps.add(step(String.valueOf(i), "flight.search", Map.of(), deps));
        }
        var results = service.execute(new ExecutionPlan(steps));

        assertEquals(100, results.size());
        results.forEach(r -> assertEquals(StepStatus.SUCCESS, r.status()));
        assertEquals(100, counter.get());
    }

    // ---- Additional edge cases ----

    @Test
    @DisplayName("should fail step when executor is missing")
    void shouldFailOnMissingExecutor() {
        // tool exists in registry but no executor registered
        var executors = Map.<String, ToolExecutor>of();
        service = new ExecutionEngineService(toolRegistry, executors);

        var steps = List.of(step("1", "flight.search", Map.of(), List.of()));
        var results = service.execute(new ExecutionPlan(steps));

        assertEquals(StepStatus.FAILED, results.getFirst().status());
        assertTrue(results.getFirst().error().contains("No executor registered"));
    }

    @Test
    @DisplayName("should handle unknown tool gracefully")
    void shouldHandleUnknownTool() {
        var executors = Map.<String, ToolExecutor>of();
        service = new ExecutionEngineService(toolRegistry, executors);

        var steps = List.of(step("1", "nonexistent.tool", Map.of(), List.of()));
        var results = service.execute(new ExecutionPlan(steps));

        assertEquals(StepStatus.FAILED, results.getFirst().status());
        assertTrue(results.getFirst().error().contains("Unknown tool"));
    }

    @Test
    @DisplayName("should execute multiple steps that depend on the same root")
    void shouldHandleFanOut() {
        var order = Collections.synchronizedList(new ArrayList<String>());

        var executors = Map.<String, ToolExecutor>of(
                "flight.search", args -> {
                    order.add(args.get("id").toString());
                    return "done";
                }
        );
        service = new ExecutionEngineService(toolRegistry, executors);

        //     1
        //   / | \
        //  2  3  4
        var steps = List.of(
                step("1", "flight.search", Map.of("id", "1"), List.of()),
                step("2", "flight.search", Map.of("id", "2"), List.of("1")),
                step("3", "flight.search", Map.of("id", "3"), List.of("1")),
                step("4", "flight.search", Map.of("id", "4"), List.of("1"))
        );
        var results = service.execute(new ExecutionPlan(steps));

        assertEquals(4, results.size());
        results.forEach(r -> assertEquals(StepStatus.SUCCESS, r.status()));
        assertEquals("1", order.get(0)); // root executes first
        assertEquals(4, order.size());
    }

    @Test
    @DisplayName("should handle diamond DAG where two branches converge")
    void shouldHandleDiamondDag() {
        var order = Collections.synchronizedList(new ArrayList<String>());

        var executors = Map.<String, ToolExecutor>of(
                "flight.search", args -> {
                    order.add(args.get("id").toString());
                    return "done";
                }
        );
        service = new ExecutionEngineService(toolRegistry, executors);

        //   1
        //  / \
        // 2   3
        //  \ /
        //   4
        var steps = List.of(
                step("1", "flight.search", Map.of("id", "1"), List.of()),
                step("2", "flight.search", Map.of("id", "2"), List.of("1")),
                step("3", "flight.search", Map.of("id", "3"), List.of("1")),
                step("4", "flight.search", Map.of("id", "4"), List.of("2", "3"))
        );
        var results = service.execute(new ExecutionPlan(steps));

        assertEquals(4, results.size());
        results.forEach(r -> assertEquals(StepStatus.SUCCESS, r.status()));
        assertEquals("1", order.get(0));
        assertEquals("4", order.get(3));
    }

    @Test
    @DisplayName("should execute two independent chains concurrently")
    void shouldExecuteIndependentChainsConcurrently() throws Exception {
        var phaser = new Phaser(3); // main + 2 chains

        var executors = Map.<String, ToolExecutor>of(
                "flight.search", args -> {
                    phaser.arriveAndAwaitAdvance();
                    return "done";
                }
        );
        service = new ExecutionEngineService(toolRegistry, executors);

        // 1 -> 2    and    3 -> 4   (independent chains)
        var steps = List.of(
                step("1", "flight.search", Map.of(), List.of()),
                step("2", "flight.search", Map.of(), List.of("1")),
                step("3", "flight.search", Map.of(), List.of()),
                step("4", "flight.search", Map.of(), List.of("3"))
        );

        var future = java.util.concurrent.CompletableFuture.supplyAsync(
                () -> service.execute(new ExecutionPlan(steps)));
        phaser.arriveAndAwaitAdvance(); // wait for both roots to reach phaser
        // Both roots (1 and 3) should have started before we reach here
        phaser.arriveAndDeregister();
        var results = future.get();
        assertEquals(4, results.size());
        results.forEach(r -> assertEquals(StepStatus.SUCCESS, r.status()));
    }

    @Test
    @DisplayName("should capture error message on step failure")
    void shouldCaptureErrorMessage() {
        var executors = Map.<String, ToolExecutor>of(
                "flight.search", args -> { throw new RuntimeException("Connection timeout"); }
        );
        service = new ExecutionEngineService(toolRegistry, executors);

        var steps = List.of(step("1", "flight.search", Map.of(), List.of()));
        var results = service.execute(new ExecutionPlan(steps));

        assertEquals(StepStatus.FAILED, results.getFirst().status());
        assertEquals("Connection timeout", results.getFirst().error());
    }

    @Test
    @DisplayName("should return data on successful execution")
    void shouldReturnData() {
        var executors = Map.<String, ToolExecutor>of(
                "flight.search", args -> Map.of("flights", List.of("AA123"))
        );
        service = new ExecutionEngineService(toolRegistry, executors);

        var steps = List.of(step("1", "flight.search", Map.of(), List.of()));
        var results = service.execute(new ExecutionPlan(steps));

        assertNotNull(results.getFirst().data());
        assertEquals(List.of("AA123"), ((Map<String, List<String>>) results.getFirst().data()).get("flights"));
    }
}
