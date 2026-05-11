# AGENTS

## Goal

Tyche is a Spring Boot application for portfolio analysis and advisory trade recommendations. Keep changes small, deterministic, and focused on correctness in calculations and integration boundaries.

## Project Map

- `src/main/java/io/codepieces/tyche/assets`: portfolio model, repository, controller, and assets view
- `src/main/java/io/codepieces/tyche/analysis`: technical indicators
- `src/main/java/io/codepieces/tyche/intelligence`: GDELT news/macro and SEC fundamentals adapters
- `src/main/java/io/codepieces/tyche/recommendations`: signal composition, scoring, workflow, schedule, and Kafka publishing
- `src/main/resources/templates/assets`: Thymeleaf UI

## Commands

- Build: `./mvnw clean package`
- Test: `./mvnw test`
- Run app: `./mvnw spring-boot:run`
- Run with local mock profile: `SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run`
- Start/stop WireMock: `docker compose -f docker-compose.mock.yml up -d` / `docker compose -f docker-compose.mock.yml down`

## Runtime Profiles

- Default profile: H2 in-memory database, scheduled recommendations enabled, Kafka publishing enabled.
- `local` profile: keeps H2, disables scheduler and Kafka publishing, routes GDELT/SEC endpoints to local WireMock on `localhost:8089`.
- `mysql` profile: external MySQL datasource.

## Adapter Rules

- Prefer WireMock-backed behavior for GDELT/SEC development and tests.
- Recommendation generation must degrade to neutral signals when external adapters fail.
- Do not require real external API calls in tests.

## Testing Expectations

- Keep unit tests deterministic and fixture-driven.
- Add tests for changed business logic, especially:
  - recommendation scoring and thresholds
  - external adapter parsing/fallback behavior
  - publishing toggles and side effects
