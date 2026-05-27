# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

An e-commerce platform built with a microservices architecture, including a hybrid recommendation system. The system uses event-driven communication via Kafka and CDC (Change Data Capture) via Debezium to feed a ClickHouse analytics store.

## Services and Ports

| Service | Language | Port | Database |
|---|---|---|---|
| `user` | Java 17 / Spring Boot | 8080 | PostgreSQL `user_service` |
| `catalog` | Java 17 / Spring Boot | 8081 | PostgreSQL `catalog` |
| `shipment` | Kotlin / Spring Boot | 8082 | PostgreSQL `shipment` |
| `inventory` | Java 17 / Spring Boot | 8083 | PostgreSQL `inventory` |
| `ordering` | Kotlin / Spring Boot | 8084 | PostgreSQL `ordering` |
| `recommendation-system` | Python / FastAPI | 8085 | PostgreSQL `recommendations` |
| `analytics` | Java 17 / Spring Boot | 8086 | ClickHouse |
| `shop-frontend` | React 18 / Vite | 5173 | — |

## Build & Run Commands

### Infrastructure (start this first)
```bash
docker-compose -f infra/docker-compose.yml up -d
```
This starts PostgreSQL (port 45432), Kafka (port 19093), Zookeeper, Kafka UI (port 17080), Debezium Connect (port 48083), ClickHouse (ports 8123/9000), and Redis (port 46379).

After Debezium is up, register the CDC connectors:
```bash
bash infra/setup-cdc.sh
```

### Java/Kotlin Spring Boot services
From within each service directory (e.g., `catalog/`, `ordering/`, etc.):
```bash
./gradlew bootRun          # run the service
./gradlew build            # build JAR
./gradlew test             # run all tests
./gradlew test --tests "hr.fer.dipl.SomeTest"  # run a single test class
```

### Recommendation system
```bash
cd recommendation-system
cp app/.env-sample app/.env   # fill in values first
pip install -r requirements.txt
python -m uvicorn app.main:app --reload --port 8085
```
Requires a `.env` file (see `app/.env-sample`). Pre-trained model files must be present in `recommendation-system/models/` — they are `.joblib` files committed to the repo.

### Frontend
```bash
cd shop-frontend
npm install
npm run start    # dev server (Vite)
npm run build    # production build
npm test         # Vitest
```

## Architecture

### Authentication
The `user` service owns the RSA key pair and issues JWT tokens. All other services validate tokens using only the public key (`public.pem`), which is embedded as a classpath resource in each service. When adding a new service, copy `public.pem` from an existing service.

### Service Communication
- **Synchronous (Feign clients)**: `ordering` → `inventory` (reserve/commit stock), `ordering` → `catalog` (product details), `catalog` → `recommendation-system` (get recommendations), `inventory` → `catalog`.
- **Asynchronous (Kafka)**: `ordering` publishes order events → `shipment` consumes them (topic: `shipment-events`). `shipment` publishes status updates → `ordering` consumes them (topic: `shipment-status-updates`). `ordering` publishes purchase events → `recommendation-system` consumes them (topic: `recommendation-events`).

### Analytics Pipeline (CDC)
Debezium captures row-level changes from PostgreSQL (WAL logical replication) and publishes them to Kafka topics. A Python connector (`infra/connectors/kafka_to_clickhouse.py`) consumes these topics and inserts into ClickHouse fact tables (`orders_fact`, `order_items_fact`, `inventory_fact`, `cart_items_fact`, `shipment_fact`). The `analytics` service queries ClickHouse and exposes aggregated metrics to the frontend dashboard.

### Recommendation System
Hybrid approach combining:
- **Content-based**: TF-IDF on product name/category/description + cosine similarity. Pre-computed matrices are stored as `.joblib` files and loaded at startup.
- **Collaborative filtering**: TruncatedSVD on a user-item interaction matrix, retrained periodically (APScheduler) and on startup if not found.

Recommendations are cached in Redis (TTL: 1 hour, key: `recs:user:{userId}`). The engine falls back to most-popular products when a user has no interaction history.

### Database Migrations
All Java/Kotlin services use **Liquibase**. Changelogs live in `src/main/resources/db/changelog/` and run automatically on service startup. `ddl-auto: validate` is used in all services (Liquibase manages DDL, Hibernate only validates).

### Frontend API Layer
`shop-frontend/src/api.js` creates separate Axios instances for each backend service, each with a request interceptor that attaches the JWT token from `localStorage`. The `AuthContext` (`src/auth/AuthContext.jsx`) provides auth state globally. Role-based UI gating uses `isAdmin()` checks in `AdminRoute.jsx`.

## Key Conventions

- **Package structure** (Java/Kotlin): `hr.fer.dipl.*` with sub-packages: `controller`, `service`, `db/model`, `db/repository`, `dto`, `mapper`, `config`, `exception`, `client` (Feign).
- **Ordering/Shipment** are written in Kotlin; the remaining Spring Boot services (`user`, `catalog`, `inventory`, `analytics`) are Java.
- The Kafka bootstrap server for local development is `localhost:19093`.
- PostgreSQL runs on non-standard port `45432` (to avoid conflicts with local installs).
- Redis runs on non-standard port `46379`.