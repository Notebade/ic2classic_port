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
    }
} finally {
    $archive.Dispose()
}
Write-Output "Imported $($textureNames.Count) local textures."
