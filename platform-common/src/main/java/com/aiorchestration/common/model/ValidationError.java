package com.aiorchestration.common.model;

public record ValidationError(String field, String message, Object rejectedValue) {
}
