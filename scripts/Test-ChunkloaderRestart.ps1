$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$verification = Join-Path $projectRoot 'build/chunkloader-verification'
$runDirectory = Join-Path $projectRoot 'runs/chunkloader-restart-check'
$metadata = Join-Path $verification 'metadata/META-INF'
$addon = Join-Path $runDirectory 'mods/api-verification-addon.jar'
[IO.Directory]::CreateDirectory($metadata) | Out-Null
[IO.Directory]::CreateDirectory((Split-Path -Parent $addon)) | Out-Null
$manifest = @'
modLoader="javafml"
loaderVersion="[1,)"
license="All Rights Reserved"
[[mods]]
modId="exampleaddon"
version="1.0.0"
displayName="IC2 Chunkloader Restart Verification"
[[dependencies.exampleaddon]]
modId="ic2"
type="required"
versionRange="[0.65.0,0.71.0)"
ordering="AFTER"
side="BOTH"
'@
[IO.File]::WriteAllText((Join-Path $metadata 'neoforge.mods.toml'), $manifest)
[IO.File]::WriteAllText((Join-Path $runDirectory 'server.properties'), "server-ip=127.0.0.1`nserver-port=0`nonline-mode=false`nview-distance=2`nsimulation-distance=2`n")
$existingEula = Join-Path $projectRoot 'runs/addon-api-check/eula.txt'
if (Test-Path -LiteralPath $existingEula) { Copy-Item -LiteralPath $existingEula -Destination (Join-Path $runDirectory 'eula.txt') }
$initScript = Join-Path $verification 'restart.gradle'
$template = @'
gradle.projectsEvaluated {
    rootProject.tasks.named('runServer') {
        workingDir rootProject.file('runs/chunkloader-restart-check')
        systemProperty 'ic2.chunkloader.phase', 'PHASE'
    }
}
'@
Push-Location $projectRoot
try {
    & .\gradlew.bat compileAddonExampleJava --offline --console=plain -q
    if ($LASTEXITCODE -ne 0) { throw 'Restart verification addon compilation failed.' }
    & jar --create --file $addon -C build/classes/java/addonExample . -C (Split-Path -Parent $metadata) META-INF
    if ($LASTEXITCODE -ne 0) { throw 'Restart verification addon packaging failed.' }
    foreach ($phaseName in @('save','restore','ticking')) {
        [IO.File]::WriteAllText($initScript, $template.Replace('PHASE', $phaseName))
        & .\gradlew.bat runServer --offline --console=plain -q --init-script $initScript -x neoFormJoined1.21.1-20240808.144430DownloadAssets
        if ($LASTEXITCODE -ne 0) { throw "Restart server failed: $phaseName" }
        $log = Get-Content -LiteralPath (Join-Path $runDirectory 'logs/latest.log') -Raw
        if (!$log.Contains("IC2 chunkloader restart checks passed: $phaseName") -or $log.Contains('IC2 chunkloader restart checks FAILED')) {
            throw "Restart checks failed: $phaseName"
        }
        Copy-Item -LiteralPath (Join-Path $runDirectory 'logs/latest.log') -Destination (Join-Path $verification "$phaseName.log")
    }
    Write-Output 'Chunkloader restart verification passed: three independent server processes, stale-ticket cleanup and ticking-mode persistence.'
} finally {
    if (Test-Path -LiteralPath $addon) { Remove-Item -LiteralPath $addon }
    Pop-Location
}
