# Ensure PostgreSQL is available for local development.
# Usage: .\start-postgres.ps1
#
# Option A: Docker Desktop (recommended if installed)
# Option B: Native PostgreSQL already listening on localhost:5432

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

function Test-PostgresPort {
    return (Test-NetConnection -ComputerName localhost -Port 5432 -WarningAction SilentlyContinue).TcpTestSucceeded
}

function Get-DockerCommand {
    $docker = Get-Command docker -ErrorAction SilentlyContinue
    if ($null -ne $docker) { return $docker.Source }

    $defaultPath = "C:\Program Files\Docker\Docker\resources\bin\docker.exe"
    if (Test-Path $defaultPath) { return $defaultPath }
    return $null
}

if (Test-PostgresPort) {
    Write-Host "PostgreSQL is already listening on localhost:5432" -ForegroundColor Green
    Write-Host "  Expected databases: location_db, company_db, auth_db, category_db"
    Write-Host "  Expected user/pass: datascraper / datascraper"
    Write-Host ""
    Write-Host "If services fail to connect, run once:" -ForegroundColor Yellow
    Write-Host "  .\setup-postgres.ps1"
    exit 0
}

$dockerExe = Get-DockerCommand
if ($null -eq $dockerExe) {
    Write-Host "Docker is not installed and PostgreSQL is not running on port 5432." -ForegroundColor Red
    Write-Host ""
    Write-Host "Choose one setup path:" -ForegroundColor Yellow
    Write-Host "  1) Install Docker Desktop, reopen PowerShell, then run .\start-postgres.ps1 again"
    Write-Host "  2) Install PostgreSQL for Windows, then run:"
    Write-Host "       psql -U postgres -f infra/postgres/init-databases.sql"
    Write-Host "     and create user/password datascraper/datascraper with access to those DBs."
    exit 1
}

Write-Host "Starting PostgreSQL via Docker Compose..." -ForegroundColor Cyan
& $dockerExe compose up -d postgres
if ($LASTEXITCODE -ne 0) {
    Write-Host "Failed to start Docker PostgreSQL." -ForegroundColor Red
    exit 1
}

Write-Host "Waiting for PostgreSQL to become healthy..." -ForegroundColor Yellow
$attempts = 0
while ($attempts -lt 30) {
    $status = & $dockerExe inspect -f "{{.State.Health.Status}}" lead-intel-postgres 2>$null
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
