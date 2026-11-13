# Equipment and crafting migration

Requested order: remaining-work row 8, then row 7. Local commits only; no push.
Client screens, rendering, language and creative classification remain with the graphics worktree.

## Conventional equipment

- Read the supplied mapped 2.1.3.3 sources for tool tiers, constructors, armor materials, shields and recipes.
- Added ten bronze/aluminium tools using native tool actions, original attributes, harvest tiers, durability and matching-ingot repair. Original shaped recipes, recipe discovery and vanilla tool tags accompany them.
- Added four bronze and four composite armor pieces with original defense, toughness, durability and repair materials; original recipes include both composite ingredient orderings. Added native armor tags for enchantment compatibility.
- Added bronze/aluminium/composite shields with native blocking and original durability/repair. Nano shield implements the public ElectricItem interface, needs 50 EU to block and spends 50 EU per point of shield wear; depletion cancels use.
- Red evidence: equipment-conventional-red.log, equipment-armor-red.log, equipment-shield-red.log, equipment-nano-shield-red.log under .tools. Each fails on the intended absent item.
- Server checks exercise actual tool mining, recipes, armor values, shield use and electric damage conversion. Full addon suite also checks prior machinery. Final unit evidence recorded after verification.

Rows 8 and 7 remain open. No whole-stage completion or shutdown until both are finished.

First unit verified: .tools/equipment-first-unit-check.log (full server/addon suite, real recipe lookup) and .tools/equipment-first-unit-build.log (check + compileAddonExampleJava). No client assets changed. Nano shield enchantment damage follows the native ItemStack wear path; it never loses durability.

## Drill variants and counted recipes

- Added diamond, advanced and rock-cutter drills using original EU cost, charging tier, speed and machine mining boosts. Configurable modes persist; sneaking right-click cycles them. ConfigurableTool is the addon contract for mode discovery/change.
- Advanced area mining uses the native server block-destruction path. Real 3x3 test protects one block through BreakEvent, verifies eight removals and exactly 640 EU spent. No neighbor chunk loading. Rock-cutter tag uses the source nine blocks.
- Restored normal, silk-touch and Fortune III source recipes. Fortune requires five advanced circuits, three end crystals per crystal slot and five magnets. Added reusable ic2:sized_shaped so native crafting and vanilla Crafter consume original counts; cache bypass handles counted stacks.
- Red: .tools/equipment-drills-red.log and .tools/equipment-fortune-red.log. Green: .tools/equipment-drills-final-check.log and .tools/equipment-fortune-take-check.log. Actual CraftingMenu take verifies full ingredient consumption and Fortune III assembly.
- Source drill/scanner probing and audio remain for the equipment audit; row 8 stays open.
- Auto-review rejected reverting JSON field-order churn because it might discard uncommitted recipes. Kept the files; no restore was executed.

## Electric hoes and chainsaws

- Added electric/advanced hoes with original capacity, tier, 50/100 EU per till, persisted radius 0..5 and native HOE_TILL events. Each target gets its own context, permission and loaded-chunk checks.
- Added both chainsaws: source charging/speed, native stripping, real IShearable entity support, intact leaf/web drops and source powered combat. Advanced saw fells connected matching logs only when an attached crown is found; native server destruction preserves block protection and actual per-log charging.
- Shared tool-mode serialization removes duplicate drill/hoe logic. Added source attack attributes and fixed zero-hardness mining to consume no EU.
- Red: equipment-hoes-red.log, equipment-chainsaws-red.log, equipment-zero-hardness-red.log, equipment-chainsaw-combat-red.log. Green: equipment-hoes-check.log, equipment-utility-check.log, equipment-chainsaw-tree-check.log. Tests use real farmland, sheep shearing/combat, block loot and native tree destruction. Sound/probing/global equipment audit remains open.

## Toolboxes: storage and upgrades

- Added carbon_box (15) and iridium_box (45), retained toolbox (8), tool-only insertion, non-nesting and detached public storage reads. Legacy non-tool contents remain extractable, never deleted on opening or upgrading.
- Added Toolbox and BoxableItem public contracts plus ic2:toolbox_tools tag admission. Native tool/electric classes are recognized; addon-specific stacks can opt in. Extended boxes cycle tools with ownership checks; Shift+right-click exchanges the other held tool.
- Backend menus reuse native container types, retain inaccessible padding and lock the held box. Native shift-click tests verify insertion/extraction and persistence for all three capacities; no client screen code changed.
- Source nano_toolbox/quantum_toolbox recipes transfer contents, including charge components, without mutating inputs. ic2:toolbox_upgrade is a native shaped-recipe serializer with inventory transfer.
- Red: equipment-toolboxes-red.log, equipment-toolbox-upgrade-red.log. Green: equipment-toolbox-menu-check.log covers all prior checks, admission, ownership, scroll conservation, all menus and both upgrade chains.
- Diagnostic/food-can delegation and source mouse-wheel binding remain open; public scroll/backend storage are ready for that integration.

## Mowing tool

- Added mowing_tool, source recipe and persistent off/on modes through ConfigurableTool. Right-click toggles; selected/offhand server ticks act every five ticks.
- Clears the original 3x3x3 bush/leaf targets while excluding CropBlock, uses native protected destruction and imports loot through BlockDropsEvent. Full inventory leaves excess drops to native world spawning. AXE_SCRAPE behavior is retained.
- Source block cost is 5 EU; manual nonzero-hardness mining uses 1 EU and the automatic harvesting guard prevents double debit. Entity ray hits now spend the advertised 1 EU; the original omitted this debit, which permitted free damage while holding charge.
- Red: equipment-mower-red.log and equipment-mower-manual-red.log. Green: equipment-mower-final-check.log. Checks cover activation, empty denial, protection, crop exclusion, seven exact imports/35 EU, full-inventory overflow, copper scraping, real entity damage and manual mining.
- Overflow fixture initially queried a hidden entity section before the first server tick. Primary entity-manager source confirmed that lookup excludes such sections. The test now observes actual uncancelled entity-spawn events; the ray target explicitly participates in entity lookup. No production loot workaround was needed.

## Wearable energy and source charging

- Added source batpack/lappack/quantum_pack/industrial_belt capacities, rates, tiers, armor slots and zero passive defense. Native equipment use is exercised rather than merely testing registration.
- ElectricEquipment is the public server contract: pure affordability, original Efficiency/Unbreaking truncation, worn-provider refill before/after consumption, and failed-use charge preservation. Armor refill intentionally bypasses charger rate/packet limits as in the source manager; only provider items can supply EU.
- Integrated native drills, saws, hoes, mower, wrench, tap and scanner use. Nano shield preserves partial-debit semantics while applying source enchantment cost. EuCharge centralizes the native component storage used by batteries, armor and shield.
- Source ShapedIC2Recipe inspection found automatic ingredient-EU transfer. Added ic2:electric_shaped, reused transfer for counted shapes, restored four wearable recipes and belt recovery, and converted existing electric-result source recipes. Transfer is capped by result capacity; a multi-item recovery result stays uncharged as in the source.
- Red: equipment-packs-red.log, equipment-native-energy-red.log, equipment-charge-crafting-red.log. Green: equipment-packs-complete-check.log checks worn use/refill conservation, real charged recipe take, ingredient consumption, enchantment costs and every previous addon scenario. Optional crapper/dryer/mutator tools were searched and are unavailable.

## Solar helmets and recipe ingredient audit

- Added solar_helmet/advanced_solar_helmet: one armor point, original 1/5 EU per player tick, source sky/day/biome-weather rules and original three recipes. They generate into worn stores rather than acting as batteries.
- Added public ElectricEquipment.offerToArmor, returning accepted EU and respecting per-store transfer rate/capacity. Shared EquipmentMaterials removes duplicate battery/solar material construction.
- Initial fixture roof assertion observed stale skylight before the light thread propagated a newly placed roof. The test now fences native light updates with a bounded future, preserving the actual source visibility check. Day, roof, night, precipitation and full-store checks pass.
- Global crafting-ingredient validation discovered missing c:ingots/refined_iron and c:rubber leaves in four newly added recipes. equipment-empty-tags-red.log reproduces an unusable chainsaw recipe; both native tags are now present and every loaded IC2 crafting ingredient is checked for nonempty resolution.
- equipment-solar-armor-check.log exposed Gradle's default heap exhaustion. Isolated 2 GB compilation succeeded; gradle.properties now sets that heap for reproducible builds without stopping other processes.
- Full green: equipment-solar-recipes-complete-check.log. Source module-install behavior and remaining equipment are still part of the open row 8 audit.

## Static boots and ordered armor callbacks

- Added static_boots, both original recipes, one armor point, walking/carpet generation and ten-tick transfer to worn stores. Generation stops at the source threshold 20; its final increment may reach 22. This private generation buffer is not an externally chargeable battery.
- GeneratedArmor exposes that buffer without inventing ElectricItem compatibility. ArmorTickItem provides the original server-worn callback order feet/legs/chest/head to native and addon items.
- Fixed the source stale-local-charge bug: simultaneous transfer and movement no longer recreate already transferred EU. Airborne/riding/water state resets the anchor, including while the buffer is full.
- Combined boots/solar test reproduced wrong independently registered callback order at a nearly full store. Central ordered callbacks restore source priority and avoid discarding solar production while unnecessarily retaining old static charge.
- Red: equipment-static-boots-red.log and equipment-armor-tick-order-red.log. Green: equipment-static-order-check.log covers actual movement, carpet, airborne reset, source 22-EU ceiling, detached copies, same-tick conservation and combined generation plus all prior suites.

## Per-stack electric parameters

- Source modular armor derives storage, transfer, tier and provider status from installed modules. Added additive ElectricItem overloads taking ItemStack; fixed-storage addons retain their original parameterless methods and defaults.
- Migrated every native generic EU consumer, battery slot, charger, generator, machine/chemistry/mining family, portable/disposable charging, recipe transfer and worn-energy helper to stack parameters. Client screens were not edited.
- Added a separately registered addon item whose baseline capacity/rate are zero but individual stacks expose 200/2000 EU, 7/40 EU rates, 32/512 EU packet requirements and switchable provider status.
- equipment-dynamic-energy-red.log reproduces baseline rejection. equipment-dynamic-energy-complete-check.log verifies native bench charge/discharge, completed extraction, source packet checks, portable/disposable use, armor refill and failed oversized use, alongside all fixed-storage regressions.
- Initial bench assertion wrongly assumed receiveEnergy(1000) bypassed a 32-EU input packet; corrected the fixture to supply a legal packet. Production packet enforcement was retained.
- Source findings for the next unit: native armor batteries are energy_crystal (armor rate250), lapatron_crystal(1000), glowtronic_crystal(5000), uesc(10000). RE/PESD/quantum accumulators have no armor-battery transfer and are not installable by default. Nano/quantum module slots, provider modifiers and damage contracts must preserve per-stack behavior.
