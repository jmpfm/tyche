# Tyche

Tyche is a trading and investing platform built with Spring Boot. The project is intended to support market data ingestion, investment analysis, trading workflows, and portfolio-oriented application features.

## Project Status

This repository is currently in its initial application scaffold phase. The Maven configuration is in place, but domain models, services, controllers, and user-facing workflows are still to be implemented.

## Tech Stack

- Java 25
- Spring Boot 4
- Spring Web MVC
- Thymeleaf
- Spring Data JPA and JDBC
- MySQL
- Apache Kafka and Kafka Streams
- TA4J for technical analysis
- Spring Boot Actuator
- H2 in MySQL compatibility mode for local in-memory development and tests
- Maven Wrapper

## Getting Started

### Prerequisites

- Java 25
- Kafka

MySQL is only required when running with the `mysql` Spring profile. By default,
the app uses an in-memory H2 database configured in MySQL compatibility mode.

The project includes Maven Wrapper scripts, so a separate Maven installation is not required.

### Build

```bash
./mvnw clean package
```

### Run

```bash
./mvnw spring-boot:run
```

The application name is configured as `tyche` in `src/main/resources/application.yaml`.

To run against MySQL instead of the in-memory database:

```bash
SPRING_PROFILES_ACTIVE=mysql \
MYSQL_URL=jdbc:mysql://localhost:3306/tyche \
MYSQL_USERNAME=tyche \
MYSQL_PASSWORD=secret \
./mvnw spring-boot:run
```

### Test

```bash
./mvnw test
```

## Configuration

Application configuration lives in:

```text
src/main/resources/application.yaml
```

As the platform develops, environment-specific settings such as database credentials, Kafka broker URLs, market data provider keys, and trading API credentials should be supplied through environment variables or externalized configuration.

## Planned Capabilities

- Market data ingestion and processing
- Technical analysis and strategy evaluation
- Portfolio and watchlist management
- Trade execution workflows
- Account, risk, and performance views
- Operational health and metrics

## Development Notes

- Keep financial credentials and API keys out of source control.
- Treat trading-related features as high-risk workflows: add tests around calculations, order handling, and data ingestion boundaries.
- Prefer deterministic fixtures for strategy and indicator tests.

## Disclaimer

Tyche is software for trading and investing workflows. It does not provide financial advice. Any trading or investment decisions made using this software are the responsibility of the user.
