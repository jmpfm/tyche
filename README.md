# Tyche

Tyche is a trading and investing platform built with Spring Boot. The project is intended to support market data ingestion, investment analysis, trading workflows, and portfolio-oriented application features.

## Project Status

This repository is currently in its initial application scaffold phase. The Maven configuration is in place, but domain models, services, controllers, and user-facing workflows are still to be implemented.

## Tech Stack

- Java 24
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

- Java 24
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

The assets dashboard reads current holdings from the `portfolio_positions`
table. The default H2 profile creates and seeds this table from
`src/main/resources/schema.sql` and `src/main/resources/data.sql`. When running
with the `mysql` profile, create the same table in the configured MySQL database
and populate these columns:

```text
symbol, name, asset_class, quantity, average_cost, market_price, day_change_percent, display_order
```

As the platform develops, environment-specific settings such as database credentials, Kafka broker URLs, market data provider keys, and trading API credentials should be supplied through environment variables or externalized configuration.

### Trade Recommendations

Tyche can generate advisory-only trade recommendations from the current portfolio,
technical indicators, open-source news and macro signals, and SEC fundamentals
when a SEC user agent is configured. Recommendations are published as JSON to
Kafka topic `tyche.trade-recommendations.v1` by default.

Manual endpoints:

```text
POST /api/recommendations/generate
GET /api/recommendations/backtest
```

Useful environment variables:

```text
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
TYCHE_RECOMMENDATIONS_TOPIC=tyche.trade-recommendations.v1
TYCHE_RECOMMENDATIONS_KAFKA_ENABLED=true
TYCHE_RECOMMENDATIONS_SCHEDULE_ENABLED=true
TYCHE_RECOMMENDATIONS_CRON="0 30 22 * * MON-FRI"
SEC_USER_AGENT="Tyche contact@example.com"
TYCHE_GDELT_DOC_URL=https://api.gdeltproject.org/api/v2/doc/doc
TYCHE_SEC_TICKERS_URL=https://www.sec.gov/files/company_tickers.json
TYCHE_SEC_COMPANYFACTS_BASE_URL=https://data.sec.gov/api/xbrl/companyfacts
```

If GDELT or SEC calls fail, the engine degrades those factors to neutral and
still records the available portfolio and technical-analysis context.

### Local Mock API (WireMock)

For local development without external API calls, this repo includes WireMock
stubs for:

- `GET /api/v2/doc/doc` (GDELT-style articles)
- `GET /files/company_tickers.json` (SEC ticker map)
- `GET /api/xbrl/companyfacts/CIK0000320193.json` (SEC company facts sample)

Start WireMock:

```bash
docker compose -f docker-compose.mock.yml up -d
```

Run Tyche against the local mock API:

```bash
SPRING_PROFILES_ACTIVE=local \
./mvnw spring-boot:run
```

The `local` profile keeps H2 as the database, disables scheduled recommendation
runs and Kafka publishing, and points GDELT/SEC lookups at the local WireMock
instance on port `8089`.

Stop WireMock:

```bash
docker compose -f docker-compose.mock.yml down
```

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
