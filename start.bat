@echo off
title LedgerLine Launcher
echo ============================================================
echo   Starting LedgerLine Core Banking Engine and Frontend...
echo ============================================================

REM 1. Start Backend in a dedicated window
echo [1/3] Starting Spring Boot Backend on http://localhost:8080 ...
start "LedgerLine Backend (Port 8080)" cmd /k "cd /d %~dp0backend && java -jar target\ledgerline-1.0.0.jar"

REM 2. Start Frontend in a dedicated window
echo [2/3] Starting React + Vite Frontend on http://localhost:5173 ...
start "LedgerLine Frontend (Port 5173)" cmd /k "cd /d %~dp0frontend && npm run dev"

REM 3. Wait and open browser
echo [3/3] Opening dashboard in browser...
timeout /t 5 >nul
start http://localhost:5173/

echo ============================================================
echo   LedgerLine is running!
echo   Frontend: http://localhost:5173/
echo   Backend:  http://localhost:8080/
echo   Health:   http://localhost:8080/api/health
echo   Audit:    http://localhost:8080/api/ledger/verify-integrity
echo ============================================================
