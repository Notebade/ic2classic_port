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
