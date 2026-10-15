# Automated Build Script for BC Android (Standard & Discreet Release APKs)
param(
    [switch]$SkipTests
)

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Magenta
Write-Host "   BC Android - Automated Build Pipeline" -ForegroundColor Magenta
Write-Host "========================================" -ForegroundColor Magenta

# Auto-configure environment if not already set
if (-not $env:JAVA_HOME -or -not (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    $candidates = @(
        "C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot",
        "C:\Program Files\Eclipse Adoptium\jdk-17*",
        "C:\Program Files\Java\jdk-17*"
    )
    foreach ($c in $candidates) {
        $found = Get-Item $c -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($found -and (Test-Path "$($found.FullName)\bin\java.exe")) {
            $env:JAVA_HOME = $found.FullName
            break
        }
    }
}
if ($env:JAVA_HOME) {
    $env:PATH = "$env:JAVA_HOME\bin;" + $env:PATH
}

if (-not $env:ANDROID_HOME -or -not (Test-Path $env:ANDROID_HOME)) {
    $defaultSdk = "$env:LOCALAPPDATA\Android\Sdk"
    if (Test-Path $defaultSdk) {
        $env:ANDROID_HOME = $defaultSdk
        $env:ANDROID_SDK_ROOT = $defaultSdk
    }
}

# 1. Run unit tests
if (-not $SkipTests) {
    Write-Host "`n[1/4] Running automated unit tests..." -ForegroundColor Cyan
    if (Test-Path ".\gradlew.bat") {
        & .\gradlew.bat test --daemon
    } else {
        gradle test
    }

    if ($LASTEXITCODE -ne 0) {
        Write-Error "Automated unit tests failed! Build aborted."
        exit 1
    }
    Write-Host "Unit tests passed successfully." -ForegroundColor Green
} else {
    Write-Host "`n[1/4] Skipping tests (-SkipTests specified)..." -ForegroundColor Yellow
}

# 2. Clean previous publish folder
Write-Host "`n[2/4] Cleaning previous publish artifacts..." -ForegroundColor Cyan
if (Test-Path "publish") {
    Remove-Item -Path "publish" -Recurse -Force -ErrorAction SilentlyContinue
}
New-Item -ItemType Directory -Path "publish" -Force | Out-Null

# 3. Build Standard & Discreet Release APKs
Write-Host "`n[3/4] Compiling Release APKs (Standard & Discreet)..." -ForegroundColor Cyan
if (Test-Path ".\gradlew.bat") {
    & .\gradlew.bat assembleStandardRelease assembleDiscreetRelease --daemon
} else {
    gradle assembleStandardRelease assembleDiscreetRelease
}

if ($LASTEXITCODE -ne 0) {
    Write-Error "Gradle release build failed!"
    exit 1
}

# 4. Copy and verify output APKs
Write-Host "`n[4/4] Collecting output APKs to publish/..." -ForegroundColor Cyan
$standardApk = Get-ChildItem -Path "app\build\outputs\apk\standard\release\*.apk" | Select-Object -First 1
$discreetApk = Get-ChildItem -Path "app\build\outputs\apk\discreet\release\*.apk" | Select-Object -First 1

if ($standardApk) {
    Copy-Item $standardApk.FullName -Destination "publish\BC-Android-Standard-Release.apk" -Force
    Write-Host " -> Copied: publish\BC-Android-Standard-Release.apk ($([math]::Round($standardApk.Length / 1MB, 2)) MB)" -ForegroundColor Green
} else {
    Write-Warning "Standard Release APK was not found in expected output folder."
}

if ($discreetApk) {
    Copy-Item $discreetApk.FullName -Destination "publish\BC-Android-Discreet-Release.apk" -Force
    Write-Host " -> Copied: publish\BC-Android-Discreet-Release.apk ($([math]::Round($discreetApk.Length / 1MB, 2)) MB)" -ForegroundColor Green
} else {
    Write-Warning "Discreet Release APK was not found in expected output folder."
}

Write-Host "`n========================================" -ForegroundColor Magenta
Write-Host " Build Complete! Artifacts ready in publish/" -ForegroundColor Magenta
Write-Host "========================================" -ForegroundColor Magenta
Get-ChildItem -Path "publish\*.apk" | Select-Object Name, @{Name="Size (MB)"; Expression={[math]::Round($_.Length / 1MB, 2)}} | Format-Table
