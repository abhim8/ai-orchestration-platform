package com.aiorchestration.gateway.registry;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Maintains the set of available tool definitions and their required
 * arguments, providing lookup capabilities for the planner and
 * execution engine.
 *
 * <p>This is the single source of truth for which tools exist and what
 * arguments each tool requires. Adding a new tool means adding an entry
 * here.
 */
@Component
public class ToolRegistry {

    private static final Map<String, Set<String>> DEFAULT_TOOLS;

    static {
        var map = new HashMap<String, Set<String>>();
        map.put("flight.search", orderedSet("origin", "destination", "departureDate"));
        map.put("weather.forecast", orderedSet("location", "date"));
        DEFAULT_TOOLS = Collections.unmodifiableMap(map);
    }

    private static Set<String> orderedSet(final String... elements) {
        var set = new LinkedHashSet<String>();
        Collections.addAll(set, elements);
        return Collections.unmodifiableSet(set);
    }

    private final Map<String, Set<String>> tools;

    public ToolRegistry() {
        this.tools = DEFAULT_TOOLS;
    }

    public ToolRegistry(final Map<String, Set<String>> tools) {
        this.tools = tools != null
            ? Collections.unmodifiableMap(new HashMap<>(tools))
            : DEFAULT_TOOLS;
    }

    public Set<String> getToolNames() {
        return tools.keySet();
    }

    public boolean hasTool(final String name) {
        return tools.containsKey(name);
    }

    public Set<String> getRequiredArguments(final String toolName) {
        if (!hasTool(toolName)) {
            return Set.of();
        }
        return tools.get(toolName);
    }
}
