# Create PostgreSQL role, databases, and grants for the platform.
# Usage: .\setup-postgres.ps1
# Requires: psql (PostgreSQL client) and superuser access (default: postgres)

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$sqlFile = Join-Path $root "infra\postgres\setup-native-postgres.sql"

function Find-PsqlPath {
    $cmd = Get-Command psql -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }

    $installed = Get-ChildItem "C:\Program Files\PostgreSQL\*\bin\psql.exe" -ErrorAction SilentlyContinue |
        Sort-Object FullName -Descending |
        Select-Object -First 1
    if ($installed) { return $installed.FullName }
    return $null
}

$psql = Find-PsqlPath
if (-not $psql) {
    Write-Host "psql not found. Install PostgreSQL or add psql to PATH." -ForegroundColor Red
    Write-Host "Download: https://www.postgresql.org/download/windows/" -ForegroundColor Yellow
    Write-Host "Then run this script again, or execute in pgAdmin:" -ForegroundColor Yellow
    Write-Host "  $sqlFile"
    exit 1
}

$pgUser = if ($env:PGUSER) { $env:PGUSER } else { "postgres" }
$pgHost = if ($env:PGHOST) { $env:PGHOST } else { "localhost" }
$pgPort = if ($env:PGPORT) { $env:PGPORT } else { "5432" }

Write-Host "Using psql: $psql" -ForegroundColor Cyan
Write-Host "Connecting as $pgUser@${pgHost}:$pgPort ..." -ForegroundColor Cyan
Write-Host "You may be prompted for the PostgreSQL superuser password." -ForegroundColor Yellow

& $psql -U $pgUser -h $pgHost -p $pgPort -f $sqlFile
if ($LASTEXITCODE -ne 0) {
    Write-Host "PostgreSQL setup failed." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "PostgreSQL ready:" -ForegroundColor Green
Write-Host "  User/Pass: datascraper / datascraper"
Write-Host "  Databases: location_db, company_db, auth_db, category_db, job_db, discovery_db, export_db"
Write-Host ""
Write-Host "Next: restart services with .\stop-platform.ps1 then .\start-platform.ps1"
