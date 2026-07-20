$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path

$services = @(
    @{ Name = "scraper-google"; Port = 8081 },
    @{ Name = "scraper-microsoft"; Port = 8082 },
    @{ Name = "scraper-ibm"; Port = 8083 },
    @{ Name = "scraper-orchestrator"; Port = 8080 }
)

foreach ($service in $services) {
    $servicePath = Join-Path $projectRoot $service.Name
    $command = "Set-Location '$servicePath'; ..\mvnw.cmd spring-boot:run"

    Start-Process powershell -ArgumentList @(
        "-NoExit",
        "-Command",
        $command
    )

    Write-Host "Started $($service.Name) on port $($service.Port)"
    Start-Sleep -Seconds 2
}

Write-Host ""
Write-Host "All service windows have been launched."
Write-Host "Orchestrator URL: http://localhost:8080/api/health"
Write-Host "Test scrape with:"
Write-Host 'curl -X POST http://localhost:8080/api/scrape'
