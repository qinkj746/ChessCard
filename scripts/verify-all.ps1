<#
.SYNOPSIS
    Runs full project verification: backend tests, then Flutter tests and analysis.
.DESCRIPTION
    Executes test-server.ps1 first. If backend passes, runs test-app.ps1.
    Stops at the first failure and reports the outcome.
#>
param()

$ErrorActionPreference = 'Stop'
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition

Write-Host "============================================" -ForegroundColor White
Write-Host " ChessCard Full Verification" -ForegroundColor White
Write-Host "============================================" -ForegroundColor White

# Step 1: Backend
Write-Host "`n[1/2] Running backend tests..." -ForegroundColor Cyan
& powershell -ExecutionPolicy Bypass -File (Join-Path $scriptDir 'test-server.ps1')
$serverExit = $LASTEXITCODE

if ($serverExit -ne 0) {
    Write-Host "`n============================================" -ForegroundColor Red
    Write-Host " VERIFICATION FAILED: Backend tests failed." -ForegroundColor Red
    Write-Host "============================================" -ForegroundColor Red
    exit 1
}

# Step 2: Flutter
Write-Host "`n[2/2] Running Flutter tests and analysis..." -ForegroundColor Cyan
& powershell -ExecutionPolicy Bypass -File (Join-Path $scriptDir 'test-app.ps1')
$appExit = $LASTEXITCODE

if ($appExit -ne 0) {
    Write-Host "`n============================================" -ForegroundColor Red
    Write-Host " VERIFICATION FAILED: Flutter tests failed." -ForegroundColor Red
    Write-Host "============================================" -ForegroundColor Red
    exit 1
}

Write-Host "`n============================================" -ForegroundColor Green
Write-Host " ALL VERIFICATIONS PASSED" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Green
exit 0
