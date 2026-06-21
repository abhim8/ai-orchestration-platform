package com.aiorchestration.common.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TraceIdFilterTest {

    private static final String VALID_TRACE_ID = "550e8400-e29b-41d4-a716-446655440000";

    private TraceIdFilter filter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new TraceIdFilter();
    }

    @Test
    @DisplayName("should use X-Trace-Id from request when present")
    void shouldUseExistingTraceId() throws Exception {
        when(request.getHeader("X-Trace-Id")).thenReturn(VALID_TRACE_ID);

        final String[] captured = new String[1];
        filter.doFilterInternal(request, response, (req, res) -> {
            captured[0] = MDC.get("traceId");
        });

        assertEquals(VALID_TRACE_ID, captured[0]);
        verify(response).setHeader("X-Trace-Id", VALID_TRACE_ID);
    }

    @Test
    @DisplayName("should generate UUID when X-Trace-Id is absent")
    void shouldGenerateTraceIdWhenMissing() throws Exception {
        when(request.getHeader("X-Trace-Id")).thenReturn(null);

        final String[] captured = new String[1];
        filter.doFilterInternal(request, response, (req, res) -> {
            captured[0] = MDC.get("traceId");
        });

        assertNotNull(captured[0]);
        verify(response).setHeader("X-Trace-Id", captured[0]);
    }

    @Test
    @DisplayName("should generate UUID when X-Trace-Id is blank")
    void shouldGenerateTraceIdWhenBlank() throws Exception {
        when(request.getHeader("X-Trace-Id")).thenReturn("   ");

        final String[] captured = new String[1];
        filter.doFilterInternal(request, response, (req, res) -> {
            captured[0] = MDC.get("traceId");
        });

        assertNotNull(captured[0]);
        verify(response).setHeader("X-Trace-Id", captured[0]);
    }

    @Test
    @DisplayName("should clear MDC after request completes")
    void shouldClearMdcAfterRequest() throws Exception {
        when(request.getHeader("X-Trace-Id")).thenReturn(VALID_TRACE_ID);

        filter.doFilterInternal(request, response, (req, res) -> {
            assertEquals(VALID_TRACE_ID, MDC.get("traceId"));
        });

        assertNull(MDC.get("traceId"));
    }

    @Test
    @DisplayName("should clear MDC even when chain throws exception")
    void shouldClearMdcOnException() {
        when(request.getHeader("X-Trace-Id")).thenReturn(VALID_TRACE_ID);

        try {
            filter.doFilterInternal(request, response, (req, res) -> {
                throw new RuntimeException("Chain failure");
            });
        } catch (Exception ignored) {
        }

        assertNull(MDC.get("traceId"));
    }

    @Test
    @DisplayName("should generate valid UUID format")
    void shouldGenerateValidUuid() throws Exception {
        when(request.getHeader("X-Trace-Id")).thenReturn(null);

        final String[] captured = new String[1];
        filter.doFilterInternal(request, response, (req, res) -> {
            captured[0] = MDC.get("traceId");
        });

        assertNotNull(captured[0]);
        assertEquals(36, captured[0].length());
    }
}
