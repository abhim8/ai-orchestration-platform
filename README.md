![Java](https://img.shields.io/badge/Java-23-orange)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.7-brightgreen)
![Spring AI](https://img.shields.io/badge/Spring_AI-2.0.0-blue)
![Google Gemini](https://img.shields.io/badge/Google_Gemini-gemini--2.5--flash--lite-purple)
![License](https://img.shields.io/badge/License-Apache_2.0-green)
![Build](https://github.com/abhim8/ai-orchestration-platform/actions/workflows/build.yml/badge.svg)
![Maven](https://img.shields.io/badge/Maven-3.9+-C71A36)
![OpenAPI](https://img.shields.io/badge/OpenAPI-3.0-85EA2D)
![Virtual Threads](https://img.shields.io/badge/Virtual_Threads-Enabled-blueviolet)

# AI Orchestration Platform

Natural language interface for multi-step tool orchestration. Accepts plain-text requests, plans workflows using Google Gemini, executes them across downstream services, and aggregates results.

## Project Overview

The AI Orchestration Platform translates natural language into structured execution plans. A user types "Book a flight from Bangalore to Tokyo tomorrow and check the weather" - the platform resolves relative dates, generates a validated dependency DAG, executes independent steps in parallel, and returns a consolidated response.

**Responsibilities**

- Expose `POST /api/v1/chat` as the sole public endpoint
- Generate execution plans from natural language via Google Gemini or keyword-based fallback
- Validate plans for confidence, tool existence, argument completeness, and graph correctness
- Execute tool steps in dependency order with parallel execution of independent branches
- Aggregate step results into a structured response
- Maintain multi-turn conversation memory with TTL-based eviction
- Propagate trace context (`X-Trace-Id`) across all services

**Deliberately out of scope**

- Authentication and authorization - no user identity layer is implemented
- Streaming responses - all responses are synchronous JSON
- Persistent storage - conversation state is in-memory and lost on restart
- Third-party flight provider integration - flight data is deterministic mock data by design

## Key Features

- **AI planning** - Google Gemini generates execution plans from natural language
- **Deterministic fallback** - keyword-based planning when the AI planner is disabled
- **Tool orchestration** - flight search and weather forecast tools with extensible registry
- **Conversation memory** - multi-turn chat with `MessageWindowChatMemory` and TTL-based eviction
- **Relative date resolution** - Gemini resolves "today", "tomorrow", "next Friday" during planning
- **Execution DAG** - steps declare dependencies; the engine topologically sorts and respects the graph
- **Parallel execution** - independent steps run concurrently via `CompletableFuture` and `ThreadPoolTaskExecutor`
- **Plan validation** - 8 validation rules: confidence threshold, tool existence, argument completeness, cycle detection, and more
- **Structured logging** - Log4j2 JSON layout with trace and conversation IDs
- **Virtual threads** - Spring Boot virtual threads enabled on the gateway
- **Trace propagation** - `X-Trace-Id` header propagated across all services
- **Graceful shutdown** - all services drain in-flight requests before stopping

## Architecture

```mermaid
graph TB
    Client["Client"]

    subgraph Gateway[":8005"]
        Controller["ChatController"]
        Planner["IntentPlannerService"]
        Fallback["DeterministicExecutionPlanFactory"]
        Validator["ExecutionPlanValidator"]
        Engine["ExecutionEngineService"]
        Registry["ToolRegistry"]
        Aggregator["ResponseAggregatorService"]
        Memory["MessageWindowChatMemory"]
    end

    subgraph AI[" "]
        Gemini["Google Gemini<br/>gemini-2.5-flash-lite"]
    end

    subgraph Services[" "]
        Flight["Flight Service :8006"]
        Weather["Weather Service :8007"]
    end

    subgraph Data[" "]
        OpenMeteo["Open-Meteo API"]
        MockData["Mock Data"]
    end

    Client -->|POST /api/v1/chat| Controller
    Controller --> Memory
    Controller --> Planner
    Planner -->|AI enabled| Gemini
    Planner -->|AI disabled| Fallback
    Planner -->|plan| Validator
    Validator -->|valid| Engine
    Registry --> Engine
    Engine -->|flight.search| Flight
    Engine -->|weather.forecast| Weather
    Flight -->|mocked| MockData
    Weather --> OpenMeteo
    Engine --> Aggregator
    Aggregator --> Controller
    Controller --> Client
```

The gateway owns all orchestration logic. Downstream services are thin - they validate inputs and return data. The gateway never duplicates service-level validation or data access.

## Request Lifecycle

```mermaid
sequenceDiagram
    actor Client
    participant Gateway
    participant Planner as IntentPlannerService
    participant Gemini as Google Gemini
    participant Validator as PlanValidator
    participant Engine as ExecutionEngine
    participant Flight as Flight Service
    participant Weather as Weather Service
    participant OM as Open-Meteo

    Client->>Gateway: POST /api/v1/chat {"message":"..."}
    activate Gateway

    Gateway->>Gateway: 1. Resolve conversation ID
    Gateway->>Gateway: 2. Load chat memory

    Gateway->>Planner: 3. plan(conversationId, message)
    alt AI enabled
        Planner->>Gemini: ChatClient.prompt() with tools + relative date resolver
        Gemini-->>Planner: PlanGenerationResult (structured JSON)
    else AI disabled
        Planner->>Planner: Keyword-based fallback plan
    end
    Planner-->>Gateway: ExecutionPlan

    Gateway->>Validator: 4. validate(plan)
    alt Invalid plan
        Gateway-->>Client: PlanValidationException (400)
    end
    Validator-->>Gateway: valid

    Gateway->>Engine: 5. execute(executionPlan)
    Engine->>Engine: 5a. Topological sort (Kahn's algorithm)
    Engine->>Engine: 5b. Build dependency DAG

    par flight.search
        Engine->>Flight: GET /api/v1/flights/search
        Flight-->>Engine: FlightSearchResponse
    and weather.forecast
        Engine->>Weather: GET /api/v1/weather/forecast
        Weather->>OM: Geocoding API + Forecast API
        OM-->>Weather: forecast data
        Weather-->>Engine: WeatherForecastResponse
    end

    Engine-->>Gateway: 6. List of StepResult
    Gateway->>Gateway: 7. ResponseAggregatorService.aggregate()
    Gateway-->>Client: ChatResponse (JSON)
    deactivate Gateway
```

| Phase | Action | Error |
|-------|--------|-------|
| 1 | Resolve or create `X-Conversation-Id` | - |
| 2 | Load chat history from `MessageWindowChatMemory` | - |
| 3 | Call planner: Gemini or deterministic fallback | - |
| 3a | Gemini resolves relative dates via `ResolveRelativeDateTool` | - |
| 3b | Gemini returns structured `PlanGenerationResult` via `BeanOutputConverter` | - |
| 4 | Validate: confidence, tool existence, arguments, cycles | `PlanValidationException` (400) |
| 5 | Topological sort (Kahn's) + dependency DAG | - |
| 5a | Execute independent steps concurrently | - |
| 6 | Collect `StepResult` from each tool | - |
| 7 | Aggregate results into `ChatResponse` with per-step status and latency | - |

## Design Principles

Each concern is owned by exactly one component:

- **Planner** generates an execution plan from natural language. It does not execute tools, validate arguments, or aggregate results.
- **Validator** checks plan structure, confidence, tool existence, argument completeness, and dependency graph correctness. It does not modify the plan.
- **Execution Engine** topologically sorts steps and runs them via `CompletableFuture`, respecting the dependency DAG. It does not interpret step results.
- **Downstream services** own their business logic and data sources. The gateway never duplicates service-level validation or data access.
- **Aggregator** combines step results into the final response. It does not re-execute or re-validate steps.

## Technology Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 23 |
| Framework | Spring Boot 4.0.7 |
| AI SDK | Spring AI 2.0.0 (Google GenAI starter) |
| LLM | Google Gemini (default: `gemini-2.5-flash-lite`) |
| Concurrency | `CompletableFuture` + `ThreadPoolTaskExecutor` + Virtual Threads (gateway) |
| HTTP Client | Spring `RestClient` backed by `java.net.http.HttpClient` |
| Validation | Jakarta Bean Validation |
| Logging | Log4j 2.x with JSON template layout |
| API Docs | SpringDoc OpenAPI 3.0.0 (disabled by default) |
| Build | Apache Maven 3.9+ (multi-module) |
| Monitoring | Spring Boot Actuator (health, info, metrics) |
| External APIs | Open-Meteo (free weather, no auth required) |

## Repository Structure

```
ai-orchestration-platform/
├── pom.xml                    # Parent POM (Spring Boot 4.0.7, Java 23)
├── platform-common/           # Shared library (error DTOs, trace filter)
├── gateway/                   # API gateway, planner, execution engine
├── flight-service/            # Flight search microservice
└── weather-service/           # Weather forecast microservice
```

| Module | Type | Port | Responsibility |
|--------|------|------|----------------|
| `platform-common` | JAR | - | Shared infrastructure: `TraceIdFilter`, `ErrorResponse` DTO, `ConversationContext` (MDC), `Headers` constants. No Spring Boot dependency - a lightweight library used by all services. |
| `gateway` | Spring Boot | 8005 | API entry point. Houses the chat controller, AI planner (`IntentPlannerService`), deterministic fallback, plan validator, execution engine, downstream HTTP clients (`FlightClient`, `WeatherClient`), response aggregator, chat memory, and all tool/planner configuration. |
| `flight-service` | Spring Boot | 8006 | Flight search microservice. Input validation, mocked `AmadeusClient`, structured error handling. Uses deterministic mock data - see note below. |
| `weather-service` | Spring Boot | 8007 | Weather forecast microservice. Real Open-Meteo integration via geocoding + forecast APIs, date constraint validation, WMO code mapping. |

The flight-service uses deterministic mock data by design. This project focuses on demonstrating AI orchestration, planning, and Spring AI integration rather than integrating with third-party flight providers. Real providers such as Amadeus can be integrated with minimal architectural changes - the `RestClient.Builder` bean is already wired and ready.

## Configuration

All services follow Spring Boot's standard precedence: environment variables override `application.yml`.

| Variable | Default | Description | Service |
|----------|---------|-------------|---------|
| `GEMINI_API_KEY` | - | Google Gemini API key (required) | gateway |
| `GEMINI_MODEL` | `gemini-2.5-flash-lite` | Gemini model name | gateway |
| `AI_PLANNER_ENABLED` | `true` | Enable AI planner; when `false`, uses keyword-based fallback | gateway |
| `SERVER_PORT` | `8005` | HTTP listen port | gateway |
| `FLIGHT_SERVICE_BASE_URL` | `http://localhost:8006` | Flight service base URL | gateway |
| `WEATHER_SERVICE_BASE_URL` | `http://localhost:8007` | Weather service base URL | gateway |
| `HTTP_CONNECT_TIMEOUT` | `10s` | Downstream HTTP connect timeout | gateway |
| `HTTP_READ_TIMEOUT` | `15s` | Downstream HTTP read timeout | gateway |
| `EXECUTION_POOL_CORE_SIZE` | `4` | Thread pool core size | gateway |
| `EXECUTION_POOL_MAX_SIZE` | `8` | Thread pool max size | gateway |
| `EXECUTION_POOL_QUEUE_CAPACITY` | `50` | Thread pool queue capacity | gateway |
| `CHAT_MEMORY_MAX_MESSAGES` | `30` | Max messages retained per conversation | gateway |
| `CHAT_MEMORY_TTL_MINUTES` | `30` | Conversation idle time-to-live (minutes) | gateway |
| `CHAT_MEMORY_CLEANUP_INTERVAL` | `300000` | Stale conversation cleanup interval (ms) | gateway |
| `PLANNING_CLARIFICATION_THRESHOLD` | `0.5` | Minimum confidence to execute plan | gateway |
| `PLANNING_MINIMUM_CONFIDENCE` | `0.5` | Minimum plan confidence threshold | gateway |
| `SERVER_PORT` | `8006` | HTTP listen port | flight-service |
| `HTTP_CONNECT_TIMEOUT` | `5s` | Downstream HTTP connect timeout | flight-service |
| `HTTP_READ_TIMEOUT` | `10s` | Downstream HTTP read timeout | flight-service |
| `SERVER_PORT` | `8007` | HTTP listen port | weather-service |
| `HTTP_CONNECT_TIMEOUT` | `10s` | Downstream HTTP connect timeout | weather-service |
| `HTTP_READ_TIMEOUT` | `15s` | Downstream HTTP read timeout | weather-service |
| `WEATHER_MAX_DAYS_AHEAD` | `16` | Max forecast days from today | weather-service |
| `OPEN_METEO_GEOCODING_URL` | `https://geocoding-api.open-meteo.com/v1/search` | Open-Meteo geocoding endpoint | weather-service |
| `OPEN_METEO_FORECAST_URL` | `https://api.open-meteo.com/v1/forecast` | Open-Meteo forecast endpoint | weather-service |

## Running Locally

**Prerequisites**

- JDK 23 (Temurin recommended)
- Apache Maven 3.9+
- Google Gemini API key

**Build**

```bash
export GEMINI_API_KEY=your-key-here
mvn clean install -DskipTests
```

Every push and pull request is automatically built via GitHub Actions - no pre-submit checks required beyond `mvn clean verify`.

**Run**

Start each service in a separate terminal. Flight and weather services can start in any order; the gateway requires both to be running.

```bash
# Terminal 1 - flight-service (port 8006)
mvn spring-boot:run -pl flight-service

# Terminal 2 - weather-service (port 8007)
mvn spring-boot:run -pl weather-service

# Terminal 3 - gateway (port 8005)
mvn spring-boot:run -pl gateway
```

| Service | Port | Base URL |
|---------|------|----------|
| Gateway | 8005 | `http://localhost:8005` |
| Flight Service | 8006 | `http://localhost:8006` |
| Weather Service | 8007 | `http://localhost:8007` |

**Run tests**

```bash
mvn clean verify
```

## API Example

```bash
curl -X POST http://localhost:8005/api/v1/chat \
  -H "Content-Type: application/json" \
  -H "X-Conversation-Id: my-session-1" \
  -d '{"message": "Book a flight from BLR to NRT tomorrow and check the weather in Tokyo"}'
```

### Response

```json
{
  "partialSuccess": false,
  "completedSteps": ["step-1", "step-2"],
  "failedSteps": [],
  "executionTrace": [
    {
      "stepId": "step-1",
      "tool": "flight.search",
      "status": "SUCCESS",
      "data": {
        "flights": [
          {
            "flightNumber": "AF123",
            "origin": "BLR",
            "destination": "NRT",
            "departureDate": "2026-06-24",
            "departureTime": "01:30",
            "status": "On Time"
          }
        ]
      },
      "error": null,
      "latencyMs": 120
    },
    {
      "stepId": "step-2",
      "tool": "weather.forecast",
      "status": "SUCCESS",
      "data": {
        "location": "Tokyo",
        "date": "2026-06-24",
        "temperatureCelsius": 22.5,
        "condition": "Partly Cloudy",
        "humidityPercent": 65,
        "windSpeedKph": 12.3
      },
      "error": null,
      "latencyMs": 340
    }
  ],
  "response": {
    "step-1": {
      "flights": [
        {
          "flightNumber": "AF123",
          "origin": "BLR",
          "destination": "NRT",
          "departureDate": "2026-06-24",
          "departureTime": "01:30",
          "status": "On Time"
        }
      ]
    },
    "step-2": {
      "location": "Tokyo",
      "date": "2026-06-24",
      "temperatureCelsius": 22.5,
      "condition": "Partly Cloudy",
      "humidityPercent": 65,
      "windSpeedKph": 12.3
    }
  },
  "summary": "Searched for flights from BLR to NRT on 2026-06-24 and retrieved the Tokyo weather forecast.",
  "clarificationRequired": false,
  "clarificationMessage": null
}
```

## Documentation

| Resource | Description |
|----------|-------------|
| [gateway/README.md](gateway/README.md) | Gateway architecture, planning flow, key components, feature flag |
| [flight-service/README.md](flight-service/README.md) | Flight service endpoint, mocked data, architecture |
| [weather-service/README.md](weather-service/README.md) | Weather service endpoint, Open-Meteo integration, validation |
| [CONTRIBUTING.md](CONTRIBUTING.md) | Setup guide, branch naming, commit conventions, PR workflow |
| [SECURITY.md](SECURITY.md) | Security policy and vulnerability reporting |
| [docs/postman/](docs/postman/) | Postman collection for API testing |

## Future Improvements

- Multi-modal input support
- Streaming responses via SSE
- Circuit breaker and retry for downstream services
- Persistent conversation storage

## License

[Apache License, Version 2.0](LICENSE)
