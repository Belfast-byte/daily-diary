$ErrorActionPreference = "Stop"

$developRoot = "F:\develop"
$sdkSource = "F:\dev\androidSdk"
$gradleSource = "F:\dev\gradle-8.7"
$javaSource = "F:\Jdk\jdk-21"
$sdkRoot = Join-Path $developRoot "android-sdk"
$gradleRoot = Join-Path $developRoot "gradle-8.7"
$javaHome = Join-Path $developRoot "jdk-21"
$gradleUserHome = Join-Path $developRoot ".gradle"
$androidUserHome = Join-Path $developRoot ".android"
$downloads = Join-Path $developRoot "_downloads"
$cmdlineZip = Join-Path $downloads "commandlinetools-win-14742923_latest.zip"
$cmdlineUrl = "https://dl.google.com/android/repository/commandlinetools-win-14742923_latest.zip"

function Require-Path($path, $name) {
    if (-not (Test-Path $path)) {
        throw "$name not found: $path"
    }
}

function Copy-Tree($source, $target) {
    if (-not (Test-Path $target)) {
        New-Item -ItemType Directory -Path $target | Out-Null
    }

    robocopy $source $target /E /R:2 /W:2 /NFL /NDL /NP
    $code = $LASTEXITCODE
    if ($code -gt 7) {
        throw "robocopy failed from $source to $target with exit code $code"
    }
}

function Install-CmdlineTools {
    $sdkManager = Join-Path $sdkRoot "cmdline-tools\latest\bin\sdkmanager.bat"
    if (Test-Path $sdkManager) {
        return
    }

    New-Item -ItemType Directory -Path $downloads -Force | Out-Null
    Invoke-WebRequest -Uri $cmdlineUrl -OutFile $cmdlineZip

    $extractRoot = Join-Path $downloads "cmdline-tools-extract"
    if (Test-Path $extractRoot) {
        Remove-Item -LiteralPath $extractRoot -Recurse -Force
    }
    Expand-Archive -Path $cmdlineZip -DestinationPath $extractRoot -Force

    $latestRoot = Join-Path $sdkRoot "cmdline-tools\latest"
    if (Test-Path $latestRoot) {
        Remove-Item -LiteralPath $latestRoot -Recurse -Force
    }
    New-Item -ItemType Directory -Path (Split-Path $latestRoot) -Force | Out-Null

    $extractedCmdline = Join-Path $extractRoot "cmdline-tools"
    Require-Path $extractedCmdline "Extracted command-line tools"
    Move-Item -LiteralPath $extractedCmdline -Destination $latestRoot
}

function Add-PathEntries($entries) {
    $current = [Environment]::GetEnvironmentVariable("Path", "User")
    $parts = @()
    if ($current) {
        $parts = $current.Split(";") | Where-Object { $_ -ne "" }
    }

    foreach ($entry in [array]::Reverse($entries.Clone())) {
        if ($parts -notcontains $entry) {
            $parts = @($entry) + $parts
        }
    }

    [Environment]::SetEnvironmentVariable("Path", ($parts -join ";"), "User")
}

Require-Path $javaSource "Existing JDK"
Require-Path $sdkSource "Existing Android SDK"
Require-Path $gradleSource "Existing Gradle"

New-Item -ItemType Directory -Path $developRoot -Force | Out-Null
New-Item -ItemType Directory -Path $gradleUserHome -Force | Out-Null
New-Item -ItemType Directory -Path $androidUserHome -Force | Out-Null

Copy-Tree $sdkSource $sdkRoot
Copy-Tree $gradleSource $gradleRoot
Copy-Tree $javaSource $javaHome
Install-CmdlineTools

[Environment]::SetEnvironmentVariable("JAVA_HOME", $javaHome, "User")
[Environment]::SetEnvironmentVariable("ANDROID_HOME", $sdkRoot, "User")
[Environment]::SetEnvironmentVariable("ANDROID_SDK_ROOT", $sdkRoot, "User")
[Environment]::SetEnvironmentVariable("GRADLE_HOME", $gradleRoot, "User")
[Environment]::SetEnvironmentVariable("GRADLE_USER_HOME", $gradleUserHome, "User")
[Environment]::SetEnvironmentVariable("ANDROID_USER_HOME", $androidUserHome, "User")

$pathEntries = @(
    (Join-Path $javaHome "bin"),
    (Join-Path $gradleRoot "bin"),
    (Join-Path $sdkRoot "platform-tools"),
    (Join-Path $sdkRoot "cmdline-tools\latest\bin"),
    (Join-Path $developRoot "androidStudio\bin")
)
Add-PathEntries $pathEntries

$env:JAVA_HOME = $javaHome
$env:ANDROID_HOME = $sdkRoot
$env:ANDROID_SDK_ROOT = $sdkRoot
$env:GRADLE_HOME = $gradleRoot
$env:GRADLE_USER_HOME = $gradleUserHome
$env:ANDROID_USER_HOME = $androidUserHome
$env:Path = ($pathEntries -join ";") + ";" + $env:Path

Write-Host "JAVA_HOME=$env:JAVA_HOME"
Write-Host "ANDROID_HOME=$env:ANDROID_HOME"
Write-Host "GRADLE_HOME=$env:GRADLE_HOME"
& (Join-Path $gradleRoot "bin\gradle.bat") -version
& (Join-Path $sdkRoot "cmdline-tools\latest\bin\sdkmanager.bat") --version
& (Join-Path $sdkRoot "platform-tools\adb.exe") version
