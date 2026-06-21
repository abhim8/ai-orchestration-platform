# Architecture Decisions

## AI Responsibility

Google Gemini (through Spring AI) is responsible **only** for:

- Understanding natural language
- Extracting structured parameters
- Resolving relative dates through Spring AI Tool Calling
- Producing a `PlanGenerationResult`
- Producing a natural-language summary

Gemini is **not** responsible for:

- Business logic
- External API calls
- Workflow execution
- Failure handling
- Retries
- Aggregation

All execution must remain deterministic Java code.

## Planning vs Execution

Planning and execution are intentionally separated.

```
User
    │
    ▼
Spring AI + Gemini
    │
    ▼
PlanGenerationResult
    │
    ▼
ExecutionPlanValidator
    │
    ▼
ExecutionEngineService
    │
    ▼
ResponseAggregatorService
```

The `ExecutionEngineService` must never depend on Spring AI.

## Execution Model

Execution order is derived exclusively from:

```
ExecutionStep.dependsOn
```

There is intentionally **no** `executionMode` field.

- Independent steps may execute in parallel.
- Dependent steps execute only after their prerequisites complete.

## Validation Strategy

```
ExecutionPlanValidator
```

throws

```
PlanValidationException
```

instead of returning a boolean.

Invalid execution plans must fail fast before execution begins.

## Clarification Strategy

If planner confidence falls below the configured threshold:

- Do not execute the plan
- Do not call downstream services
- Return a clarification response

The API communicates this using:

- `clarificationRequired`
- `clarificationMessage`

rather than HTTP 400.

## Layering

The gateway follows simple layered architecture:

```
Controller
    ↓
Service
    ↓
Planner / Registry / Client
    ↓
Model
```

Hexagonal Architecture, CQRS, Event Sourcing, and unnecessary abstractions are intentionally not used in v1.

## Spring AI Usage

Spring AI is intentionally isolated to the `planner` package.

The project demonstrates:

- ChatClient
- PromptTemplate
- BeanOutputConverter
- Tool Calling
- MessageChatMemoryAdvisor

Business logic should never depend directly on Spring AI.

## Communication

The gateway communicates with:

- flight-service
- weather-service

using REST.

Domain services do not contain LLM logic.

## Coding Standards

- Constructor injection only
- Lombok `@Slf4j` for logging
- No field injection
- No `System.out.println`
- Prefer immutable records for DTOs
- Validate external input using Jakarta Validation
- Run `mvn clean compile` after every implementation step

## Future Evolution

Possible enhancements without affecting the current architecture:

- Redis-backed chat memory
- Additional domain services
- Real external API integrations
- Observability and metrics
- Streaming responses
- Persistent execution history
