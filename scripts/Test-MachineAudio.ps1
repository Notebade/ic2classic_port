param(
    [Parameter(Mandatory)] [string] $WorldPath,
    [Parameter(Mandatory)] [string] $JeiJar,
    [string] $NeoProfile = 'neoforge-21.1.238'
)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$run = Join-Path $root 'runs/audio-client-check'
$metadata = Join-Path $root 'build/audio-verification/META-INF'
$addon = Join-Path $run 'mods/audio-checks.jar'
New-Item -ItemType Directory -Force -Path $metadata,(Join-Path $run 'saves'),(Split-Path -Parent $addon) | Out-Null
if (!(Test-Path -LiteralPath (Join-Path $run 'saves/verification'))) {
    Copy-Item -LiteralPath (Resolve-Path -LiteralPath $WorldPath).Path -Destination (Join-Path $run 'saves/verification') -Recurse
}
Copy-Item -LiteralPath $JeiJar -Destination (Join-Path $run 'mods') -Force
'pauseOnLostFocus:false','soundCategory_master:0.03','soundCategory_blocks:1.0' | Set-Content -LiteralPath (Join-Path $run 'options.txt') -Encoding utf8NoBOM
@'
modLoader="javafml"
loaderVersion="[1,)"
license="All Rights Reserved"
[[mods]]
modId="exampleaddon"
version="1.0.0"
displayName="IC2 Audio Verification"
[[dependencies.exampleaddon]]
modId="ic2"
type="required"
versionRange="[0.65.0,0.71.0)"
ordering="AFTER"
side="BOTH"
'@ | Set-Content -LiteralPath (Join-Path $metadata 'neoforge.mods.toml') -Encoding utf8NoBOM
Push-Location $root
try {
    & .\gradlew.bat jar compileAddonExampleJava --offline --console=plain -q
    if ($LASTEXITCODE -ne 0) { throw 'Audio verification compilation failed.' }
    & jar --create --file $addon -C build/classes/java/addonExample . -C (Split-Path -Parent $metadata) META-INF
    if ($LASTEXITCODE -ne 0) { throw 'Audio verification addon packaging failed.' }
    $version = [regex]::Match((Get-Content build.gradle -Raw), "(?m)^version = '([^']+)'").Groups[1].Value
    $process = & "$PSScriptRoot/Start-LocalClient.ps1" -ModJar (Join-Path $root "build/libs/ic2-classic-re-$version.jar") -NeoProfile $NeoProfile -RunName 'audio-client-check' -QuickPlayWorld 'verification' -VerifyAudio
    $deadline = [DateTime]::UtcNow.AddMinutes(3)
    while (!$process.WaitForExit(1000)) { if ([DateTime]::UtcNow -gt $deadline) { throw 'Audio verification timed out.' } }
    $log = Get-Content -LiteralPath (Join-Path $run 'console.log') -Raw
    if (!$log.Contains('IC2 client audio checks passed:') -or $log.Contains('Unreported exception')) { throw 'Client audio verification failed; inspect runs/audio-client-check/console.log.' }
    Write-Output 'Client audio verification passed: actual playing loops, volume modifiers, mute/unmute and removal cleanup.'
} finally {
    if ($process -and !$process.HasExited) { Stop-Process -Id $process.Id -ErrorAction SilentlyContinue }
    if (Test-Path -LiteralPath $addon) { Remove-Item -LiteralPath $addon }
    Pop-Location
}
