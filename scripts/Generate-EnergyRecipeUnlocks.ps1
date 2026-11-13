$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$recipes = Join-Path $root 'src/main/resources/data/ic2/recipe'
$destination = Join-Path $root 'src/main/resources/data/ic2/advancement/recipes/energy_advanced'
[IO.Directory]::CreateDirectory($destination) | Out-Null
$items = [Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
@('re_battery','energy_crystal','lapatron_crystal','glowtronic_crystal','uesc','pesd','quantum_accumulator','quantum_accumulator_big','su_battery',
  'batbox','mfe','mfsu','esu','isu','pesu','adjustable_transformer','induction_furnace','induction_blast_furnace','induction_smoker',
  'rotary_macerator','centrifugal_extractor','singularity_compressor','centrifugal_rare_earth_extractor','compacting_recycler','slow_grinder',
  'pressure_alloy_furnace','pressure_alloy_furnace_structure','charged_electrolyzer','electric_fisher','electric_enchanter','chunkloader','magnetizer','tesla_coil',
  'overclocked_pump','ranged_pump','miner','drill','od_scanner','ov_scanner','ov_scanner_ranged','fluid_scanner',
  'fluid_import_upgrade_simple','fluid_export_upgrade_simple','cobblegen_upgrade') | ForEach-Object { [void]$items.Add($_) }
function Get-DiscoveryItems($ingredient) {
    if ($ingredient -is [Collections.IDictionary]) {
        if ($ingredient.item) { [string]$ingredient.item }
        elseif ($ingredient.tag) { '#' + [string]$ingredient.tag }
        elseif ($ingredient.ingredient) { Get-DiscoveryItems $ingredient.ingredient }
    } elseif ($ingredient -is [array]) { foreach ($entry in $ingredient) { Get-DiscoveryItems $entry } }
}
$count = 0
foreach ($file in (Get-ChildItem -LiteralPath $recipes -Filter '*.json' | Sort-Object Name)) {
    $recipe = Get-Content -LiteralPath $file.FullName -Raw | ConvertFrom-Json -AsHashtable
    $output = if ($recipe.result -is [Collections.IDictionary]) { [string]$recipe.result.id } else { '' }
    if (!$output.StartsWith('ic2:')) { continue }
    $name = $output.Substring(4)
    if (!$items.Contains($name) -and $name -notmatch '^(chargepad_|charging_bench_|battery_station_|redirector_|transformer_|colossal_|upgrade_)' -and $name -notmatch '(_flux_generator|_cable_item)$') { continue }
    $ingredients = @(
        if ($recipe.key) { foreach ($entry in $recipe.key.Values) { Get-DiscoveryItems $entry } }
        if ($recipe.ingredients) { Get-DiscoveryItems $recipe.ingredients }
    ) | Sort-Object -Unique
    if (!$ingredients.Count) { continue }
    $recipeId = 'ic2:' + $file.BaseName
    $criteria = [ordered]@{ has_the_recipe = @{ trigger = 'minecraft:recipe_unlocked'; conditions = @{ recipe = $recipeId } } }
    $index = 0
    foreach ($ingredient in $ingredients) {
        $criteria['has_material_' + $index++] = @{ trigger = 'minecraft:inventory_changed'; conditions = @{ items = @(@{ items = $ingredient }) } }
    }
    $advancement = [ordered]@{ parent = 'minecraft:recipes/root'; criteria = $criteria; requirements = @(,@($criteria.Keys)); rewards = @{ recipes = @($recipeId) } }
    [IO.File]::WriteAllText((Join-Path $destination ($file.BaseName + '.json')), ($advancement | ConvertTo-Json -Depth 12) + "`n")
    $count++
}
Write-Output "Generated $count energy/advanced recipe discovery advancements."
