<#
.SYNOPSIS
    Runs Flutter app tests and static analysis.
.DESCRIPTION
    Enters the app directory, runs flutter test then flutter analyze.
    Stops on the first failure.
#>
param()

$ErrorActionPreference = 'Stop'
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
$projectRoot = Split-Path -Parent $scriptDir
$appDir = Join-Path $projectRoot 'app'

if (-not (Test-Path $appDir)) {
    Write-Error "App directory not found: $appDir"
    exit 1
}

Write-Host "`n=== Flutter Tests ===" -ForegroundColor Cyan
Push-Location $appDir
try {
    & flutter test
    $testExit = $LASTEXITCODE

    if ($testExit -ne 0) {
        Write-Host "`n[FAIL] Flutter tests failed (exit code: $testExit)." -ForegroundColor Red
        exit $testExit
    }

    Write-Host "`n=== Flutter Analyze ===" -ForegroundColor Cyan
    & flutter analyze
    $analyzeExit = $LASTEXITCODE

    if ($analyzeExit -ne 0) {
        Write-Host "`n[FAIL] Flutter analyze failed (exit code: $analyzeExit)." -ForegroundColor Red
        exit $analyzeExit
    }
} finally {
    Pop-Location
}

Write-Host "`n[OK] Flutter tests and analysis passed." -ForegroundColor Green
exit 0
