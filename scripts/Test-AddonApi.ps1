$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$verification = Join-Path $projectRoot 'build/api-verification'
$runDirectory = Join-Path $projectRoot 'runs/addon-api-check'
$metadata = Join-Path $verification 'metadata/META-INF'
$addon = Join-Path $runDirectory 'mods/api-verification-addon.jar'
[IO.Directory]::CreateDirectory($metadata) | Out-Null
[IO.Directory]::CreateDirectory((Split-Path -Parent $addon)) | Out-Null
$manifest = @'
modLoader = "javafml"
loaderVersion = "[1,)"
license = "All Rights Reserved"
[[mods]]
modId = "exampleaddon"
version = "1.0.0"
displayName = "IC2 API Verification Addon"
[[dependencies.exampleaddon]]
modId = "ic2"
type = "required"
versionRange = "[0.71.0,0.72.0)"
ordering = "AFTER"
side = "BOTH"
'@
[IO.File]::WriteAllText((Join-Path $metadata 'neoforge.mods.toml'), $manifest)
[IO.File]::WriteAllText((Join-Path $runDirectory 'server.properties'), "server-ip=127.0.0.1`nserver-port=0`nonline-mode=false`nview-distance=2`nsimulation-distance=2`n")
$initScript = Join-Path $verification 'run-api.gradle'
[IO.File]::WriteAllText($initScript, @'
gradle.projectsEvaluated {
    rootProject.tasks.named('runServer') {
        workingDir rootProject.file('runs/addon-api-check')
        systemProperty 'ic2.api.verify', 'true'
    }
}
'@)
Push-Location $projectRoot
try {
    & .\gradlew.bat compileAddonExampleJava --offline --console=plain -q
    if ($LASTEXITCODE -ne 0) { throw 'Verification addon compilation failed.' }
    & jar --create --file $addon -C build/classes/java/addonExample . -C (Split-Path -Parent $metadata) META-INF
    if ($LASTEXITCODE -ne 0) { throw 'Verification addon packaging failed.' }
    & .\gradlew.bat runServer --offline --console=plain -q --init-script $initScript -x neoFormJoined1.21.1-20240808.144430DownloadAssets
    if ($LASTEXITCODE -ne 0) { throw 'Verification server failed.' }
    $log = Get-Content -LiteralPath (Join-Path $runDirectory 'logs/latest.log') -Raw
    if (!$log.Contains('IC2 API runtime smoke checks passed') -or $log.Contains('IC2 API runtime smoke checks FAILED')) {
        throw 'API runtime checks did not pass. Inspect runs/addon-api-check/logs/latest.log.'
    }
    Write-Output 'API addon verification passed. Production IC2 JAR was not built.'
} finally {
    if (Test-Path -LiteralPath $addon) { Remove-Item -LiteralPath $addon }
    Pop-Location
}
