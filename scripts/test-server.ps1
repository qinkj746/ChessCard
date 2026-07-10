<#
.SYNOPSIS
    Runs backend (server) unit tests.
.DESCRIPTION
    Sets JAVA_HOME if not already set, enters the server directory, and runs Maven tests.
    Exits with Maven's exit code so callers can detect failure.
#>
param()

$ErrorActionPreference = 'Stop'
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
$projectRoot = Split-Path -Parent $scriptDir
$serverDir = Join-Path $projectRoot 'server'

if (-not (Test-Path $serverDir)) {
    Write-Error "Server directory not found: $serverDir"
    exit 1
}

# Set JAVA_HOME if not already configured
if (-not $env:JAVA_HOME) {
    $defaultJava = 'D:\Java\jdk17'
    if (Test-Path $defaultJava) {
        $env:JAVA_HOME = $defaultJava
        $env:Path = "$defaultJava\bin;" + $env:Path
        Write-Host "[INFO] JAVA_HOME set to $defaultJava" -ForegroundColor Yellow
    } else {
        Write-Error "JAVA_HOME is not set and default path $defaultJava does not exist. Please set JAVA_HOME."
        exit 1
    }
} else {
    Write-Host "[INFO] Using existing JAVA_HOME: $env:JAVA_HOME" -ForegroundColor Yellow
}

Write-Host "`n=== Backend Tests ===" -ForegroundColor Cyan
Push-Location $serverDir
try {
    & mvn test
    $exitCode = $LASTEXITCODE
} finally {
    Pop-Location
}

if ($exitCode -eq 0) {
    Write-Host "`n[OK] Backend tests passed." -ForegroundColor Green
} else {
    Write-Host "`n[FAIL] Backend tests failed (exit code: $exitCode)." -ForegroundColor Red
}

exit $exitCode
