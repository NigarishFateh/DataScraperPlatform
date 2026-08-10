# Stop Global Business Intelligence Platform services.
# Usage: .\stop-platform.ps1

$ports = @(8080, 8081, 8082, 8083, 8084, 8085, 8086, 8087, 8088, 8091, 8095)

Write-Host "Stopping platform Java services on ports: $($ports -join ', ')" -ForegroundColor Cyan

$stopped = 0
foreach ($port in $ports) {
    $connections = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    foreach ($connection in $connections) {
        $procId = $connection.OwningProcess
        $process = Get-Process -Id $procId -ErrorAction SilentlyContinue
        if ($null -eq $process) { continue }

        $commandLine = (Get-CimInstance Win32_Process -Filter "ProcessId=$procId" -ErrorAction SilentlyContinue).CommandLine
        if ($commandLine -match "DataScraperPlatform" -or $process.ProcessName -eq "java") {
            Write-Host "Stopping PID $procId on port $port" -ForegroundColor Yellow
            Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
            $stopped++
        }
    }
}

Write-Host "Stopped $stopped process(es)." -ForegroundColor Green
Write-Host "PostgreSQL and Redis containers are left running." -ForegroundColor DarkGray
