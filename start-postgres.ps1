# Start PostgreSQL for local development (Phase 13)
# Usage: .\start-postgres.ps1

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

Write-Host "Starting PostgreSQL via Docker Compose..." -ForegroundColor Cyan
docker compose up -d postgres

Write-Host "Waiting for PostgreSQL to become healthy..." -ForegroundColor Yellow
$attempts = 0
while ($attempts -lt 30) {
    $status = docker inspect -f "{{.State.Health.Status}}" lead-intel-postgres 2>$null
    if ($status -eq "healthy") {
        Write-Host "PostgreSQL is ready on localhost:5432" -ForegroundColor Green
        Write-Host "  Databases: location_db, company_db, auth_db, category_db"
        Write-Host "  User/Pass: datascraper / datascraper"
        exit 0
    }
    Start-Sleep -Seconds 2
    $attempts++
}

Write-Host "PostgreSQL did not become healthy in time. Check: docker compose logs postgres" -ForegroundColor Red
exit 1
