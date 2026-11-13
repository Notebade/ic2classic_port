param(
    [Parameter(Mandatory)]
    [string] $OriginalJar
)

$ErrorActionPreference = 'Stop'
$jarPath = (Resolve-Path -LiteralPath $OriginalJar).Path
$resources = Join-Path (Split-Path -Parent $PSScriptRoot) 'src/main/resources'
$models = Join-Path $resources 'assets/ic2/models'

$textureNames = foreach ($file in (Get-ChildItem -LiteralPath $models -Recurse -Filter '*.json' -File)) {
    $model = Get-Content -LiteralPath $file.FullName -Raw | ConvertFrom-Json
    if ($null -eq $model.textures) { continue }
    foreach ($texture in $model.textures.PSObject.Properties.Value) {
        if ($texture -cmatch '^ic2:((?:block|item)/[a-z0-9_/-]+)$') {
            "assets/ic2/textures/$($Matches[1]).png"
        }
    }
}
if (!$textureNames) { throw 'No original texture references found in the port models.' }

$runtimeTextures = @{
    'assets/ic2/textures/models/armor/hazmat_1.png' = 'assets/ic2/textures/models/armor/hazmat_layer_1.png'
    'assets/ic2/textures/models/armor/hazmat_2.png' = 'assets/ic2/textures/models/armor/hazmat_layer_2.png'
    'assets/ic2/textures/mob_effect/radiation.png' = 'assets/ic2/textures/mob_effect/radiation.png'
    'assets/ic2/textures/block/fluids/steam_still.png' = 'assets/ic2/textures/block/fluids/steam_still.png'
    'assets/ic2/textures/block/fluids/steam_still.png.mcmeta' = 'assets/ic2/textures/block/fluids/steam_still.png.mcmeta'
    'assets/ic2/textures/block/fluids/woodgas_still.png' = 'assets/ic2/textures/block/fluids/woodgas_still.png'
    'assets/ic2/textures/block/fluids/woodgas_still.png.mcmeta' = 'assets/ic2/textures/block/fluids/woodgas_still.png.mcmeta'
    'assets/ic2/textures/block/fluids/base_fluid_still.png' = 'assets/ic2/textures/block/fluids/base_fluid_still.png'
    'assets/ic2/textures/block/fluids/base_fluid_still.png.mcmeta' = 'assets/ic2/textures/block/fluids/base_fluid_still.png.mcmeta'
}
foreach ($rotor in @('wood','wool','iron','carbon','alloy','iridium')) {
    $path = "assets/ic2/textures/models/windmill/${rotor}_rotor.png"
    $runtimeTextures[$path] = $path
}
foreach ($sound in @('generators/generator_operating','generators/windmill_operating','generators/reactor/nuclear_reactor_operating')) {
    $path = "assets/ic2/sounds/$sound.ogg"
    $runtimeTextures[$path] = $path
}
foreach ($sound in @('geiger_low','geiger_med','geiger_high')) {
    $path = "assets/ic2/sounds/generators/reactor/$sound.ogg"
    $runtimeTextures[$path] = $path
}
foreach ($gui in @('generators/gui_solar_turbine','generators/gui_slag_generator','generators/gui_windmill',
    'generators/gui_liquid_generator','generators/gui_fuel_boiler','machines/ev/gui_plasmafier','machines/luv/gui_fusion_reactor','misc/gui_monitor')) {
    $path = "assets/ic2/textures/gui_sprites/blocks/$gui.png"
    $runtimeTextures[$path] = $path
}
$textureNames = @((@($textureNames) + @($runtimeTextures.Keys)) | Sort-Object -Unique)

Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [IO.Compression.ZipFile]::OpenRead($jarPath)
try {
    $entries = foreach ($name in $textureNames) {
        $entry = $archive.GetEntry($name)
        if ($null -eq $entry) { throw "Missing $name in the original JAR. Use IC2 Classic 1.19.2-2.1.3.3." }
        $entry
    }
    foreach ($entry in $entries) {
        $destination = if ($runtimeTextures.ContainsKey($entry.FullName)) { $runtimeTextures[$entry.FullName] } else { $entry.FullName }
        $path = Join-Path $resources $destination
        New-Item -ItemType Directory -Path (Split-Path -Parent $path) -Force | Out-Null
        [IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $path, $true)
        if ($entry.FullName.EndsWith('.png')) {
            $animation = $archive.GetEntry("$($entry.FullName).mcmeta")
            if ($null -ne $animation -and !(Test-Path -LiteralPath "$path.mcmeta")) {
                [IO.Compression.ZipFileExtensions]::ExtractToFile($animation, "$path.mcmeta", $true)
            }
        }
    }
} finally {
    $archive.Dispose()
}
Write-Output "Imported $($textureNames.Count) local textures."
