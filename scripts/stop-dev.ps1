# ============================================================
# stop-dev.ps1  —  Stop all Spring Boot services + Docker infra
# ============================================================
# Usage:
#   .\scripts\stop-dev.ps1              # kill services + stop containers
#   .\scripts\stop-dev.ps1 -KeepInfra   # kill JVMs only, leave Docker running
# ============================================================

param(
    [switch]$KeepInfra
)

function Write-Header($msg) { Write-Host "`n==> $msg" -ForegroundColor Cyan }
function Write-Ok($msg)     { Write-Host "    [OK]  $msg" -ForegroundColor Green }
function Write-Warn($msg)   { Write-Host "    [!!]  $msg" -ForegroundColor Yellow }

$Root = Split-Path -Parent $PSScriptRoot

$Ports = @(8080, 8081, 8082, 8083, 8084, 8086, 8090)

# ── 1. Kill Java processes on known service ports ────────────
Write-Header "Stopping Spring Boot services..."

foreach ($port in $Ports) {
    # Find PID listening on the port
    $netstat = netstat -ano | Select-String ":$port\s"
    foreach ($line in $netstat) {
        if ($line -match "\s+(\d+)\s*$") {
            $pid_ = $matches[1]
            $proc = Get-Process -Id $pid_ -ErrorAction SilentlyContinue
            if ($proc -and $proc.Name -match "java") {
                Stop-Process -Id $pid_ -Force
                Write-Ok "Killed $($proc.Name) PID $pid_ (was on port $port)"
            }
        }
    }
}

# ── 2. Docker infra ──────────────────────────────────────────
if (-not $KeepInfra) {
    Write-Header "Stopping Docker infrastructure..."
    $composeFile = Join-Path $Root "infra\docker-compose.yml"
    docker compose -f $composeFile down
    Write-Ok "Docker containers stopped."
} else {
    Write-Warn "Keeping Docker infrastructure running (--KeepInfra flag set)."
}

Write-Header "Done."
