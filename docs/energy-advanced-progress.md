# Energy and advanced machines: work log

Task: fully finish backlog rows 3 and 4, retain addon API availability, update remaining work, commit locally without pushing. These rows are still open.

## Completed units

- Compared pad, magnetizer, Tesla and chunkloader behavior against the locally supplied original mapped sources.
- Added all eight projection upgrades, their models, translations and crafting recipes. Corrected undefined pattern letters in the original proximity/wide-band recipes. Proximity's undefined bottom-center ingredient becomes an empty cell; wide-band uses the advanced-circuit key actually referenced by its pattern.
- Added passive speed/storage/tier upgrades and the additive `MachineUpgrade` addon item contract. Charging pads apply these modifiers; existing advanced machines still need integration. Pad slots are separate for projection and passive upgrades, preserve legacy fuel inventories, and use the original GUI background and coordinates.
- Added the fission pad's fuel durability, twenty-tick production reserve and persistence. Damage/drain modes retain source PVP/tier restrictions; drain consumes pad energy rather than recovering target energy. Pure range/selection and modifier tests pass. Real-server tests cover duplicate projection flags, input directions, upgraded storage/tier, reload and actual fuel consumption/output.
- Added magnetizer and Tesla blocks with source capacities, input packets, redstone disable behavior and working effects. Iron fence now has fence geometry, persistent charge and magnetic movement with metal boots; addons may add boots to `ic2:magnetic_boots`. Tests cover real fence charge/EU and Tesla damage/EU. Zombie fixture armor is explicitly removed to measure the electrical pulse independently of armor mitigation.
- Added chunkloader radius/ticking costs, startup buffer, ownership/settings menu and separate persistent NeoForge tickets. Tests verify two owners in one chunk, removal of only one owner's ticket and cleanup on energy exhaustion. Mode 1 uses Minecraft team alliances because the original separate friend subsystem is not ported. Client rendering and restart recovery still require review.
- Exposed pad, monitor wall appearance, enricher and utility machine telemetry. Added an unambiguous `output_steam_mb` alias while preserving the legacy steam reactor key. Updated `API.md` with exact units and current contract scope.
- Fixed missing uranium-enricher JEI category. Recipe displays derive from the actual catalyst/point definitions, including retained surplus and separate preparation costs. Runtime tests check an acquisition route for every registered IC2 ingot and actually manufacture all five enriched families from each displayed catalyst route. Added missing copper-dust blasting.
- Original textures remain local imports, excluded from Git. Extended importer for original pad/chunkloader GUI textures.

## Validation logs

- `.tools/energy-advanced-check.log`, `.tools/utility-check.log`: Gradle standalone checks and separate addon compilation.
- `.tools/recipe-server-check.log`: all ingot acquisition routes, actual enrichment production, existing API/production/chunk lifecycle smoke checks.
- `.tools/pad-server-check.log`, `.tools/utility-server-check.log`, `.tools/chunkloader-server-check.log`: successive completed server units.
- Client fixture extended from 15 to 22 screens, including four pads, chunkloader, enricher and a real JEI enrichment page. The run passed; screenshots of the pad and actual JEI recipes were inspected. Extended the fixture floor after the added devices exceeded its old bounds and the test player fell, closing the chunkloader screen. Increased enrichment category height to fit longer Russian notes, and retained original GUI charge bars.

## Remaining

Five colossal processing machines, electric enchanter and electric fisher; upgrades/sounds/layout verification for existing advanced machines; charged electrolyzer and pressure-furnace final game checks; full row-3 network/source comparison, charging-device layouts/JEI and remaining visuals. Finish client QA, persisted-ticket restart validation and API coverage audit. Then reconcile `осталось.md` and produce the final local commit; no push.

## Colossal processing follow-up

- Added five working controllers and the shared base block. Source-oriented solid cubes extend inward, right and down from the top-corner controller; edges 2/3/4 unlock 2/4/8 independent lanes. Each lane consumes 30 EU/t, reaches 6000 progress in six ticks (recycler: one), consumes 50 mB water and produces 25 mB steam. Startup reserve is 10% of capacity; per-lane fluid reserve checks and progress rollback are implemented.
- Added original crafting patterns and machine JEI recipes, storage/tier modifiers, charge item support, output XP, state persistence and owner-checked linked capability forwarding. Shared `MachineModifiers` now also serves pads without duplicating modifier aggregation.
- Added `MachineSnapshot.Access` for optional linked-controller reads. Colossal parts, existing power parts and pressure-furnace parts now expose loaded live controllers; broken, removed and unloaded links return empty. Pressure parts also reject stale entity references explicitly.
- `.tools/colossal-server-check.log` passed real production for all five machines in all three cube sizes, every parallel lane, EU/water/steam accounting, simulated filling, linked API lookup and immediate rejection through cached capabilities after a different part breaks.
- User assigned graphics/GUI to a parallel stage after the initial colossal menu/screen drafts were written. Subsequent work avoids visual files; `docs/graphics-handoff.md` records new IDs, states, expected paths and recipe label semantics for that stage. Visual completion is no longer claimed by this gameplay unit.
- Remaining machine implementations are electric enchanter and electric fisher; the broader upgrade/network/restart/API verification items remain open.
