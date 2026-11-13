# IC2 Remastered: architecture audit and alloy prototype

Base: `d2267319eb625c1b71c8dfe3607441f17da07a0e`, independent `ic2classic-port-gui` checkout, `gui-update` branch.

## Current layout revision

Latest recipe revision: replaced the single large recipe with five visible 18px
rows in NeoForge's existing `ScrollPanel`. The panel handles wheel input, scrollbar
dragging and clipping; the adapter supplies recipe rows, selection and hover
items. Buttons retain previous/next selection and reveal the selected row; arrow
keys navigate a focused list. Recipe details occupy the right side with EU/t at
y=118, clear of the lower edge and paging controls. No new dependency was added.

Library research: inspected the installed NeoForge 21.1.219 `ScrollPanel` source
and the official NeoForge GUI documentation. Also considered owo-ui's documented
scroll containers (https://docs.wispforest.io/owo/ui/components/scroll-container)
and NeoForge setup (https://docs.wispforest.io/owo/setup). The already available
NeoForge primitive covers this feature without adopting another UI framework.

Revision log: first reproduced the missing ScrollPanel with a failing real-client
assertion. Added the adapter and five-row/detail layout, corrected the narration
implementation against the installed API, then passed the full offline build and
24 checks. Real-client wheel, sixth-row selection, repeated next-button wrap,
four-tab and shift-click checks passed. Inspected screenshots showing five full
rows and EU/t inside the panel. Launched in a separate copied world using the new
RunName option and kept the verified client open for the user.

Latest user revision: restored a single full 256x240 outer steel frame around
both the machine and inventory. Kept native rendering, styled buttons, recessed
inventory and verified slot coordinates. Removed unused split-frame dimensions.
The T-shaped composition described below records the previous iteration.

The user selected a T-shaped composition and then requested native rendering in
place of a generated frame. The production screen now uses code-drawn beveled
steel frames: a wide 256x148 machine cap above a centered 176x94 inventory panel.
Total bounds remain 256x240. All 27 inventory cells and 9 hotbar cells fit inside
the lower panel. `AlloyGuiLayout` shares coordinates between menu and rendering.
`RemasteredButton` keeps vanilla input, focus and narration while drawing its own
graphite/steel skin, orange selected border and distinct hover/disabled states.
Recipe paging uses the same button skin. The unused generated PNG was removed;
earlier generation notes below describe the superseded first prototype only.

An additional real-client assertion reproduced the vanilla-button problem before
the rendering change. The suite now checks styled buttons and actual inventory
slot bounds as well as tabs and shift-click. `Test-RemasteredClient.ps1 -KeepOpen`
returns after successful verification, leaving the client and alloy screen open
until the user's manual exit.
The opt-in test addon stops automated actions after verification and supplies
copper/tin in the player's creative inventory for manual checks. Normal test runs
still exit the client automatically. Production gameplay has no test behavior.

Revision action log: reproduced the old button rendering; replaced raster shell
with dimensioned frames; centralized slot coordinates; styled tabs and paging;
removed unused texture; added keep-open handoff and checks. Offline build and 24
existing checks passed. The first keep-open launch exposed an empty startup-log
race in the test script; added a null check before reading its success marker.
Real-client checks passed for styled tabs, all 36 inventory cell bounds, recipe
paging and bidirectional shift-click. Inspected the actual game screenshots.
Confirmed the retained Minecraft process is responding outside the sandbox.
Sandbox process enumeration cannot see that elevated game process; use the same
execution context as the launcher to check it. No changes in the original project.

## Existing architecture

Minecraft 1.21.1, NeoForge 21.1.219, Java 21, NeoGradle userdev 7.1.20.
JEI 19.64.0.455 is a compile-only integration; `Ic2JeiPlugin` uses
`MachineRecipeCategory`, `MachineRecipeViews` and the public addon recipe bridge.
No REI/EMI implementation or new dependency is introduced.

`IC2` owns the item register and initializes subsystem registrations. `Machines`,
`AdvancedMachines`, `PowerMachines`, `UtilityMachines`, charging, extra generators,
and supporting material registries use NeoForge deferred registers. Blocks,
block entities, menus and recipes retain their existing resource IDs.

`MachineEntity` owns basic machines, processing, energy and inventory; `MachineMenu`
owns authoritative slots and short-based `ContainerData` synchronization. Existing
energy and work values use two 16-bit words each. `MachineScreen` shares rendering
across basic processing, storage and generator menus. Advanced processing,
chemistry/generation, reactors, charging, monitoring and utility menus use their
own screens and layouts (`ProcessingLayout`, `GenerationLayout`). Do not force
these distinct data shapes into one universal menu.

EU is the existing custom energy network (`EnergySink` and related cable/network
code), not a new FE replacement. Processing uses existing cooking/macerating
recipe-manager entries and specific alloy/extraction/compression/canning tables.
`MachineRecipeViews` adapts these sources for JEI and now the embedded browser.
Upgrade rules exist in `MachineUpgradeRules`, the public `MachineUpgrade` API and
charging/utility implementations. Basic alloy processing has no upgrade inventory.

## Complete visual inventory

`remastered-asset-catalog.json` lists every current blockstate ID (159), item model
ID (365), model path (648), model texture reference (1132 unique), screen and menu
file (13 each, including the new prototype screen). Item models include block
items; these numbers are a resource inventory, not independent registry counts.

Scope includes base processors, induction machines, pressure structures, storage
tiers, transformers/cables, generators, reactors/planner/heat pump, turbines,
charging pads, monitors, chunkloader, utility and chemistry machines. Items include
materials/ingots/dusts/plates, cells, circuits, cables, batteries, tools, armor,
reactor components, upgrades and all other entries in the catalog.

Assets live under `src/main/resources/assets/ic2/{blockstates,models,textures,lang}`.
Most original PNGs are intentionally ignored by Git. The existing
`Import-OriginalTextures.ps1` restores local development assets from the supplied
original JAR (1164 selected resources). These original assets are retained as the
baseline, not represented as newly created artwork. Runtime-only textures include
GUI sprites, fluids, armor and rotors; consult that importer in addition to model
references. New Remastered artwork lives in a separate tracked namespace.

## Proposed GUI architecture

- Server: preserve machine processing, inventory validation and energy behavior.
- Menu: preserve slot ordering and IDs; apply a machine-specific coordinate layout
  on both client and server. Keep presentation visibility local to the client.
- Client: machine screen composes a generated shell, tabs, slot rendering, progress,
  status, energy and recipe panels. `RemasteredGui` centralizes common rendering.
- Recipes: adapt `MachineRecipeViews`; never copy recipe definitions to a screen.
- Capabilities: render supported functionality only. No invented side controls,
  voltage tiers, automation switches or upgrade slots.
- Migration: extract larger panel/tab classes after a second real screen shares
  their behavior; avoid empty frameworks and one copy of the GUI per machine.

## Prototype plan and implementation

1. Register `AlloySmelterScreen` only for the existing alloy menu type.
2. Use a 256x240 shell generated with ChatGPT image generation from the supplied
   visual direction: steel frame, graphite recesses, restrained orange accents.
3. Retain two inputs/one output and player inventory. Move their coordinates only
   for this machine, preserving slot ordering and shift-click routing.
4. Work tab: live EU, progress, remaining ticks converted to seconds, server LIT
   status and a small active animation. The current EU/t label reports zero idle.
5. Recipes tab: ingredient alternatives/counts, output count, paging, hover item
   tooltips, 4000 EU and 12.5 seconds per recipe from `MachineKind` (16 EU/t).
6. Upgrades tab: explicit unsupported notice and actual base characteristics.
7. Energy tab: 4000 EU buffer and honest notice that existing alloy logic has no
   incoming packet voltage limit. Do not label it LV just to match the reference.
8. Reuse the otherwise unused alloy FLUID sync field for its server LIT bit;
   other menu types and machine behavior stay unchanged.
9. Keep player inventory active on all tabs; disable machine slots outside Work.
   Recipe preview items are not interactive container slots.

## Files changed

- `MachineScreen.java`: register the alloy screen.
- `MachineMenu.java`: alloy coordinates and local slot visibility.
- `MachineEntity.java`: synchronize alloy activity through the existing data field.
- `AlloySmelterScreen.java`: machine panels and tabs.
- `RemasteredGui.java`: texture shell, slots and gauges.
- `assets/ic2/lang/{en_us,ru_ru}.json`: translated interface text.
- `assets/ic2/textures/gui/remastered/machine_panel.png`: generated artwork.
- `RemasteredClientChecks.java`, `scripts/Test-RemasteredClient.ps1`: isolated real
  client/server verification and screenshots; excluded from the production JAR.
- This design/audit and the asset catalog.

## Next asset stages

Do not replace every texture in this prototype. For each block in the catalog,
preserve existing model/texture paths and orientation mapping. Prepare coherent
top/side/bottom and unique front variants, plus existing lit/unlit faces. Use
16x16 or 32x32 native pixel assets, nearest-neighbor sampling, consistent borders,
steel/graphite bases and functional heat/energy/cooling/warning accents.
Validate all FACING/LIT blockstate variants before rollout. Item assets must keep
silhouettes and readable differences at 16 GUI pixels. Armor/rotor/fluid atlases
have dedicated UV/animation requirements and need separate batches.

New GUI panel: generated 1254x1254 PNG, sampled in three bands to the exact runtime
layout; no baked text, item icons or slot contents. Never resample blindly into
block/item textures. Existing texture paths and resource-pack overrides remain;
the new shell is overridable at `ic2:textures/gui/remastered/machine_panel.png`.

## Verification and action log

- Read ub-mode and imagegen instructions; inspected the independent checkout.
- Audited registrations, screen/menu sharing, energy, recipes and upgrade support
  before selecting the minimal alloy-only implementation.
- Generated and inspected the panel; copied the selected PNG into resources.
- Restored ignored local assets with the repository importer; original checkout
  and its ongoing edits were not modified.
- Added the prototype and translations; corrected a compilation mismatch in title
  trimming against the installed Minecraft API.
- Full offline Gradle build passed, including all 24 existing standalone checks
  and compilation of the public API consumer.
- Added opt-in client checks for actual menu opening, four tabs, recipe paging,
  synchronized input slots and bidirectional shift-click; screenshots use the
  real game renderer in an isolated copied world.
- Client checks passed. Visual inspection caught clipping at 854x480 / GUI scale
  2; reduced the shell to 256x240, adjusted inventory placement, reran the client
  checks successfully and inspected all four tabs. Final full build also passed.

The screenshot suite does not prove every GUI scale, multiplayer latency, JEI
overlay behavior or resource pack combination. These remain explicit manual
acceptance checks before migrating other mechanisms. No balance, recipe, block
or item ID changes; no new production dependency. No push into the original
repository or its main branch: GUI work remains isolated as requested.
