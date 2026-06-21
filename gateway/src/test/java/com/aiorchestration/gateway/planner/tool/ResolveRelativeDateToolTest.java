package com.aiorchestration.gateway.planner.tool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ResolveRelativeDateTool}.
 * Uses a fixed "today" date (2026-06-21, Sunday) for deterministic results.
 */
class ResolveRelativeDateToolTest {

    private static final LocalDate FIXED_TODAY = LocalDate.of(2026, 6, 21);

    private ResolveRelativeDateTool tool;

    @BeforeEach
    void setUp() {
        tool = new ResolveRelativeDateTool(FIXED_TODAY);
    }

    @Test
    @DisplayName("should resolve 'today' to the fixed date")
    void shouldResolveToday() {
        var result = tool.resolveRelativeDate("today");
        assertEquals("2026-06-21", result);
    }

    @Test
    @DisplayName("should resolve 'tomorrow' to the next day")
    void shouldResolveTomorrow() {
        var result = tool.resolveRelativeDate("tomorrow");
        assertEquals("2026-06-22", result);
    }

    @Test
    @DisplayName("should resolve 'yesterday' to the previous day")
    void shouldResolveYesterday() {
        var result = tool.resolveRelativeDate("yesterday");
        assertEquals("2026-06-20", result);
    }

    @Test
    @DisplayName("should resolve 'next week' to 7 days from today")
    void shouldResolveNextWeek() {
        var result = tool.resolveRelativeDate("next week");
        assertEquals("2026-06-28", result);
    }

    @ParameterizedTest
    @CsvSource({
        "next monday,    2026-06-22",
        "next tuesday,   2026-06-23",
        "next wednesday, 2026-06-24",
        "next thursday,  2026-06-25",
        "next friday,    2026-06-26",
        "next saturday,  2026-06-27",
        "next sunday,    2026-06-28"
    })
    @DisplayName("should resolve 'next X' to the correct date")
    void shouldResolveNextDayOfWeek(String expression, String expected) {
        var result = tool.resolveRelativeDate(expression);
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for unrecognized expressions")
    void shouldThrowOnInvalidExpression() {
        var exception = assertThrows(IllegalArgumentException.class,
                () -> tool.resolveRelativeDate("invalid"));
        assertTrue(exception.getMessage().contains("invalid"));
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for empty input")
    void shouldThrowOnEmptyInput() {
        var exception = assertThrows(IllegalArgumentException.class,
                () -> tool.resolveRelativeDate(""));
        assertTrue(exception.getMessage().contains(""));
    }

    @Test
    @DisplayName("should be case-insensitive")
    void shouldBeCaseInsensitive() {
        assertEquals("2026-06-22", tool.resolveRelativeDate("TOMORROW"));
        assertEquals("2026-06-22", tool.resolveRelativeDate("Tomorrow"));
        assertEquals("2026-06-22", tool.resolveRelativeDate("ToMoRrOw"));
    }

    @Test
    @DisplayName("should trim whitespace from input")
    void shouldTrimWhitespace() {
        assertEquals("2026-06-22", tool.resolveRelativeDate("  tomorrow  "));
        assertEquals("2026-06-26", tool.resolveRelativeDate("  next Friday  "));
    }
}
