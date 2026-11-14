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

### Modular armor foundation (14 November 2026)

- Added public `ArmorBattery`, `ArmorModule`, `ArmorEnergyModifier`, `InstalledArmorModule`, `ModularArmor` and `ProtectiveArmor` contracts. Module categories use resource IDs; addon modules provide pure modifiers/features and committed lifecycle callbacks. All module reads return detached copies.
- Native Nano suit has four diamond-material shells (20 armor / 8 toughness in a full set), default energy crystals, source normal/enhanced module limits and per-stack EU parameters. Only the four source-compatible crystal families opt into battery slots; their armor transfer rates are independent of portable charging rates.
- Installation consumes one item and moves its EU into armor. Removal simulation is pure; actual removal restores EU into the returned battery. Removing capacity is rejected if it would lose remaining EU. Enhancement preserves installed modules and charge.
- Protection runs at the actual vanilla armor phase, normalizes source ratios and spends integer wear energy. Charged full-set normal hit 10 spends exactly 6400 EU; excluded magic bypasses protection; empty shells retain physical defense. Corrected test fixture: vanilla `generic` bypasses physical armor, so the physical-defense test uses an actual mob attack.
- Eight original recipes and discovery advancements added. Default crafting installs the battery and carries ingredient EU; enhancement copies the complete source stack.
- Red evidence: `.tools/equipment-modular-armor-red.log` (missing armor), `.tools/equipment-modular-crafting-red.log` (missing original recipe). Green: `.tools/equipment-modular-complete-check.log` (including separately compiled addon lifecycle, real damage and stack serialization).
- Still open: source modules, Quantum suit, configurator, fall behavior, electrical protection audit and remaining equipment. This foundation does not close row 8. GUI/models remain with the separate graphics stage.
- Existing recipe-load error discovered: belt recovery `re_battery_1` returns three nonstackable batteries and vanilla strict result codec rejects it. Fix required before stage completion.
- Requested crapper/dryer/mutator tools are unavailable in this session; behavioral checks and local review used.

### Five passive modules and recipe batch recovery

- Implemented source IDs `base_module`, `air_refill_module`, `charger_module`, `protective_module`, `extinguish_module`, `golden_module`, original recipes and discovery. Charger enables real EU output; air adds 200 below 100 air for 2000 EU; protection removes poison/radiation/wither at 10000/20000/25000 per amplifier level; extinguishing clears fire without EU; golden exposes real piglin neutrality.
- Source air refill checked only 1000 EU before spending 2000. Native requires all 2000, preventing underpriced air. Source ANY-category protective/extinguish modules retain broad installation compatibility (their source `canEquip` methods do not override module admission).
- Strengthened recipe availability to require every IC2 JSON resource to exist in the loaded recipe manager. Red `.tools/equipment-recipe-loading-red.log` exposed belt recovery's rejected three-item nonstackable output.
- Added `ic2:batch_shapeless` with a legal single result plus `output_count`; inventory transfer splits an oversized crafting batch across legal individual slots. Real Shift-crafting a charged belt consumes it and yields exactly three separate uncharged batteries, preserving original recovery semantics. An initial actual-menu test caught vanilla's one-empty-slot transfer limitation; fixed at the shared backend transfer method.
- Green `.tools/equipment-passive-complete-check.log`: all recipes load, passive modules run through server armor ticks, real belt recovery passes. No production JAR built yet. GUI unchanged; row 8 and row 7 remain open.

### Night vision and equipment input

- Source `nightvision_goggles` is a zero-defense, non-durable helmet and GENERIC/HEAD module: 20000 EU, 200/tick charging rate, 32 packet, consumer only. Disabled by default; enabled uses 1 EU/tick, clears blindness, grants 250 ticks night vision. Disable/depletion leaves the existing effect to expire. Standalone goggles use normal worn-provider charging; installed goggles draw only from parent armor and retain their independent module battery.
- Added `EquipmentInput`/`EquipmentControls` public server contracts and `ic2:equipment_input` owner-only payload. Six boolean intents, invalid bits rejected, twenty-tick expiry, client changes/ten-tick refresh, NONE while screens open. Gameplay bindings M/H/left-control/left-alt plus vanilla jump/forward; no screen/HUD/model changes.
- HUD+Mode switches worn goggles with source twenty-tick cooldown, requiring no open server menu. Shift-use offers held toggling through the existing ConfigurableTool API. Module setting stays with the installed module and survives removal, avoiding hidden parent-state leftovers.
- Original recipe and missing clear reinforced glass dependency added, including its four-from-four source recipe and Silk Touch loot. Appearance belongs to graphics stage.
- Red `.tools/equipment-nightvision-red.log`: missing goggles. Green `.tools/equipment-nightvision-complete-check.log`: separate/module behavior, parent/independent EU, disabled/empty behavior, input mask/expiry and repeated-key cooldown. Fixture corrections: consumed module stacks become empty, so recipe assertion now compares a fresh goggles stack; world time setter belongs to ServerLevelData.

### Four movement modules

- Source IDs jump_boost_module, speed_boost_module, step_assist_module, parachute_module with source categories/slot restrictions and original shaped recipes plus alternate jump recipe.
- Jump reserves one boost for 1000 EU while grounded, then applies original default 1.75 initial horizontal multiplier / 0.15 decaying vertical force while Jump+Boost is held. Speed adds source ground/water/air acceleration for sprint or Boost+Forward, charging 1000 per ten active ticks.
- `ic2-equipment-server.toml` exposes source `originalQuantumBehavior=false`; true restores 3.5 initial multiplier, 0.3 vertical and 0.25 air acceleration.
- Step assist adds one transient +0.6 native STEP_HEIGHT modifier regardless of duplicate modules; refresh revokes it after module removal or unequip. Source periodic climbing cost 250 EU. It cannot leave a permanent saved player attribute.
- Parachute: Jump+Sneak activates while falling, limits descent to -0.2, applies 0.02 forward acceleration, clears fall distance and resets the server floating counter. Landing/water or held combination after five ticks disables it.
- Red `.tools/equipment-movement-red.log` missing jump module; green `.tools/equipment-movement-check.log` real player velocity/reserve EU, ten-tick speed EU, real step attribute add/remove and parachute activation/landing. Added alternate jump recipe after the run; covered by next full recipe-load run.
- Water walker remains pending its original carbon-boat recipe dependency. Quantum, storage, active utility modules and remaining row 8 still open; row 7 not started.

### Water walking and carbon boat dependency

- Added water_walker_module (MOVEMENT/FEET), its original carbon boat ingredient and both original recipes/discovery. Falling near a liquid surface is supported only with >=100 parent EU, without sneaking; source rate 100 EU each ten world ticks, native ground/motion reset and server floating-counter reset. Particle feedback uses native packets.
- Native carbon_boat entity/item preserves original vanilla rowing, custom item drop, survival of long falls and immunity to bubble-column ejection. Normal entity serialization/spawn packets used. A minimal vanilla BoatRenderer registration prevents missing-renderer crashes; graphics stage owns appearance, with no model/texture/screen edits here.
- Red `.tools/equipment-water-walker-red.log` (missing module). Green `.tools/equipment-water-walker-complete-check.log`: real water/energy/sneak/insufficient-EU behavior, entity serialization/reload, physical fall collision and 65-tick bubble-column passenger test. Static `check compileAddonExampleJava` passed (`.tools/equipment-water-walker-build.log`).
- Extended isolated client fixture. `.tools/equipment-client-check.log` and `runs/audio-client-check/console.log` prove native boat reaches the real client with a renderer, H+M payload toggles goggles/server EU/effect and separately compiled addon module data/parameters sync correctly. Existing machine/battery audio and recipe/stat checks still pass. Verification built a development 0.71.0 JAR; this is not a completed equipment release.
- User set a remaining twenty-five-minute work window at 02:48:57 MSK, deferring full completion until tomorrow. Continue only to a saved, reviewed checkpoint before 03:13:57 MSK; no push. Full row 8/7 completion and shutdown condition have not been met.

### Paid fall protection

- Added public FallProtectiveArmor feet-slot contract and native LivingFallEvent integration using MC 1.21 safe-fall-distance/damage-multiplier attributes. Simulation does not mutate. Full protection cancels the fall; partial protection reduces only paid whole damage points, then native damage processing continues.
- Enhanced Nano boots absorb all affordable damage at 800 EU/point. Normal Nano and rubber boots protect at most eleven points on long falls; rubber pays ceil(points/2) durability. Empty/insufficient stores never grant free extra reduction or discard fractional EU.
- Source adaptations are deliberate: original long-fall Nano only simulated payment (free protection), original partial-charge branch reduced requested rather than paid damage, and source fall arithmetic ignored modern attributes. Native charges actual protected points and respects the native attributes.
- Red `.tools/equipment-fall-red.log` proved missing enhanced-boot fall payment. Ordinary NeoForge FakePlayer is unconditionally invulnerable, so damage assertions use a separate vulnerable FakePlayer subclass with native Player/LivingEntity hurt processing. Green `.tools/equipment-fall-complete-check.log` covers real health/EU, preview purity, normal/enhanced/empty boots, rubber durability and separately compiled addon hook invoked exactly once.

### Electrical armor protection and source balance settings

- Audited existing native hazards: cables, pads/stations, Tesla and electrical reactor explosion already use `ic2:electricity`, so no replacement damage type was needed.
- Source enableSpecialElectricArmor=true behavior: full electrical absorption, charge free storage by integer absorbed damage, otherwise spend normal wear unless electric boots stand above a nonempty block. Empty modular shells retain source conductive protection. Feature can be disabled.
- Added original electricSuitAbsorptionScale/electricSuitEnergyCostModifier settings (1.0, range 0.01–500) to ic2-equipment-server.toml. Affordability uses scaled unadjusted per-point energy; actual wear then applies existing electric enchantment rules. Source fall-event payment remains its separate base-cost rule.
- Red `.tools/equipment-electric-armor-red.log` missing electrical charge recovery. Initial `.tools/equipment-electric-armor-check.log` passed electrical behavior; the additional configured-hit assertion in `.tools/equipment-electric-armor-complete-check.log` is still under investigation. Covered cases: actual electricity hits, empty crystal charging, full floating wear, grounded boots, disabled protection, pure configured descriptor and actual configured health/EU. Fixture explicitly sets 20 maximum health (vanilla cows have 10).
- Updated the main row 8 table itself, removing completed night vision and listing ten ready native module families; remaining Quantum/configurator/utility modules/jetpacks/tools and row 7 are still open.

Electrical configuration verification is now green: `.tools/equipment-electric-armor-complete-check.log`. Corrected the test arithmetic: absorbing two of ten damage applies eight damage, leaving 12 of 20 health (not 18). Production behavior already matched that result; no additional protection code change was required.

### Module callback custody fix and saved checkpoint

- Red `.tools/equipment-module-callback-red.log` exposed that installation callback changes to the module itself were discarded: InstalledArmorModule had already taken a detached copy. Native install now saves the callback-mutated owned module after the committed notification. Parent mutations remain intact.
- Green `.tools/equipment-module-callback-check.log` verifies separately compiled addon state for install, real worn tick and removal, in addition to existing parent counters, persistence, energy and damage tests. API/Javadoc clarifies that electrical modifiers are declarative and captured before install callbacks; callback mutations concern operational state. Live armor mutations are server-owned; previews build fresh stacks.
- Final static/build checkpoint: `.tools/equipment-checkpoint-build.log` — `check jar compileAddonExampleJava`, BUILD SUCCESSFUL. Final actual client+JEI checkpoint: `.tools/equipment-checkpoint-client.log`, with equipment-success marker in `runs/audio-client-check/console.log`. Boat client test matches the newly spawned entity UUID, avoiding false success from an older saved fixture boat.
- Remaining row 8: Quantum armor/default modules, armor configurator, eight source utility/storage module families, source food-storage/auto-food and fluid delegation, all jetpacks/compact variants, laser/saber/precision wrench/advanced tap/sprayers and full tool/scanner/audio/catalog audit. Also audit addon extension of native armor category limits and installed-module inventory/update access before calling module API complete. GUI/model/lang/creative remain the user's parallel stage. Row 7 is still open and follows 8.
- User deferred full completion to tomorrow and allowed at most twenty-five more minutes. This is an intermediate development checkpoint, not completion of rows 8/7; no push, no manual shutdown. Original shutdown condition (both rows fully complete) is not met.

Checkpoint artifact: `build/checkpoints/ic2-classic-re-0.71.0-equipment-checkpoint-20261114.jar`, SHA256 `7282F0E7EB439F17958996B90FCCB9A082D32AEE66682ABFD7D974A471F74CEA`. Development version retained at 0.71.0; do not treat this as a completed equipment release. No game-instance installation or push performed.
