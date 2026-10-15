# Fast local development build script for BC Android
param(
    [switch]$Install
)

$ErrorActionPreference = "Stop"

Write-Host "Building Local Debug APK..." -ForegroundColor Cyan
if (Test-Path ".\gradlew.bat") {
    & .\gradlew.bat assembleStandardDebug --daemon
} else {
    gradle assembleStandardDebug
}

if ($LASTEXITCODE -ne 0) {
    Write-Error "Local debug build failed!"
    exit 1
}

$apk = Get-ChildItem -Path "app\build\outputs\apk\standard\debug\*.apk" | Select-Object -First 1
if ($apk) {
    Write-Host "Debug APK built successfully: $($apk.FullName) ($([math]::Round($apk.Length / 1MB, 2)) MB)" -ForegroundColor Green

    if ($Install) {
        Write-Host "Installing to connected device via adb..." -ForegroundColor Cyan
        adb install -r $apk.FullName
    }
}
