package com.aiorchestration.gateway.service;

import java.util.Map;

/**
 * Executes a single tool invocation. Each registered tool (e.g.
 * {@code flight.search}, {@code weather.forecast}) has a corresponding
 * executor that performs the actual work.
 *
 * <p>This is a functional interface so executors can be provided as
 * lambdas or method references, and the executor registry is simply a
 * {@code Map<String, ToolExecutor>}.
 */
@FunctionalInterface
public interface ToolExecutor {

    /**
     * Execute the tool with the given arguments.
     *
     * @param arguments the arguments for this invocation (never null)
     * @return the result data produced by the tool
     * @throws Exception if execution fails
     */
    Object execute(Map<String, Object> arguments) throws Exception;
}
