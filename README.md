<div align="center">

# ⚡ IC 2 Classic Re

### IndustrialCraft Classic for Minecraft 1.21.1

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-62B47A?style=for-the-badge)
![NeoForge](https://img.shields.io/badge/NeoForge-21.1.219-F07C32?style=for-the-badge)
![Java](https://img.shields.io/badge/Java-21-3973B9?style=for-the-badge)
![Version](https://img.shields.io/badge/Version-0.57.0-8E78C9?style=for-the-badge)

**⚙️ Machines · 🔋 EU Power · 🌊 Generators · 🧪 UU-Matter · 🔌 Automation**

A prototype port of **IC2 Classic 2.1.3.3** from Forge 1.19.2 to **Minecraft 1.21.1 / NeoForge**.

</div>

> [!IMPORTANT]
> This is a **work-in-progress prototype**, not a complete port of IC2 Classic. Building requires Java 21 and a local copy of the original mod for texture import.

## 📚 Contents

- [🧰 Build & Run](#-build--run)
- [📊 Project Overview](#-project-overview)
- [⚡ Power Generation](#power-generation)
- [🔌 Cables, Transformers & Storage](#cables-transformers--storage)
- [🏭 Machines & Processing](#machines--processing)
- [🧪 UU-Matter & Recycling](#uu-matter--recycling)
- [🌳 Resources, Tools & Survival](#resources-tools--survival)
- [📖 Compatibility & Testing](#compatibility--testing)
- [🚧 Current Limitations](#current-limitations)
- [📜 License](#-license)

---

## 🧰 Build & Run

Import the original textures from a local copy of **IC2 Classic 1.19.2-2.1.3.3**. From the project root, run in PowerShell:

```powershell
.\scripts\Import-OriginalTextures.ps1 -OriginalJar 'C:\path\to\ic2classic-1.19.2-2.1.3.3.jar'
.\gradlew.bat build
```

Launch the development client:

```powershell
.\gradlew.bat runClient
```

> [!NOTE]
> The prototype was built on the development machine with a JDK and preconfigured NeoForge dependencies. The standard Gradle build encountered issues downloading Minecraft files.

---

## 📊 Project Overview

| Component | Version / Status |
|:--|:--|
| Minecraft | `1.21.1` |
| Mod loader | `NeoForge` |
| Tested NeoForge version | `21.1.219` |
| Java | `21` |
| Original mod | IC2 Classic `1.19.2-2.1.3.3` |
| Project version | `0.57.0` |
| Project status | In development |

### Generator Comparison

| Generator | Output | Notes |
|:--|:--|:--|
| 🔥 Thermal | Up to **90 EU/t** | Lava, biome temperature, nearby blocks |
| 🌊 Ocean | Up to **100 EU/t** | Connected water and living coral |
| 🌊 Wave | **15 EU/t** base at 50 sources | Modified by weather and waves |
| 💧 Water Mill | Up to **0.52 EU/t** passive; **1 EU/t** tank | Water sources, buckets, cells, pipes |
| 💧 LV Water Mill | Up to **1.56 EU/t** passive; **4 EU/t** tank | 32 EU buffer |
| 💧 MV Water Mill | Up to **18.56 EU/t** passive; **32 EU/t** tank | 256 EU buffer |
| 💧 HV Water Mill | Up to **115.2 EU/t** passive; **256 EU/t** tank | 2,048 EU buffer |
| 🌬️ Wind | Up to **16 EU/t** | Wind and obstacles |
| 🌋 Geothermal | Up to **20 EU/t** | 24,000 mB lava tank |
| ☀️ Basic Solar Panel | **1 EU/t** | Standard and compact versions |
| ☀️ LV / MV / HV Solar Panel | **8 / 64 / 512 EU/t** | Standard and compact versions |

> Outputs refer to the operating conditions described below.

---

## ⚡ Power Generation

### Electric Nuclear Reactor

Apply a redstone signal to the reactor or one of its chambers. A single uranium rod generates 20 EU/t and adds four units of heat per cycle. Adjacent rods increase both output and heat. Place a heat vent next to a rod to absorb heat and dissipate up to six units per cycle. Each reactor chamber adds one column to the core grid and can transmit EU. The core explodes at 10,000 heat.

At 70% of its heat limit, the reactor irradiates nearby living entities. A meltdown causes radiation poisoning within 30 blocks. A complete four-piece hazmat suit blocks radiation damage. Uranium ore generates in new Overworld chunks.

Fresh rods can be combined with dense copper plates. With no adjacent rods, a dual rod generates 80 EU/t and 24 heat per cycle, and a quad rod generates 240 EU/t and 96 heat per cycle.

Craft eight Near-Depleted Uranium Rods from a uranium ingot surrounded by empty cells. Mix a depleted rod with coal or charcoal dust to make an isotopic rod, then place it next to active fuel. Neutron pulses re-enrich the rod faster as the hull heats up. Mix the re-enriched rod with coal or charcoal dust to craft fresh fuel. Heating Cells warm the hull even when the reactor is off.

The Uranium Enricher accepts up to 512 EU per packet and stores 100,000 EU. Feed uranium ingots and a catalyst into separate slots. Redstone, charcoal, blaze rods, ender pearls, and nether stars produce five fuel families with distinct heat, pulse and durability rules. Each family has single, dual, quad, near-depleted, isotopic and re-enriched rods. Ender Pearl rods pulse along their row and column and can heat components diagonally. The original survival recipe requires a Vacuum Canner.

Neutron Reflectors placed next to a fuel rod add one pulse per fuel cell. Both reflector types increase EU output and heat. The regular reflector has 10,000 durability and the thick reflector has 40,000. Each reflected energy pulse wears the reflector by one point.

Coolant Cells absorb heat from adjacent fuel rods and store 10,000, 30,000, or 60,000 heat. They do not dissipate heat on their own. Craft the smallest cell with tin and a water bucket or water cell, then combine smaller cells to craft the larger versions.

Reactor and overclocked heat vents draw heat from the hull. Advanced vents dissipate heat faster. Electric vents consume some of the reactor's output while it runs and cool at half speed while it is off.

Heat exchangers redistribute heat between the hull and adjacent components. Heat balancers target components at or below 25% heat or at or above 75% heat. Component Heat Vents remove four heat from each adjacent heat-storing component per cycle. RSH and LZH Condensators absorb heat until saturated and can be restored with redstone or, for LZH, lapis lazuli.

Reactor Plating raises the hull's heat limit by 1,000, 2,000, or 500 depending on the type and reduces radiation and blast damage. The Iridium Neutron Reflector stops reflecting at 320,000 wear and can be repaired with four piles of coal or charcoal dust in separate crafting slots. Iridium ore is made from seven UU-Matter.

### Steam Reactor and Turbine

The steam reactor stores 2,000 mB of water and 20,000 mB of steam. Add matching chambers to expand the core. Water can be supplied by bucket or a fluid handler through the reactor or a chamber. Steam vents use component heat to boil water. The reactor processes heat each tick and wears fuel once every 20 ticks.

Right-click a Heat Pump to choose the sides that pull heat. Other sides push heat into adjacent components. The crafting recipe uses Reinforced Glass in the High Pressure Pipe slot until the pipe network is ported.

An inactive reactor fuel cell becomes a Fuel Cell after absorbing enough neutron pulses. On a crafting table, combine an empty cell, a clay dust, and eight netherrack dust stacked in one slot. The two-step recipe through a prepared cell also works.

Connect a Steam Turbine directly to the reactor or a chamber. It consumes up to 128 mB of steam per tick, produces up to 64 EU/t at full speed, and stores up to 4,000 EU. Its EU output is sent in packets of up to 128 EU.

### Reactor Planner and Card

Creative items are grouped into six IC 2 tabs: Tools and Protection, Generators, Machines, Energy, Nuclear, and Materials.

Click H to add 500 starting heat or Shift-click to subtract it. The setting is saved with the layout and copied by the Reactor Card.

Click T to raise the cycle limit to the next preset or Shift-click to lower it. Both Run and Step stop at the limit. The Reactor Card preserves the limit.

Click S or Shift-click S to adjust automatic simulation speed from 1 to 100 cycles per tick. Each running tick costs 100 EU. A manual Step advances one cycle. The speed is saved with the layout and copied by the Reactor Card.

Hover over the output line to see total EU or steam produced, average and peak hull heat, the first cycle forecast, and the initial layout's blast strength. Heat is sampled after each simulated cycle.

Select V2, V1, NBT, or Talon above the inventory, then click ↔ to copy a plan code. Shift-click ↔ to import any of those formats from the clipboard.

The Reactor Planner runs either core mode with the same component rules as the live reactor. Select a component from the palette or carry an item with the cursor, then click a grid slot to copy it into the plan. Right-click a grid slot to clear it. Adjust the chamber count, step or run the simulation for 100 EU per step, and reset to the saved layout. Steam simulations assume continuous water supply and steam extraction.

Use a Reactor Card on a reactor or chamber to link it. Use the card again to display the linked reactor's heat and production while it is loaded in the current dimension. Use the card on a planner to copy its layout, or sneak-use it to load the saved layout. The card uses a direct crafting recipe while the monitor system is pending.

### Thermal Generator

The Thermal Generator holds 20,000 mB of lava and outputs up to 90 EU/t. A lava bucket provides 30,000 EU. Without fuel, it calculates heat from hot and cold blocks within a seven-block radius and the biome's base temperature. Its slot can charge an Energy Crystal.

### Ocean Generator

The Ocean Generator counts connected water source blocks and living coral within five blocks. With 1,000 sources and 50 coral blocks, it produces up to 100 EU/t. Generators in the same connected area share the output. Its buffer holds 8,000 EU. Right-click to view current production.

### Wave Generator

The Wave Generator produces EU when its front face points along a continuous line of water source blocks. Up to 50 sources count. A full-length line yields a base 15 EU/t, modified by weather and waves. Its buffer holds 1,000 EU. Right-click to view current output.

### HV Water Mill

The HV Water Mill is crafted from eight MV Water Mills and an HV Transformer. It stores 2,048 EU, produces 256 EU/t from its tank, and obtains 128,000 EU from one water bucket. Connected water sources within two blocks provide up to 115.2 EU/t. Its slot can charge a Lapotron Crystal.

### MV Water Mill

The MV Water Mill is crafted from eight LV Water Mills and an MV Transformer. It stores 256 EU, produces 32 EU/t from its tank, and obtains 16,000 EU from one water bucket. Connected sources within two blocks provide up to 18.56 EU/t. Its slot can charge an Energy Crystal.

### LV Water Mill

The LV Water Mill is crafted from eight standard Water Mills and an LV Transformer. It stores 32 EU and produces up to 4 EU/t using tank water. A bucket provides 2,000 EU. Passive generation reaches 1.56 EU/t.

### Water Mill

A standard Water Mill passively produces 0.02 EU/t per connected surrounding water source, up to 0.52 EU/t. Water can also be supplied by bucket, cell, or pipe. In that mode it outputs 1 EU/t and gains 500 EU per water bucket. Empty containers are returned, and a battery can be charged in a separate slot. One generator, four sticks, and four planks craft two Water Mills.

### Wind Generator

The Wind Generator is crafted from a generator and four refined iron ingots. Output varies with wind and reaches 16 EU/t. Obstacles reduce airflow, and a fully enclosed space stops production. It stores up to 32 EU and charges a battery in its slot.

### Geothermal Generator

The Geothermal Generator holds 24,000 mB of lava and outputs up to 20 EU/t. One bucket provides 20,000 EU. Lava can be poured in by hand, supplied through NeoForge fluid capabilities, or inserted in a bucket through the input slot. A hopper underneath collects empty containers and fully charged batteries.

### Basic Solar Panels

A basic Solar Panel is crafted from three coal dust, three glass blocks, two Electronic Circuits, and a generator. Regular and charcoal dust both work. A Compressor converts one basic panel into one compact panel. The compact version occupies half a block, can be waterlogged, and outputs 1 EU/t only through its bottom face. The standard panel outputs the same through its sides and bottom.

### LV Solar Panels

An LV Solar Panel is crafted from eight basic panels and an LV Transformer. Both standard and compact versions produce 8 EU/t. Craft a compact LV panel from eight compact basic panels and a transformer, or compress a standard LV panel.

### HV Transformer and Iron Cable

The HV Transformer stores up to 4,096 EU and splits a 2,048-EU packet into four 512-EU packets. Redstone switches it to step-up mode. Crafting requires an MV Transformer, Electronic Circuit, Energy Crystal, and two triple-insulated iron cables. Iron cable carries packets up to 2,048 EU. Loss is 1 EU/block bare, 0.95 with one layer, 0.9 with two, and 0.8 with three.


---

## 🔌 Cables, Transformers & Storage

### MFE Energy Storage

The MFE stores up to 600,000 EU and outputs up to 128 EU/t through its marked face. Battery charging and discharging work as in the BatBox. The prototype network allows a BatBox to charge an MFE, while preventing transfer between equal-tier storage blocks or in the reverse direction. The MFE is in the Redstone tab.

### Tin Cable

Tin Cable supports basic Solar Panels. It carries packets up to 5 EU with 0.025 EU/block loss. Three tin ingots craft nine cables.

### Bronze Cable

Bronze Cable carries packets up to 128 EU. Losses are 0.70 EU/block uninsulated, 0.65 with one insulation layer, and 0.60 with two. Three bronze ingots craft six cables. Insulation can be added in crafting or in-world with the cutter.

### LV Transformer

The LV Transformer stores up to 256 EU. Without redstone, its marked face accepts packets up to 128 EU, while the other faces output up to four 32-EU packets per tick. With redstone, five faces accept packets up to 32 EU and the marked face outputs 128-EU packets. Right-click to inspect mode, stored energy, and the 128-EU face.

### MV Transformer and Solar Panels

The MV Transformer stores up to 1,024 EU and splits a 512-EU packet into four 128-EU packets. Redstone switches it to step-up mode. Standard and compact MV Solar Panels produce 64 EU/t. Gold wire is used in transformer crafting. Gold cable losses are 0.50 EU/block bare, 0.45 with one insulation layer, and 0.40 with two.

### EV Transformer

The EV Transformer stores up to 16,384 EU and splits an 8,192-EU packet into four 2,048-EU packets. Redstone switches it to step-up mode. Crafting requires an HV Transformer, Advanced Circuit, Lapotron Crystal, and two silver ingots.

### Cable Switch

The Cable Switch cuts EU transmission while powered by redstone and restores it when the signal disappears. Unpowered, it passes packets up to 2,048 EU with 0.5 EU/block loss. Crafting requires a lever, two redstone dust, and two triple-insulated iron cables.

### Detector Cable

The Detector Cable checks every 20 ticks whether energy has passed through it. If a receiver accepted EU during that interval, the cable emits a level-15 redstone signal on all faces until the next check. It carries packets up to 2,048 EU with 0.5 EU/block loss. Crafting requires an Electronic Circuit, three redstone dust, and triple-insulated iron cable.

### Comparator Cable

The Comparator Cable measures average energy flow over 20 ticks and converts it into redstone strength 0–15. Right-click to open a GUI for lower and upper EU/t thresholds, defaulting to 0 and 1. At or below the lower threshold output is 0; at the upper threshold it is 15; between them it scales linearly. It carries packets up to 2,048 EU with 0.5 EU/block loss. Its recipe replaces the Detector Cable's Electronic Circuit with an Advanced Circuit.

### Aluminum Cable

Aluminum Cable carries packets up to 8,192 EU. Three aluminum ingots craft twelve cables. Bare cable loses 1.20 EU/block, reduced by 0.05 EU for each of up to four insulation layers.

### Glass Fiber Cable

Glass Fiber Cable carries packets up to 512 EU with 0.025 EU/block loss. Six glass blocks, one diamond, and two redstone dust craft four cables; using two silver ingots yields six cables.

### Energy Crystal and MFE Recipe

An Energy Crystal is crafted from a diamond and eight redstone dust. It stores up to 100,000 EU and transfers energy to/from an MFE at up to 250 EU/t. An MFE is crafted from four crystals, four double-insulated gold cables, and Machine Casing. Empty crystals are valid ingredients; any charge stored in them is consumed during crafting.

### MFSU and Lapotron Crystal

The MFSU stores up to 10,000,000 EU and outputs 512-EU packets through its marked face. A Lapotron Crystal stores 1,000,000 EU and exchanges energy with the MFSU at up to 600 EU/t. Crafting an MFSU requires six Lapotron Crystals, an Advanced Circuit, an MFE, and an Advanced Machine Casing.


---

## 🏭 Machines & Processing

### Canning Machines

Compress an empty cell to make an Air Cell. The Canning Machine combines an empty cell with an ordinary or enriched uranium ingot to make its fuel rod. Both canners can repair reactor condensators and the iridium reflector with the same materials as their crafting-table repairs. The Vacuum Canner can spin up under a redstone signal and processes faster at higher speed. It accepts 128 EU per packet. Food canning, fuel and foam filling, and the original battery and queue slots remain to be ported.

### Iron Furnace

The Iron Furnace smelts a standard recipe in 160 ticks instead of 200. It uses regular furnace fuel and slots and supports hoppers. Craft it from eight iron ingots or upgrade a furnace with five ingots. It is also an ingredient in alternative generator and Electric Furnace recipes.

### Electric Blast Furnace

The Electric Blast Furnace uses vanilla blast furnace recipes, including data-pack recipes. A standard operation takes 65 ticks and 325 EU. It accepts packets up to 32 EU, receives items from above, and outputs ingots below. Craft it from a vanilla blast furnace, Electronic Circuit, two redstone dust, and Machine Casing.

### Electric Smoker

The Electric Smoker uses smoking recipes, including data-pack recipes. A standard operation takes 65 ticks and 325 EU. It accepts packets up to 32 EU, loads from above, and outputs below. Craft it from a vanilla smoker, Electronic Circuit, two redstone dust, and Machine Casing.

### Sawmill

The Sawmill uses 800 EU over 400 ticks to turn a log into six planks, a melon into nine slices, or a potato into eight slices. The plank type follows the log's crafting recipe. Rubber Tree planks and hoppers are supported. Potato slices can be cooked into fries in a smoker or over a campfire. Crafting requires two stone axes, a turbine blade, four bronze ingots, a casing, and an Electronic Circuit.

### Stone Macerator

The Stone Macerator operates without EU. One batch takes 400 ticks; coal provides 800 burn ticks. Raw copper, raw tin, and their ores produce two corresponding dusts. It can be upgraded to an Electric Macerator.

### Macerator Recipes

Stone and Electric Macerators share a recipe set covering copper, tin, iron, gold, quartz and redstone ores, plus stone, wool, and other vanilla materials. The Electric Macerator consumes 2 EU/t and preserves progress on long recipes, such as 1,600 ticks for a raw copper block. Iron and gold dust can be smelted into ingots.

### Alloy Furnace and Advanced Casings

The Alloy Furnace consumes 4,000 EU over 250 ticks per batch. It makes four bronze ingots from three copper and one tin ingot, or their dust equivalents. Aluminum is made from aluminum dust and sand. Aluminum ore generates in new Nether chunks, and raw aluminum can be macerated. Advanced Alloy combines refined iron, bronze, and aluminum. A Compressor turns alloy ingots into plates and carbon fabric into carbon fiber composite. These plates and a basic casing make an Advanced Machine Casing.

### Rare Earth Extractor

The Rare Earth Extractor processes one item or block in 35 ticks for 35 EU. Five soul sand, soul soil, or obsidian blocks yield two Rare Earth Dust; two clay or terracotta yield two Aluminum Dust. Accumulated input is preserved between cycles and resets when switching dust types. Four Rare Earth Dust and an iron ingot make a blank that a Compressor turns into an uncharged magnet.

### Electrolyzer

The Electrolyzer charges an uncharged magnet or water cell for 20,000 EU. Place it next to a BatBox, MFE, or MFSU with at least 70% charge. The left slot is accessible from above and the right from below. When storage drops below 30%, place electrolyzed water in the right slot: the Electrolyzer returns a water cell to the left slot and supplies 14,000, 16,000, or 18,000 EU to the respective storage tier. In reverse mode, andesite, diorite, and granite yield quartz and 700, 800, or 900 EU.


---

## 🧪 UU-Matter & Recycling

### Recycler Blacklist

Recycler exclusions are configured with the **`ic2:recycler_blacklist`** item tag in a data pack.

### Scrap Boxes

Nine pieces of Scrap can be packed into a Scrap Box in a crafting table or Recycler. Open it by right-clicking or using a dispenser. Rewards are defined by the **`ic2:items/scrapbox`** loot table.

### Matter Fabricator

The Matter Fabricator produces one UU-Matter for 7,000,000 EU of accumulated progress. Scrap, Scrap Boxes, and scrap metal supply amplifier reserves. While reserves last, each incoming EU contributes up to six progress units. A redstone signal blocks energy input.

### UU-Matter Recipes

UU-Matter is used in 54 crafting-table recipes. The position of each piece in the 3×3 grid matters. For example, one piece in the top-left yields 16 cobblestone, one in the center yields 16 stone, and a full grid yields a diamond. Layouts are in `data/ic2/recipe/uu` and can be entered manually in a crafting table.


---

## 🌳 Resources, Tools & Survival

### Rubber Trees and Resin

Rubber Trees generate in newly created forest, jungle, and swamp chunks. Saplings drop from leaves and respond to bone meal. Craft a Treetap with five planks in the pattern `·P· / PPP / P··`. Right-click an orange resin spot to collect 1–3 Sticky Resin. The spot regenerates over time; tapping an empty spot again may destroy it. The Treetap lasts 16 uses.

### Electric Treetap

The Electric Treetap is crafted from a manual Treetap, an Electronic Circuit, and a battery. It stores 10,000 EU, charges in a BatBox at up to 100 EU/t, and consumes 50 EU per collection. Resin drops next to the trunk.

### Getting Started in Survival

Start survival by smelting Sticky Resin into Rubber and iron ingots into Refined Iron. Tin generates in new Overworld chunks between Y -6 and Y 122. Rubber, copper, tin, refined iron, and redstone let you craft insulated wire, batteries, circuits, and machine casings, followed by a generator, BatBox, Extractor, Macerator, Electric Furnace, Compressor, and Recycler. Insulated copper cable loses 0.2 EU/block; bare copper loses 0.3 EU/block. The network rounds total route loss down.

### Bronze Wrench

The Bronze Wrench is crafted from six bronze ingots. Right-click the center of a machine face to rotate it toward that face; sneak-right-click to face the opposite way. Clicking near an edge selects the adjacent face. Rotation uses 1 of 160 durability; clicking the current facing removes the machine for 10 durability. The block and slot contents drop when removed.

### Electric Wrench

The Electric Wrench is crafted from a Bronze Wrench, Electronic Circuit, and battery. It stores 12,000 EU and charges in a BatBox at up to 250 EU/t. Rotation costs 50 EU and removal costs 500 EU. Standard prototype drop rules apply.

### Insulation Cutter

The Insulation Cutter is crafted from three refined iron ingots and two iron ingots. Right-click copper, bronze, gold, iron, or aluminum cable to add one insulation layer using one Rubber from your inventory. Left-click to remove a layer and return Rubber. Each change uses 1 of 128 durability.

### Fluid Cells

An empty cell can be filled by right-clicking a water or lava source or placing it in a Pump. Right-clicking with a full cell places the fluid in the world and returns an empty cell in Survival. The Geothermal Generator accepts lava cells and returns empties. Its recipe requires glass, empty cells, refined iron, and a fuel generator.

### Silver and Stabilized Casings

Silver generates in new Overworld and Nether chunks. Magnets and silver ingots are needed for Stabilized Casings; two of these casings are used to craft the Matter Fabricator.


---

## 📖 Compatibility & Testing

### JEI Integration

With JEI installed, press R to see how to obtain an item and U to see its uses. JEI displays IC2 machine recipes, EU costs, processing times, and Recycler output chances. UU-Matter recipes appear under crafting with exact 3×3 layouts. JEI is optional.

### Implemented and Tested Features

The prototype includes a fuel generator, copper cable, BatBox, battery, Macerator, Electric Furnace, Extractor, Compressor, basic Solar Panel, Pump, and Recycler. Machines are in the Redstone creative tab. Testing on NeoForge 21.1.234 verified EU transfer, hopper automation, battery charge transfer, solar shutdown without light, snowball production from water, and scrap/scrap metal production.


---

## 🚧 Current Limitations

### Porting Limitations

Crafting for other machines, full machine recipe lists, advanced network tiers, and compatibility with original saves still require further porting.

---

## 📜 License

The original mod is distributed under **All Rights Reserved**. This repository contains the prototype implementation, models referencing locally imported textures, and build configuration. [Original mod license](https://www.curseforge.com/minecraft/mc-mods/ic2-classic/license).

IV Transformer supports 8,192/32,768 EU packets and a 65,536 EU buffer. Redstone switches step-up mode. HV solar crafting uses eight MV panels and an IV Transformer; the IV survival recipe awaits its original late-game materials.

### Extended EU storage and portable batteries (0.57.0)

| Storage | Capacity (EU) | Output packet (EU) |
|---|---:|---:|
| ESU | 75,000,000 | 2,048 |
| ISU | 500,000,000 | 8,192 |
| PESU | 1,000,000,000 | 32,768 |

All six storage tiers support the existing two-slot charging interface, top/bottom hopper automation, directional EU output, and persistent charge. Shift-right-click with an empty hand cycles eight original redstone modes: no output, full, partially filled, at least half, at most half, below one packet, stop EU output while powered, and output only when full while powered. Comparators report charge independently of the selected mode.

Glowtronic Crystals store 7,500,000 EU, UESCs 17,500,000 EU, PESDs 50,000,000 EU, Quantum Accumulators 900,000 EU, and Large Quantum Accumulators 5,400,000 EU. Hold right-click with any battery to transfer energy to compatible hotbar items, then the offhand; empty batteries and batteries with no eligible target stop using automatically. Tools cannot supply energy.

The Alloy Furnace makes Pulsating Quartz from two redstone dust and one quartz. Pulsating Quartz and magnets unlock the original Complex Circuit and Glowtronic Crystal recipes, followed by ESU and ISU crafting. Both Quantum Accumulator recipes are available. PESD and PESU are currently available through Creative; their survival recipes await the plasma and reinforced/iridium stone branches. Original battery armor modules and storage slot expansions remain unported.

Run the standalone storage boundary tests without building a mod JAR:

```powershell
.\scripts\Test-StorageControl.ps1
```

If an IDE client shows new items with missing models or untranslated names, run `.\scripts\Sync-DevResources.ps1` and press F3+T. The IDE launch reads `build/resources/main`; compiling Java alone does not refresh these assets. The script validates JSON and expands mod metadata without building a JAR.
