param(
    [ValidateSet('equipment', 'laser', 'visual', 'all')][string]$Suite = 'equipment',
    [string]$Case = 'all',
    [string]$World = 'verification'
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$verification = Join-Path $projectRoot 'build/api-verification'
$runDirectory = Join-Path $projectRoot 'runs/laser-client'
$metadata = Join-Path $verification 'metadata/META-INF'
$addon = Join-Path $runDirectory 'mods/api-verification-addon.jar'
if (!(Test-Path -LiteralPath (Join-Path $runDirectory "saves/$World/level.dat"))) {
    throw "Create the disposable test world '$World' in runs/laser-client before running this check."
}
[IO.Directory]::CreateDirectory($metadata) | Out-Null
[IO.Directory]::CreateDirectory((Split-Path -Parent $addon)) | Out-Null
[IO.File]::WriteAllText((Join-Path $metadata 'neoforge.mods.toml'), @'
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
'@)
Push-Location $projectRoot
try {
    & .\gradlew.bat compileAddonExampleJava --offline --console=plain -q
    if ($LASTEXITCODE) { throw 'Verification addon compilation failed.' }
    & jar --create --file $addon -C build/classes/java/addonExample . -C (Split-Path -Parent $metadata) META-INF
    if ($LASTEXITCODE) { throw 'Verification addon packaging failed.' }
    $suites = if ($Suite -eq 'all') { @('equipment', 'laser') } else { @($Suite) }
    foreach ($name in $suites) {
        $initScript = Join-Path $verification "client-$name.gradle"
        $escapedWorld = $World.Replace('\', '\\').Replace("'", "\'")
        $escapedCase = $Case.Replace('\', '\\').Replace("'", "\'")
        [IO.File]::WriteAllText($initScript, @"
gradle.projectsEvaluated {
    rootProject.tasks.named('runClient') {
        workingDir rootProject.file('runs/laser-client')
        systemProperty 'ic2.$name.verify', 'true'
        systemProperty 'ic2.equipment.case', '$escapedCase'
        args '--quickPlayPath', 'quickplay/log.json', '--quickPlaySingleplayer', '$escapedWorld'
    }
}
"@)
        & .\gradlew.bat runClient --offline --console=plain --init-script $initScript -x neoFormJoined1.21.1-20240808.144430DownloadAssets
        $clientExit = $LASTEXITCODE
        $logPath = Join-Path $runDirectory 'logs/latest.log'
        Copy-Item -LiteralPath $logPath -Destination (Join-Path $verification "client-$name.log")
        if ($clientExit) { throw "$name client exited with code $clientExit." }
        $log = Get-Content -LiteralPath $logPath -Raw
        $marker = switch ($name) {
            'equipment' { 'IC2 equipment integrated-client fixtures passed:' }
            'laser' { 'IC2 laser client checks passed: all eight modes' }
            'visual' { 'IC2 equipment visual fixture ready:' }
        }
        if (!$log.Contains($marker)) { throw "$name client did not report success. Inspect build/api-verification/client-$name.log." }
        Write-Output "$name integrated-client checks passed."
    }
} finally {
    if (Test-Path -LiteralPath $addon) { Remove-Item -LiteralPath $addon }
    Pop-Location
}
