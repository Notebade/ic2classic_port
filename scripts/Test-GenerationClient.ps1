param(
    [Parameter(Mandatory)] [string] $OriginalJar,
    [Parameter(Mandatory)] [string] $WorldPath,
    [string] $JeiJar,
    [string] $NeoProfile = 'neoforge-21.1.238',
    [string] $Install = (Join-Path $env:USERPROFILE 'curseforge/minecraft/Install')
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$run = Join-Path $root 'runs/generation-client-check'
$metadata = Join-Path $root 'build/client-verification/META-INF'
$addon = Join-Path $run 'mods/client-checks.jar'
New-Item -ItemType Directory -Force -Path $metadata,(Join-Path $run 'saves'),(Split-Path -Parent $addon) | Out-Null
if (!(Test-Path -LiteralPath (Join-Path $run 'saves/verification'))) {
    Copy-Item -LiteralPath (Resolve-Path -LiteralPath $WorldPath).Path -Destination (Join-Path $run 'saves/verification') -Recurse
}
if ($JeiJar) { Copy-Item -LiteralPath $JeiJar -Destination (Join-Path $run 'mods') -Force }
'pauseOnLostFocus:false','guiScale:2','soundCategory_master:0.0' | Set-Content -LiteralPath (Join-Path $run 'options.txt') -Encoding utf8NoBOM
@'
modLoader="javafml"
loaderVersion="[1,)"
license="All Rights Reserved"
[[mods]]
modId="exampleaddon"
version="1.0.0"
displayName="IC2 Client Verification"
[[dependencies.exampleaddon]]
modId="ic2"
type="required"
versionRange="[0.70.0,0.71.0)"
ordering="AFTER"
side="BOTH"
'@ | Set-Content -LiteralPath (Join-Path $metadata 'neoforge.mods.toml') -Encoding utf8NoBOM
Push-Location $root
try {
    & "$PSScriptRoot/Import-OriginalTextures.ps1" -OriginalJar $OriginalJar
    & ./gradlew.bat jar compileAddonExampleJava --offline --console=plain -q
    if ($LASTEXITCODE -ne 0) { throw 'Client verification compilation failed.' }
    & jar --create --file $addon -C build/classes/java/addonExample . -C (Split-Path -Parent $metadata) META-INF
    if ($LASTEXITCODE -ne 0) { throw 'Client verification packaging failed.' }
    $version = [regex]::Match((Get-Content build.gradle -Raw), "(?m)^version = '([^']+)'").Groups[1].Value
    $process = & "$PSScriptRoot/Start-LocalClient.ps1" -ModJar (Join-Path $root "build/libs/ic2-classic-re-$version.jar") -NeoProfile $NeoProfile -RunName 'generation-client-check' -QuickPlayWorld 'verification' -Install $Install -VerifyGeneration
    $deadline = [DateTime]::UtcNow.AddMinutes(3)
    while (!$process.WaitForExit(1000)) {
        if ([DateTime]::UtcNow -gt $deadline) { throw 'Client verification timed out; inspect the isolated test window and log.' }
    }
    $log = Get-Content -LiteralPath (Join-Path $run 'console.log') -Raw
    if (!$log.Contains('IC2 client render checks passed: 22 screens') -or $log.Contains('Failed to load texture: ic2:')) {
        throw 'Client verification failed; inspect runs/generation-client-check/console.log.'
    }
    Write-Output 'Client verification passed: 22 screens, including charging pads, chunkloader and enrichment recipes in JEI, and monitor wall.'
} finally {
    if ($process -and !$process.HasExited) { Stop-Process -Id $process.Id -ErrorAction SilentlyContinue }
    if (Test-Path -LiteralPath $addon) { Remove-Item -LiteralPath $addon }
    Pop-Location
}
