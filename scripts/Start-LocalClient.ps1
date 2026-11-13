param(
    [Parameter(Mandatory)] [string] $ModJar,
    [string] $NeoProfile = 'neoforge-21.1.238',
    [ValidatePattern('^[a-zA-Z0-9_-]+$')] [string] $RunName = 'smoke-client',
    [string] $QuickPlayWorld,
    [switch] $VerifyGeneration,
    [string] $Install = (Join-Path $env:USERPROFILE 'curseforge/minecraft/Install')
)

$ErrorActionPreference = 'Stop'
$neo = Get-Content -LiteralPath (Join-Path $Install "versions/$NeoProfile/$NeoProfile.json") -Raw | ConvertFrom-Json
$version = $neo.inheritsFrom
$vanilla = Get-Content -LiteralPath (Join-Path $Install "versions/$version/$version.json") -Raw | ConvertFrom-Json
$libraryDirectory = Join-Path $Install 'libraries'
$run = Join-Path (Split-Path -Parent $PSScriptRoot) "runs/$RunName"
$modDirectory = Join-Path $run 'mods'
$nativesDirectory = Join-Path $run 'natives'
New-Item -ItemType Directory -Path $modDirectory,$nativesDirectory -Force | Out-Null
Copy-Item -LiteralPath $ModJar -Destination (Join-Path $modDirectory 'ic2classic-local.jar') -Force
$optionsPath = Join-Path $run 'options.txt'
$options = if (Test-Path -LiteralPath $optionsPath) {
    Get-Content -LiteralPath $optionsPath | Where-Object { $_ -notmatch '^soundCategory_master:' }
}
(@($options) + 'soundCategory_master:0.0') | Set-Content -LiteralPath $optionsPath -Encoding utf8NoBOM

$seen = [Collections.Generic.HashSet[string]]::new()
$classpath = foreach ($library in @($neo.libraries) + @($vanilla.libraries)) {
    if ($library.rules -and !($library.rules | Where-Object { $_.action -eq 'allow' -and $_.os.name -eq 'windows' })) {
        continue
    }
    $group, $artifact, $revision, $classifier = $library.name -split ':'
    if ($classifier -in 'natives-windows-arm64','natives-windows-x86') { continue }
    if ($seen.Add("$group/$artifact/$classifier")) {
        $path = Join-Path $libraryDirectory $library.downloads.artifact.path
        if (!(Test-Path -LiteralPath $path -PathType Leaf)) { throw "Missing Minecraft library $($library.name)" }
        $path
    }
}
$classpath += Join-Path $Install "versions/$NeoProfile/$NeoProfile.jar"
$values = @{
    version_name = $NeoProfile
    library_directory = $libraryDirectory
    classpath_separator = ';'
    natives_directory = $nativesDirectory
    launcher_name = 'IC2LocalSmoke'
    launcher_version = '1'
    classpath = $classpath -join ';'
    auth_player_name = 'IC2Smoke'
    game_directory = $run
    assets_root = Join-Path $Install 'assets'
    assets_index_name = $vanilla.assets
    auth_uuid = '00000000000000000000000000000000'
    auth_access_token = '0'
    clientid = '0'
    auth_xuid = '0'
    user_type = 'legacy'
    version_type = 'release'
}

$arguments = @('-Xmx3G') + @($neo.arguments.jvm) + @($vanilla.arguments.jvm | Where-Object { $_ -is [string] })
if ($VerifyGeneration) { $arguments += '-Dic2.client.verify=true' }
$arguments += @($neo.mainClass) + @($neo.arguments.game) + @($vanilla.arguments.game | Where-Object { $_ -is [string] })
if ($VerifyGeneration) { $arguments += @('--width','1280','--height','900') }
if ($QuickPlayWorld) {
    $arguments += @('--quickPlayPath','quickplay/log.json','--quickPlaySingleplayer',$QuickPlayWorld)
}
$argFile = Join-Path $run 'java-args.txt'
$arguments | ForEach-Object {
    $argument = $_
    $values.GetEnumerator() | ForEach-Object {
        $argument = $argument.Replace(('$' + '{' + $_.Key + '}'), [string] $_.Value)
    }
    '"' + $argument.Replace('\', '\\').Replace('"', '\"') + '"'
} | Set-Content -LiteralPath $argFile -Encoding utf8NoBOM

$java = (Get-Command java.exe).Source
Start-Process -FilePath $java -ArgumentList ('@"' + $argFile + '"') -WorkingDirectory $run -WindowStyle Hidden -RedirectStandardOutput (Join-Path $run 'console.log') -RedirectStandardError (Join-Path $run 'console-error.log') -PassThru
