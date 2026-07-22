# Start catalog backend: PostgreSQL + Gateway + Auth + Location + Company + Category
# Usage: .\start-foundation.ps1

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

Write-Host "Ensuring PostgreSQL is running..." -ForegroundColor Cyan
& "$root\start-postgres.ps1"
if ($LASTEXITCODE -ne 0) {
    exit 1
}

Write-Host "Building catalog services..." -ForegroundColor Cyan
& .\mvnw.cmd -q -pl gateway-service,auth-service,location-service,company-service,category-service -am -DskipTests package
if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed." -ForegroundColor Red
    exit 1
}

$jobs = @(
    @{ Name = "auth-service";     Jar = "auth-service\target\auth-service-0.0.1-SNAPSHOT.jar";     Port = 8081 },
    @{ Name = "location-service"; Jar = "location-service\target\location-service-0.0.1-SNAPSHOT.jar"; Port = 8082 },
    @{ Name = "company-service";  Jar = "company-service\target\company-service-0.0.1-SNAPSHOT.jar";  Port = 8083 },
    @{ Name = "category-service"; Jar = "category-service\target\category-service-0.0.1-SNAPSHOT.jar"; Port = 8084 },
    @{ Name = "gateway-service";  Jar = "gateway-service\target\gateway-service-0.0.1-SNAPSHOT.jar";  Port = 8080 }
)

foreach ($svc in $jobs) {
    $jarPath = Join-Path $root $svc.Jar
    if (-not (Test-Path $jarPath)) {
        Write-Host "Missing jar: $jarPath" -ForegroundColor Red
        exit 1
    }
    Write-Host "Starting $($svc.Name) on port $($svc.Port)..." -ForegroundColor Green
    Start-Process -FilePath "java" -ArgumentList "-jar", $jarPath -WindowStyle Minimized
    Start-Sleep -Seconds 2
}

Write-Host ""
Write-Host "Catalog services started:" -ForegroundColor Cyan
Write-Host "  PostgreSQL localhost:5432 (datascraper/datascraper)"
Write-Host "  Gateway    http://localhost:8080"
Write-Host "  Auth       http://localhost:8081"
Write-Host "  Location   http://localhost:8082"
Write-Host "  Company    http://localhost:8083"
Write-Host "  Category   http://localhost:8084"
Write-Host ""
Write-Host "Try: GET http://localhost:8080/api/companies/search?cityIds=DE-berlin&page=0&pageSize=8"
Write-Host "Try: GET http://localhost:8080/api/categories"
