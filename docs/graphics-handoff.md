# Parallel graphics / GUI handoff

User assigned graphics and GUI to a parallel stage. Gameplay work must preserve those changes and avoid further visual edits.

New gameplay registrations available to that stage:

- `colossal_base`, `colossal_furnace`, `colossal_macerator`, `colossal_extractor`, `colossal_compressor`, `colossal_recycler`.
- `ColossalBlock`: `facing` (horizontal) and `lit`. Structure extends behind the controller, to the right relative to that inward direction, and downward; controller occupies the top corner. Cube edges 2/3/4 enable 2/4/8 recipe lanes.
- `ColossalPartBlock`: currently no visual state properties; `ColossalPartEntity` retains the controller position. Original linked multi-block rendering is still for the graphics stage.
- Initial `ColossalMenu` / `ColossalScreen` drafts exist. Extra opening data: lane count followed by `ColossalMachines.Kind`. Slot ordering: all active inputs, all active outputs, battery, two upgrade slots, player inventory. Background paths: `textures/gui_sprites/blocks/machines/ev/colossal_<kind>/gui_{small,medium,large}.png`. These source assets have not yet been added to the importer or imported by gameplay work.
- Original controller textures: `block/machine/hv/colossal_<kind>/{inactive,active}_<side>`. Original base texture: `block/misc/colossal_base/all`.
- New recipe labels needed: `jei.ic2.colossal_lanes`, `jei.ic2.colossal_recycler_chance`, `jei.ic2.colossal_water_reserve`. They describe respectively 2/4/8 parallel lanes, five times the basic recycling chance (capped at 100%), and a startup reserve of at least 100 mB water plus 25 mB steam space per active lane. Each completed operation consumes 50 mB water and generates 25 mB steam.
- Add new blocks to desired creative categories, models, blockstates, names and harvest tags as appropriate; crafting JSONs and machine JEI recipe lists are owned by gameplay work.

Earlier gameplay commit `d226731` includes original pad/chunkloader screens, utility block models and source imports. Its client fixture passed 22 screens, including an actual JEI enrichment page. Gameplay work no longer edits those visuals after the user's parallel-stage instruction.

## Electric enchanter backend

- Registered `electric_enchanter` with horizontal `facing` and `lit`; original recipe is implemented. `ElectricEnchanting.MENU` is the server menu registration. Client screen/renderer/model/name wiring is left to the graphics stage.
- `ElectricEnchanterMenu` keeps original seven item-slot coordinates. Player inventory starts at y=90; a synchronized **read-only hidden slot 43** carries the preview. `menu.preview()` reads it for rendering. It cannot be clicked or shift-moved into inventory.
- `menu.value(index)`: 0 EU, 1 mode (0 enchant/1 anvil/2 disenchant), 2 stored raw XP, 3 required raw XP, 4 enchant level, 5 running (0/1), 6 progress, 7 progress target, 8 valid (0/1), 9 operation seed. Values are already reassembled from split network shorts.
- Button IDs: 0 cycle mode; 1/2/3 deposit 1/10/100 raw XP; 4/5/6 withdraw 1/10/100 raw XP; 7 start. Withdrawal is disabled while running. Server validates menu distance and resources. Preview/start use the server player enchantment seed; clients do not send arbitrary seeds.
- Source background: `textures/gui_sprites/blocks/machines/hv/gui_electric_enchanter.png`. Source animated book rendering remains a graphics responsibility. Gameplay tests cover all modes, exact XP/EU costs, item/book/lapis consumption, seed/preview persistence, stale capabilities, and a real addon item's `ElectricEnchantable` contract.

## Electric fisher backend

- Registered `electric_fisher` with horizontal `facing` and `lit`. No inventory/menu on the controller, matching the source; output goes into an inventory with more than twenty slots directly above it.
- Structure: controller centered in a 5×5 top plane; inner eight blocks are planks, outer sixteen logs. Below: five to ten waterlogged chain blocks in the center and a complete 5×5 source-water/waterlogged prism at every chain depth. Source checks do not require a particular chain axis.
- All twenty-four wood blocks at controller height are real EU input ports. `EnergyPortOwner` / `EuNetwork.registerPorts` implement mechanics and server-side API lookup; cable-arm visuals for these plain wood blocks remain a graphics concern. Ports are reconstructed after reload and rejected immediately when a remote structure block breaks.
- API exposes formed state, chain length/luck, progress and thirty-two loot rolls per cycle. `lit` indicates a working powered tick. Each cycle is 300 ticks at 150 EU/t, with source Luck of the Sea III tool context and depth-based luck.
- Real-server tests compare every actual output item/component with the exact thirty-two seeded loot-table rolls, including a mid-cycle reload, and exercise EU input through plain wood, invalid containers, starvation and broken-port revocation.

## Advanced upgrades backend contract

- Advanced inventories now contain their original slots plus two upgrade slots. Backend menu data retains indices 0–6 and extends to 13: 7 energy high word, 8/9 capacity low/high, 10/11 input packet low/high, 12 sound multiplier permille. `AdvancedMachineMenu.energy()`, `capacity()` and `packet()` reconstruct the values. Upgrade slots currently use (134,53)/(152,53); final screen layout belongs to the graphics stage.
- New item IDs needing names/models: upgrade_simple_import, upgrade_simple_export, upgrade_redstone_inverter, upgrade_redstone_sensitivity, upgrade_loudness, upgrade_muffler, upgrade_mute. Existing fluid_import_upgrade_simple/fluid_export_upgrade_simple and cobblegen_upgrade retain their IDs and now work in compatible slots.
- Transport direction is selected by using the upgrade on a block face (Shift reverses); unset direction disables transport. Sound registration/import and actual client audio checks are complete. No screen rendering or model files were modified in this unit.

## Extraction dependency backend

- New block overclocked_pump, common backend menu extraction_machine (three slots): 0 battery, 1 empty container, 2 output. Four data values: EU, progress (25 max), tank mB, capacity (2500). No client Screen/model/lang changes were made. Add this machine's creative classification, names, visuals and menu registration in the graphics stage.
- Item automation: top container input; horizontal discharged battery/output; down disabled. Fluid automation: extraction only, down disabled. Source GUI/background belongs to the original accelerated-pump family. Native public FluidPump handoff is implemented and tested.

- Mining dependency item IDs now registered: drill, od_scanner, ov_scanner, ov_scanner_ranged, fluid_scanner. Real power/tool/scanner behavior and source recipes are verified. Their names/models/creative classification belong to the graphics stage. Manual scanner result uses message.ic2.scanner.result with a fallback. No client screen or language files were edited.

## Miner backend

- Registered miner, backend menu miner: four source roles 0 battery, 1 scanner, 2 pipes/backfill, 3 drill. Six data values: EU, progress (200 max), blocked flag, capacity (10000), queued target count, pipe tip Y. Existing extraction_machine menu remains three slots/four values. Both use the common ExtractionMenu backend; separate client menu factories preserve their sizes.
- Physical miningpipe_slim shafts and miningpipe bottom now work through the machine. The bottom is internal, has no creative item and drops a shaft. Add names/models/creative classification and screen registration in the graphics stage. No client screen or language/model files were modified here.

## Ranged pump backend

- Registered ranged_pump and its backend menu: three source roles (battery, pipes/backfill, fluid scanner), four data values (EU, progress/5, tank mB, capacity/5000). Source crafting path through accelerated pump/miner is complete, as is the original electric_fisher recipe. Common ExtractionMenu supplies the backend; no client screen/model/language files were changed.

- New missing catalog item su_battery is functional, with six original recipes. Names/model/creative energy classification remain the graphics stage. The original portable battery audio loop is imported, registered and verified in a real client; no screen/layout files were changed.

- Cables now expose a gameplay fluid property with values none/water/lava and standard fluid state/bucket behavior. Existing connection selectors can retain their models; this stage did not edit blockstates/models/screens/language files. Recipe discovery metadata and player statistics are verified separately from GUI work.

## Equipment migration in progress

Bronze/aluminium tools: tool_{axe,hoe,pickaxe,shovel,sword}_{bronze,aluminium}. Armor: armor_{bronze,composite}_{helmet,chest,leggings,boots}. Shields: bronze_shield, aluminium_shield, composite_shield, nano_shield. Mechanics, recipes, discovery and enchantment tags are supplied. Client textures/models/language, armor layers, shield transforms and creative classification remain with the graphics stage; no client GUI was changed.

Drill IDs diamond_drill, advanced_drill, rock_cutter_drill added. Public ConfigurableTool exposes modes; current server gesture Shift+right-click, no new key bindings/screens. Source rock-cutter tag and recipes are present. Sized-shape recipe diamond_drill_fortune adds a counts map; JEI presentation of per-slot counts remains the graphics stage (native result includes Fortune III).

Electric/advanced hoes and chainsaws added: electric_hoe, advanced_hoe, chainsaw, advanced_chainsaw. Advanced hoe ConfigurableTool modes are radius strings 0..5; Shift+right-click cycles. Original gameplay recipes and tags supplied. Names/models/creative grouping remain the graphics stage; no screen/layout changed.

Toolbox backend now has source capacities 8/15/45: toolbox, carbon_box, iridium_box. Extended storage reuses native 9x2/9x5 menus (carbon has three blocked padding slots); the base keeps its existing 3x3 backend/eight active slots. No client screen changed. Public Toolbox.scroll and Shift+right-click other-hand exchange are ready; source mouse-wheel/key binding and visual layout remain for the client stage. Original upgrade recipes preserve contents/EU.

mowing_tool added with off/on ConfigurableTool mode, normal right-click toggle, source recipe and server-held operation. Models/names/creative classification remain with graphics. No screen or client layout changed.
