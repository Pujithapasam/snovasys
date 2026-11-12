<#
.SYNOPSIS
    BalanceVault - First-Admin Bootstrap Utility (PowerShell)
.DESCRIPTION
    Bootstraps the root administrator account via the /api/admin/bootstrap endpoint.
#>

param (
    [string]$BackendUrl = "http://localhost:8080",
    [string]$AdminUsername = "admin",
    [string]$AdminPassword = "AdminSecure2026!",
    [string]$AdminEmail = "admin@balancevault.internal",
    [string]$BootstrapSecret = "balancevault-bootstrap-secret-2026"
)

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  BalanceVault - First-Admin Bootstrap Utility" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "Target URL:       $BackendUrl"
Write-Host "Admin Username:   $AdminUsername"
Write-Host "Admin Email:      $AdminEmail"
Write-Host "=========================================================="

$payload = @{
    username = $AdminUsername
    password = $AdminPassword
    email    = $AdminEmail
    secret   = $BootstrapSecret
} | ConvertTo-Json

try {
    Write-Host "[i] Invoking bootstrap endpoint..." -ForegroundColor Yellow
    $response = Invoke-RestMethod -Uri "$BackendUrl/api/admin/bootstrap" `
        -Method Post `
        -ContentType "application/json" `
        -Body $payload

    Write-Host "✅ First-Admin bootstrap SUCCESSFUL!" -ForegroundColor Green
    Write-Host "Admin user '$AdminUsername' configured with role ADMIN." -ForegroundColor Green
    Write-Host "Timestamp: $($response.bootstrappedAt)"
}
catch {
    Write-Host "❌ Bootstrap failed: $_" -ForegroundColor Red
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        Write-Host "Server Response: $($reader.ReadToEnd())" -ForegroundColor Red
    }
}

