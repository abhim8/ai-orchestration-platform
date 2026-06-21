package com.aiorchestration.gateway.planner.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * Spring AI Tool that resolves relative date expressions into ISO-8601 date
 * strings during the planning phase.
 *
 * <p>This tool exists exclusively to assist Gemini in understanding relative
 * date references (e.g. "tomorrow", "next Friday") when generating execution
 * plans. It performs deterministic date resolution only, using
 * {@link java.time} APIs. It does not execute business logic, call external
 * APIs, or perform any workflow execution.</p>
 *
 * <p>Designed for Spring AI {@link Tool @Tool} discovery. The method
 * {@link #resolveRelativeDate(String)} is automatically registered as a
 * callable tool when this component is picked up by Spring's component scan.</p>
 */
@Slf4j
@Component
public class ResolveRelativeDateTool {

    private final LocalDate today;

    /**
     * Default constructor using the system clock.
     */
    public ResolveRelativeDateTool() {
        this.today = LocalDate.now();
    }

    /**
     * Package-private constructor for deterministic testing.
     *
     * @param today the fixed date to use as "today"
     */
    ResolveRelativeDateTool(final LocalDate today) {
        this.today = today;
    }

    /**
     * Resolves a relative date expression into an ISO-8601 date string.
     *
     * <p>Supported expressions: today, tomorrow, yesterday, next Monday,
     * next Tuesday, next Wednesday, next Thursday, next Friday, next Saturday,
     * next Sunday, next week.</p>
     *
     * @param relativeDateExpression a relative date expression
     * @return the resolved date as an ISO-8601 string (e.g. "2026-06-26")
     * @throws IllegalArgumentException if the expression cannot be resolved
     */
    @Tool(name = "resolveRelativeDate", description = """
            Resolves relative date expressions into ISO-8601 date strings.
            Supported expressions: today, tomorrow, yesterday, next Monday,
            next Tuesday, next Wednesday, next Thursday, next Friday,
            next Saturday, next Sunday, next week.""")
    public String resolveRelativeDate(final String relativeDateExpression) {
        log.debug("Resolving relative date expression: {}", relativeDateExpression);

        var expr = relativeDateExpression.trim().toLowerCase();

        LocalDate resolved = switch (expr) {
            case "today" -> today;
            case "tomorrow" -> today.plusDays(1);
            case "yesterday" -> today.minusDays(1);
            case "next week" -> today.plusWeeks(1);
            case "next monday" -> today.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
            case "next tuesday" -> today.with(TemporalAdjusters.next(DayOfWeek.TUESDAY));
            case "next wednesday" -> today.with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY));
            case "next thursday" -> today.with(TemporalAdjusters.next(DayOfWeek.THURSDAY));
            case "next friday" -> today.with(TemporalAdjusters.next(DayOfWeek.FRIDAY));
            case "next saturday" -> today.with(TemporalAdjusters.next(DayOfWeek.SATURDAY));
            case "next sunday" -> today.with(TemporalAdjusters.next(DayOfWeek.SUNDAY));
            default ->
                throw new IllegalArgumentException(
                        "Unrecognized relative date expression: " + relativeDateExpression);
        };

        var isoDate = resolved.toString();
        log.debug("Resolved '{}' to {}", relativeDateExpression, isoDate);
        return isoDate;
    }
}
