param(
    [Parameter(Mandatory)] [string] $WorldPath,
    [string] $NeoProfile = 'neoforge-21.1.238',
    [switch] $KeepOpen,
    [ValidatePattern('^[a-zA-Z0-9_-]+$')] [string] $RunName = 'processing-gallery'
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$run = Join-Path $root "runs/$RunName"
$metadata = Join-Path $root 'build/remastered-verification/META-INF'
New-Item -ItemType Directory -Force -Path $metadata,(Join-Path $run 'saves'),(Join-Path $run 'mods') | Out-Null
$world = Join-Path $run 'saves/verification'
if (!(Test-Path -LiteralPath $world)) {
    Copy-Item -LiteralPath (Resolve-Path -LiteralPath $WorldPath).Path -Destination $world -Recurse
}
'pauseOnLostFocus:false','guiScale:2','soundCategory_master:0.0','lang:ru_ru','tutorialStep:none' |
    Set-Content -LiteralPath (Join-Path $run 'options.txt') -Encoding utf8NoBOM
@'
modLoader="javafml"
loaderVersion="[1,)"
license="All Rights Reserved"
[[mods]]
modId="exampleaddon"
version="1.0.0"
displayName="IC2 Remastered Client Verification"
'@ | Set-Content -LiteralPath (Join-Path $metadata 'neoforge.mods.toml') -Encoding utf8NoBOM
$addon = Join-Path $run 'mods/remastered-checks.jar'
$previousOptions = $env:JAVA_TOOL_OPTIONS
Push-Location $root
try {
    & ./gradlew.bat jar compileAddonExampleJava --offline --console=plain -q
    if ($LASTEXITCODE -ne 0) { throw 'Compilation failed' }
    & jar --create --file $addon -C build/classes/java/addonExample . -C (Split-Path -Parent $metadata) META-INF
    if ($LASTEXITCODE -ne 0) { throw 'Verification addon packaging failed' }
    $env:JAVA_TOOL_OPTIONS = "$previousOptions -Dic2.processing.verify=true"
    if ($KeepOpen) { $env:JAVA_TOOL_OPTIONS += ' -Dic2.processing.keepOpen=true' }
    $version = [regex]::Match((Get-Content build.gradle -Raw), "(?m)^version = '([^']+)'").Groups[1].Value
    $process = & "$PSScriptRoot/Start-LocalClient.ps1" -ModJar (Join-Path $root "build/libs/ic2-classic-re-$version.jar") -NeoProfile $NeoProfile -RunName $RunName -QuickPlayWorld 'verification'
    $deadline = [DateTime]::UtcNow.AddMinutes(5)
    while (!$process.WaitForExit(1000)) {
        if ($KeepOpen -and (Test-Path -LiteralPath (Join-Path $run 'console.log'))) {
            $currentLog = Get-Content -LiteralPath (Join-Path $run 'console.log') -Raw
            if ($currentLog -and $currentLog.Contains('IC2 processing gallery checks passed:')) {
                $leaveRunning = $true
                Write-Output "Remastered verification passed; Minecraft left running (PID $($process.Id))."
                return
            }
        }
        if ([DateTime]::UtcNow -gt $deadline) { throw 'Remastered verification timed out' }
    }
    $log = Get-Content -LiteralPath (Join-Path $run 'console.log') -Raw
    if (!$log.Contains('IC2 processing gallery checks passed:') -or $log.Contains('Failed to load texture: ic2:')) {
        throw "Remastered verification failed; inspect runs/$RunName/console.log"
    }
    Write-Output "Remastered verification passed; screenshots in runs/$RunName/screenshots"
} finally {
    $env:JAVA_TOOL_OPTIONS = $previousOptions
    if ($process -and !$process.HasExited -and !$leaveRunning) { Stop-Process -Id $process.Id }
    Pop-Location
}
