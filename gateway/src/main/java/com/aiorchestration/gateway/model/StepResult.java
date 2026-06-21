package com.aiorchestration.gateway.model;

/**
 * The result of executing a single step.
 *
 * @param stepId    the identifier of the step that was executed
 * @param tool      the tool that executed the step
 * @param status    whether the step succeeded or failed
 * @param data      the data returned by the tool on success
 * @param error     error message if the step failed
 * @param latencyMs execution duration in milliseconds
 */
public record StepResult(
    String stepId,
    String tool,
    StepStatus status,
    Object data,
    String error,
    long latencyMs
) {}
