# Start the Lead Intelligence backend (Chrome extension architecture).
# Usage: .\start-platform.ps1   (run from project root, not chrome-extension)

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

function Import-DotEnv([string]$Path) {
    if (-not (Test-Path $Path)) { return }
    Write-Host "Loading environment from $Path" -ForegroundColor Cyan
    Get-Content $Path | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith("#")) { return }
        $eq = $line.IndexOf("=")
        if ($eq -lt 1) { return }
        $key = $line.Substring(0, $eq).Trim()
        $value = $line.Substring($eq + 1).Trim()
        if (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'"))) {
            $value = $value.Substring(1, $value.Length - 2)
        }
        [Environment]::SetEnvironmentVariable($key, $value, "Process")
        Set-Item -Path "Env:$key" -Value $value
    }
}

function Test-PortListening([int]$Port) {
    return [bool](Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
}

function Start-ServiceJar([string]$Name, [string]$JarRelativePath, [int]$Port) {
    if (Test-PortListening $Port) {
        Write-Host "Already running: $Name (port $Port)" -ForegroundColor DarkYellow
        return
    }

    $jarPath = Join-Path $root $JarRelativePath
    if (-not (Test-Path $jarPath)) {
        Write-Host "Missing jar for $Name : $jarPath" -ForegroundColor Red
        Write-Host "Run: .\mvnw.cmd -pl $Name -am package -DskipTests" -ForegroundColor Yellow
        exit 1
    }

    Write-Host "Starting $Name on port $Port..." -ForegroundColor Green
    Start-Process -FilePath "java" -ArgumentList "-jar", $jarPath -WorkingDirectory $root -WindowStyle Minimized
    # Cold start on newer JDKs often exceeds 3s; poll instead of a fixed sleep.
    $ready = $false
    for ($i = 0; $i -lt 20; $i++) {
        Start-Sleep -Seconds 1
        if (Test-PortListening $Port) {
            $ready = $true
            break
        }
    }
    if (-not $ready) {
        Write-Host "WARNING: $Name did not stay up on port $Port (crashed after start)." -ForegroundColor Red
        if ($Name -eq "auth-service") {
            Write-Host "  Fix DB permissions, then retry:" -ForegroundColor Yellow
            Write-Host "  .\fix-auth-db.ps1"
        }
    }
}

Import-DotEnv (Join-Path $root ".env")

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

Write-Host "Ensuring PostgreSQL is available..." -ForegroundColor Cyan
& "$root\start-postgres.ps1"
if ($LASTEXITCODE -ne 0) { exit 1 }

$toBuild = @()
foreach ($svc in $services) {
    if (-not (Test-PortListening $svc.Port)) {
        $toBuild += $svc.Name
    }
}

if ($toBuild.Count -eq 0) {
    Write-Host "All platform services are already running. Skipping build." -ForegroundColor Green
}
else {
    $moduleList = ($toBuild + @("platform-common") | Select-Object -Unique) -join ","
    Write-Host "Building modules not currently running: $moduleList" -ForegroundColor Cyan
    cmd /c ".\mvnw.cmd -q -pl $moduleList -am package -DskipTests"
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Build failed. If jars are locked, run .\stop-platform.ps1 first, then retry." -ForegroundColor Red
        exit 1
    }
}

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
Write-Host "Chrome extension (already built):" -ForegroundColor Cyan
Write-Host '  Load unpacked from chrome-extension\dist in chrome://extensions'
Write-Host ""
Write-Host 'To stop services: .\stop-platform.ps1'
Write-Host ""
Write-Host "If auth fails to start (permission denied for schema public), run:" -ForegroundColor Yellow
Write-Host "  .\setup-postgres.ps1"
