Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "   Starting LedgerLine Core Banking Engine & Frontend...     " -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

$projectRoot = $PSScriptRoot
if (-not $projectRoot) { $projectRoot = Get-Location }

# 1. Start Backend in new PowerShell window
Write-Host "[1/3] Starting Spring Boot Backend on http://localhost:8080 ..." -ForegroundColor Green
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$projectRoot\backend'; Write-Host '--- LedgerLine Spring Boot Backend (Port 8080) ---' -ForegroundColor Cyan; java -jar target\ledgerline-1.0.0.jar"

# 2. Start Frontend in new PowerShell window
Write-Host "[2/3] Starting React + Vite Frontend on http://localhost:5173 ..." -ForegroundColor Green
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$projectRoot\frontend'; Write-Host '--- LedgerLine React Frontend (Port 5173) ---' -ForegroundColor Cyan; npm run dev"

# 3. Wait 5 seconds and open browser
Write-Host "[3/3] Opening dashboard in browser..." -ForegroundColor Yellow
Start-Sleep -Seconds 5
Start-Process "http://localhost:5173/"

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "   LedgerLine is running!                                   " -ForegroundColor Green
Write-Host "   Frontend: http://localhost:5173/                         " -ForegroundColor White
Write-Host "   Backend:  http://localhost:8080/                         " -ForegroundColor White
Write-Host "============================================================" -ForegroundColor Cyan
