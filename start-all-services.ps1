# DEPRECATED: legacy Phase-1 scrapers conflict with current service ports.
# Use .\start-platform.ps1 instead.

Write-Host "start-all-services.ps1 is deprecated." -ForegroundColor Yellow
Write-Host "It launched legacy scraper-google/microsoft/ibm on ports 8081-8083," -ForegroundColor Yellow
Write-Host "which conflict with auth/location/company services." -ForegroundColor Yellow
Write-Host ""
Write-Host "Run this instead:" -ForegroundColor Green
Write-Host "  .\start-platform.ps1"
exit 1
