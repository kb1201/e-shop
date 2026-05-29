# ============================================================
# start-dev.ps1  —  Start the full e-shop dev environment
# ============================================================
# Usage:
#   .\scripts\start-dev.ps1            # start everything
#   .\scripts\start-dev.ps1 -SkipInfra # skip docker-compose (infra already up)
#   .\scripts\start-dev.ps1 -Services "user,catalog,gateway"  # only some services
# ============================================================

param(
    [switch]$SkipInfra,
    [string]$Services = "all"
)

$Root = Split-Path -Parent $PSScriptRoot   # project root

# ── Colour helpers ──────────────────────────────────────────
function Write-Header($msg) { Write-Host "`n==> $msg" -ForegroundColor Cyan }
function Write-Ok($msg)     { Write-Host "    [OK]  $msg" -ForegroundColor Green }
function Write-Warn($msg)   { Write-Host "    [!!]  $msg" -ForegroundColor Yellow }
function Write-Err($msg)    { Write-Host "    [ERR] $msg" -ForegroundColor Red }

# ── Service definitions (name → port) ───────────────────────
$AllServices = [ordered]@{
    "user"      = 8080
    "catalog"   = 8081
    "shipment"  = 8082
    "inventory" = 8083
    "ordering"  = 8084
    "analytics" = 8086
    "gateway"   = 8090
}

# Determine which services to start
if ($Services -eq "all") {
    $ToStart = $AllServices
} else {
    $ToStart = [ordered]@{}
    foreach ($svc in ($Services -split ",")) {
        $svc = $svc.Trim()
        if ($AllServices.ContainsKey($svc)) {
            $ToStart[$svc] = $AllServices[$svc]
        } else {
            Write-Warn "Unknown service '$svc', skipping."
        }
    }
}

# ── 1. Docker infrastructure ─────────────────────────────────
if (-not $SkipInfra) {
    Write-Header "Starting Docker infrastructure (infra/docker-compose.yml)..."

    $composeFile = Join-Path $Root "infra\docker-compose.yml"
    docker compose -f $composeFile up -d
    if ($LASTEXITCODE -ne 0) {
        Write-Err "docker compose failed. Is Docker Desktop running?"
        exit 1
    }
    Write-Ok "Docker containers started."

    # Wait for PostgreSQL to accept connections
    Write-Header "Waiting for PostgreSQL on port 45432..."
    $maxWait = 60
    $waited  = 0
    do {
        Start-Sleep -Seconds 2
        $waited += 2
        $test = Test-NetConnection -ComputerName localhost -Port 45432 -WarningAction SilentlyContinue
        if ($test.TcpTestSucceeded) { break }
        Write-Host "    ... still waiting ($waited s)" -ForegroundColor DarkGray
    } while ($waited -lt $maxWait)

    if (-not $test.TcpTestSucceeded) {
        Write-Err "PostgreSQL did not come up within $maxWait s. Check Docker logs."
        exit 1
    }
    Write-Ok "PostgreSQL is ready."

    # Extra grace period so Kafka / Debezium finish init
    Write-Host "    Giving Kafka 5 s to finish initialising..." -ForegroundColor DarkGray
    Start-Sleep -Seconds 5
} else {
    Write-Warn "Skipping infrastructure (--SkipInfra flag set)."
}

# ── 2. Spring Boot services ───────────────────────────────────
Write-Header "Starting Spring Boot services..."

foreach ($svc in $ToStart.Keys) {
    $port    = $ToStart[$svc]
    $svcPath = Join-Path $Root $svc

    if (-not (Test-Path (Join-Path $svcPath "gradlew.bat"))) {
        Write-Warn "$svc — no gradlew.bat found at $svcPath, skipping."
        continue
    }

    # Check if port is already occupied
    $inUse = Test-NetConnection -ComputerName localhost -Port $port -WarningAction SilentlyContinue
    if ($inUse.TcpTestSucceeded) {
        Write-Warn "$svc (port $port) — port already in use, skipping."
        continue
    }

    # Open a new PowerShell window with a coloured title
    $cmd = "cd '$svcPath'; `$host.UI.RawUI.WindowTitle = '$svc (:$port)'; .\gradlew.bat bootRun; Read-Host 'Press Enter to close'"
    Start-Process powershell -ArgumentList "-NoExit", "-Command", $cmd

    Write-Ok "$svc started  →  http://localhost:$port"
    Start-Sleep -Milliseconds 500   # stagger launches slightly
}

# ── 3. Summary ───────────────────────────────────────────────
Write-Header "Dev environment is up!"
Write-Host ""
Write-Host "  Service       Port    URL" -ForegroundColor White
Write-Host "  ─────────── ─────── ────────────────────────────" -ForegroundColor DarkGray
foreach ($svc in $ToStart.Keys) {
    $port = $ToStart[$svc]
    Write-Host ("  {0,-12}  {1,-6}  http://localhost:{1}" -f $svc, $port) -ForegroundColor White
}
Write-Host ""
Write-Host "  Gateway (entry point) →  http://localhost:8090" -ForegroundColor Cyan
Write-Host "  Kafka UI              →  http://localhost:17080" -ForegroundColor Cyan
Write-Host "  ClickHouse HTTP       →  http://localhost:8123" -ForegroundColor Cyan
Write-Host ""
