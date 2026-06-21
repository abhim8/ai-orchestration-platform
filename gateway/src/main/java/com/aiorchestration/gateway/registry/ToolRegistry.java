package com.aiorchestration.gateway.registry;

import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Maintains the set of available tool definitions and provides
 * look-up capabilities for the planner and execution engine.
 */
@Slf4j
@Component
public class ToolRegistry {

    public Set<String> getToolNames() {
        return Set.of();
    }

    public boolean hasTool(final String name) {
        return false;
    }
}
