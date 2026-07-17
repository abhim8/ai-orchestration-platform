# Contributing

Thanks for your interest in contributing to the AI Orchestration Platform.

## Project Setup

### Prerequisites

- **JDK 23** (Temurin recommended)
- **Apache Maven 3.9+**
- **A Google Gemini API key** (for AI planning)
- **IntelliJ IDEA** (preferred, though any IDE works)

### Running Locally

Set your Gemini API key:

```bash
export GEMINI_API_KEY=your-api-key-here
```

Start each service in its own terminal:

```bash
# Terminal 1 - gateway (port 8005)
mvn spring-boot:run -pl gateway

# Terminal 2 - flight-service (port 8006)
mvn spring-boot:run -pl flight-service

# Terminal 3 - weather-service (port 8007)
mvn spring-boot:run -pl weather-service
```

The gateway is the only entry point. Send requests to `POST http://localhost:8005/api/v1/chat`.

### Building

```bash
mvn clean compile
```

### Running Tests

```bash
mvn clean verify
```

To run a specific module:

```bash
mvn clean test -pl gateway
```

## Branch Naming

Use a consistent prefix followed by a short description:

- `feat/` - new features
- `fix/` - bug fixes
- `chore/` - tooling, CI, or dependency updates
- `docs/` - documentation-only changes

Examples: `feat/add-hotel-search`, `fix/planner-date-resolution`.

## Commit Messages

Use [conventional commit](https://www.conventionalcommits.org/) style:

```
<type>(<scope>): <short description>

<optional body>
```

Examples:

- `feat(gateway): add hotel search tool support`
- `fix(weather-service): handle Open-Meteo rate limiting`
- `chore(ci): pin GitHub Actions runner versions`

Types: `feat`, `fix`, `chore`, `docs`, `refactor`, `test`.

Scopes: `gateway`, `flight-service`, `weather-service`, `common`, `ci`.

## Pull Request Workflow

1.  Create a feature/fix branch from `main`.
2.  Make your changes and commit them using conventional commits.
3.  Run `mvn clean verify` locally before pushing.
4.  Open a PR against `main` using the PR template.
5.  Ensure the CI build passes.
6.  Request a review from a maintainer.

## Coding Guidelines

- Follow the existing code style (package layout, naming conventions, record usage).
- Use `@Slf4j` for logging; avoid `System.out`.
- Validate inputs at the controller layer using Jakarta Bean Validation.
- Add unit tests for new services and logic.
- Do **not** commit secrets or hardcoded credentials.
- Use environment variables for all sensitive configuration.
