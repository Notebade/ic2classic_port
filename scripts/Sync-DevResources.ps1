# Refresh the IntelliJ/NeoGradle resource directory without compiling Java or packaging a JAR.
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$source = Join-Path $root 'src/main/resources'
$destination = Join-Path $root 'build/resources/main'
$gradle = Get-Content -LiteralPath (Join-Path $root 'build.gradle') -Raw
$version = [regex]::Match($gradle, "(?m)^version = '([^']+)'").Groups[1].Value
$neoVersion = [regex]::Match($gradle, "(?m)^def neoForgeVersion = '([^']+)'").Groups[1].Value
if (!$version -or !$neoVersion) { throw 'Missing version metadata in build.gradle.' }
Get-ChildItem -LiteralPath $source -Recurse -Filter '*.json' -File | ForEach-Object {
    Get-Content -LiteralPath $_.FullName -Raw | ConvertFrom-Json -AsHashtable | Out-Null
}
New-Item -ItemType Directory -Path $destination -Force | Out-Null
Copy-Item -Path (Join-Path $source '*') -Destination $destination -Recurse -Force
$metadataPath = Join-Path $destination 'META-INF/neoforge.mods.toml'
$metadata = Get-Content -LiteralPath $metadataPath -Raw
$metadata = $metadata.Replace(('${modVersion}'), $version).Replace(('${neoForgeVersion}'), $neoVersion)
[IO.File]::WriteAllText($metadataPath, $metadata, [Text.UTF8Encoding]::new($false))
Write-Output "Development resources refreshed for $version. In a running client, press F3+T."
