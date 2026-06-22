# Gateway

The central entry point for the AI Orchestration Platform. Accepts natural language requests and coordinates AI planning, validation, execution, and response aggregation.

## Responsibilities

- Expose `POST /api/v1/chat` as the sole public endpoint
- Manage conversation context and chat memory
- Generate execution plans via Google Gemini (AI mode) or keyword matching (deterministic fallback)
- Validate plans for confidence, tool existence, argument completeness, and DAG correctness
- Execute plans by calling downstream services in dependency order
- Aggregate step results into a structured response

## Planning Flow

```
User message
    → IntentPlannerService.plan()       # Call Gemini with planning prompt
    → ExecutionPlanValidator.validate()  # Check confidence, tools, arguments, cycles
    → ExecutionEngineService.execute()   # Topological sort + parallel execution
    → ResponseAggregatorService.aggregate() # Combine results
```

## Key Components

| Component | Description |
|-----------|-------------|
| `ChatController` | REST endpoint, resolves conversation ID, delegates to `ChatService` |
| `ChatService` | Orchestrates the plan → validate → execute → aggregate pipeline |
| `IntentPlannerService` | Calls Gemini with planning prompt, parses JSON response via `BeanOutputConverter` |
| `PromptProvider` | Builds the Gemini system prompt with tool definitions and planning rules |
| `DeterministicExecutionPlanFactory` | Fallback planner when AI is disabled; keyword-based plan generation |
| `ExecutionPlanValidator` | 8 validation rules including confidence threshold, tool existence, cycle detection |
| `ExecutionEngineService` | Topological sort (Kahn's), concurrent execution via `CompletableFuture` |
| `ToolExecutor` | Functional interface for step execution |
| `ToolRegistry` | Maps tool names to required argument sets |
| `FlightClient` | HTTP client for flight-service |
| `WeatherClient` | HTTP client for weather-service |
| `ResponseAggregatorService` | Builds `ChatResponse` from plan summary and step results |

## Spring AI Integration

- `ChatClient` configured with `MessageChatMemoryAdvisor` for multi-turn conversation context
- `ResolveRelativeDateTool` registered as a Gemini `@Tool` for resolving relative dates during planning
- `BeanOutputConverter<PlanGenerationResult>` for structured JSON parsing
- `MessageWindowChatMemory` with TTL-based eviction wrapper

## Planner Feature Flag

The AI planner can be disabled via:

```yaml
ai:
  planner:
    enabled: false   # default: true
```

When disabled, `DeterministicExecutionPlanFactory` handles requests using keyword matching (detects "flight" and "weather" keywords) and returns hardcoded plans.
