# Start Dev Environment

Start the full e-shop development environment (Docker infra + all Spring Boot services).

## Usage
- `/dev-start`               — starts everything (Docker + all 7 services)
- `/dev-start skip-infra`    — skips Docker compose (use when infra is already up)
- `/dev-start only=user,catalog,gateway`  — starts only listed services

## Steps to execute

1. Parse the argument `$ARGUMENTS`:
   - If it contains `skip-infra`, add `-SkipInfra` flag to the script call.
   - If it contains `only=<list>`, extract the comma-separated list and pass it as `-Services "<list>"`.

2. Run the startup script using PowerShell:
   ```
   .\scripts\start-dev.ps1 [flags]
   ```
   from the project root `C:\Users\kbosnjak3\IdeaProjects\katarina-learning`.

3. Report back:
   - What Docker containers are running (`docker compose -f infra/docker-compose.yml ps`)
   - Which service windows were opened and on which ports
   - Any warnings (port already in use, missing gradlew, etc.)

## Service port reference
| Service    | Port | Notes                        |
|------------|------|------------------------------|
| user       | 8080 | Auth / JWT issuer             |
| catalog    | 8081 | Product search, recommendations |
| shipment   | 8082 | Shipment tracking             |
| inventory  | 8083 | Stock & reservations          |
| ordering   | 8084 | Cart & orders (Kafka producer)|
| analytics  | 8086 | ClickHouse-backed analytics   |
| gateway    | 8090 | **Single entry point (CORS)** |

## Useful URLs after startup
- Gateway → http://localhost:8090
- Kafka UI → http://localhost:17080
- ClickHouse → http://localhost:8123
