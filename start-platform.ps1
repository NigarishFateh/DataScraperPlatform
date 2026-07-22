# Start the current Lead Intelligence backend (extension-focused architecture).
# Does NOT start legacy scraper-google / scraper-microsoft / scraper-ibm.
# Usage: .\start-platform.ps1

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

function Test-PortListening([int]$Port) {
    return [bool](Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
}

function Start-ServiceJar([string]$Name, [string]$JarRelativePath, [int]$Port) {
    if (Test-PortListening $Port) {
        Write-Host "Skipping $Name — port $Port already in use." -ForegroundColor DarkYellow
        return
    }

    $jarPath = Join-Path $root $JarRelativePath
    if (-not (Test-Path $jarPath)) {
        Write-Host "Missing jar for $Name : $jarPath" -ForegroundColor Red
        exit 1
    }

    Write-Host "Starting $Name on port $Port..." -ForegroundColor Green
    Start-Process -FilePath "java" -ArgumentList "-jar", $jarPath -WorkingDirectory $root -WindowStyle Minimized
    Start-Sleep -Seconds 2
}

Write-Host "Ensuring PostgreSQL is available..." -ForegroundColor Cyan
& "$root\start-postgres.ps1"
if ($LASTEXITCODE -ne 0) { exit 1 }

Write-Host "Building platform modules..." -ForegroundColor Cyan
& .\mvnw.cmd -q install -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed." -ForegroundColor Red
    exit 1
}

$services = @(
    @{ Name = "gateway-service";      Jar = "gateway-service\target\gateway-service-0.0.1-SNAPSHOT.jar";      Port = 8080 },
    @{ Name = "auth-service";         Jar = "auth-service\target\auth-service-0.0.1-SNAPSHOT.jar";         Port = 8081 },
    @{ Name = "location-service";     Jar = "location-service\target\location-service-0.0.1-SNAPSHOT.jar"; Port = 8082 },
    @{ Name = "company-service";      Jar = "company-service\target\company-service-0.0.1-SNAPSHOT.jar";  Port = 8083 },
    @{ Name = "category-service";     Jar = "category-service\target\category-service-0.0.1-SNAPSHOT.jar"; Port = 8084 },
    @{ Name = "scraper-orchestrator"; Jar = "scraper-orchestrator\target\scraper-orchestrator-0.0.1-SNAPSHOT.jar"; Port = 8085 },
    @{ Name = "scraper-website";      Jar = "scraper-website\target\scraper-website-0.0.1-SNAPSHOT.jar";      Port = 8091 },
    @{ Name = "scraper-tech";         Jar = "scraper-tech\target\scraper-tech-0.0.1-SNAPSHOT.jar";         Port = 8092 },
    @{ Name = "scraper-news";         Jar = "scraper-news\target\scraper-news-0.0.1-SNAPSHOT.jar";         Port = 8093 },
    @{ Name = "scraper-github";       Jar = "scraper-github\target\scraper-github-0.0.1-SNAPSHOT.jar";       Port = 8094 },
    @{ Name = "scraper-contact";      Jar = "scraper-contact\target\scraper-contact-0.0.1-SNAPSHOT.jar";  Port = 8095 }
)

foreach ($svc in $services) {
    Start-ServiceJar -Name $svc.Name -JarRelativePath $svc.Jar -Port $svc.Port
}

Write-Host ""
Write-Host "Platform services:" -ForegroundColor Cyan
Write-Host "  Gateway      http://localhost:8080"
Write-Host "  Auth         http://localhost:8081"
Write-Host "  Location     http://localhost:8082"
Write-Host "  Company      http://localhost:8083"
Write-Host "  Category     http://localhost:8084"
Write-Host "  Orchestrator http://localhost:8085"
Write-Host "  Scrapers     8091-8095"
Write-Host ""
Write-Host "Chrome extension:" -ForegroundColor Cyan
Write-Host "  1) cd chrome-extension"
Write-Host "  2) npm install && npm run build"
Write-Host "  3) Load unpacked extension from chrome-extension\dist"
Write-Host ""
Write-Host "To stop services: .\stop-platform.ps1"
