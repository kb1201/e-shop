# Stop Dev Environment

Stops all running Spring Boot services and optionally the Docker infrastructure.

## Usage
- `/dev-stop`              — kills all Spring Boot JVMs + stops Docker containers
- `/dev-stop keep-infra`   — kills JVMs only, leaves Docker running (faster restart next time)

## Steps to execute

1. Parse `$ARGUMENTS`:
   - If it contains `keep-infra`, add `-KeepInfra` flag.

2. Run:
   ```
   .\scripts\stop-dev.ps1 [flags]
   ```
   from the project root.

3. Report which PIDs were killed and whether Docker was stopped.
