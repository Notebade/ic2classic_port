<div align="center">

# ⚡ IC 2 Classic Re

### IndustrialCraft Classic for Minecraft 1.21.1

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-62B47A?style=for-the-badge)
![NeoForge](https://img.shields.io/badge/NeoForge-21.1.219-F07C32?style=for-the-badge)
![Java](https://img.shields.io/badge/Java-21-3973B9?style=for-the-badge)
![Version](https://img.shields.io/badge/Version-0.67.0-8E78C9?style=for-the-badge)

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
| Project version | `0.67.0` |
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

### Induction furnaces (0.58.0)

Induction Furnace, Induction Blast Furnace and Induction Smoker process smelting, blasting and smoking recipes, including data-pack recipes. Craft each with seven copper ingots, its electric predecessor and an Advanced Machine Casing. Each stores 10,000 EU and accepts packets up to 128 EU. A discharge slot accepts compatible batteries; automation supplies ingredients from above, extracts products and empty batteries below, and accesses both inputs and the battery from the sides.

Heat rises by one per tick to 10,000 while processing or maintained by redstone, costing 1 EU/t. Without heating it falls by four per tick. Processing adds 15 EU/t for the ordinary furnace or 29 EU/t for the other two. At full heat a standard batch takes 12 or 6 ticks respectively and can process two inputs. Custom cooking times scale the operation length. The interface shows charge, heat and progress; its button switches between separate and shared output slots. Shared mode can process two items from one occupied input. Changing mode or input type resets progress. Inventory, EU, heat, progress and pending experience survive reloads. Manual product collection or breaking the machine awards accumulated XP.

JEI lists the machines alongside vanilla cooking recipes. Wrenches rotate and dismantle them. Original upgrade slots, processing sounds and recipe-unlock statistics remain unported. The GUI uses the prototype style rather than the original artwork. These machines require a client restart and Java recompilation; F3+T alone only refreshes resources.

```powershell
.\scripts\Test-InductionCycle.ps1
```

The standalone checks live in `src/verification/java` and run automatically through `check` / `build` as `verifyStorageControl` and `verifyInductionCycle`. They are executable Java checks rather than JUnit tests. NeoGradle default run generation is disabled; the explicitly configured client and server runs remain available.

### Advanced processors (0.59.0)

Rotary Macerator, Centrifugal Extractor and Singularity Compressor use the existing maceration, extraction and compression recipes. Their inventory layouts match the original: one input and respectively two, three or one output slots, plus a battery discharge slot. Output is pooled automatically; only induction furnaces offer the split/shared toggle. All six advanced machines share one implementation while retaining the existing induction block, menu and block-entity registry identifiers.

Each new processor stores 10,000 EU and accepts packets up to 128 EU. Speed/pressure rises from 0 to 10,000 by one per tick while working or maintained by redstone, at 1 EU/t; processing costs another 15 EU/t. Without heating/spin-up, speed falls by four per tick. A standard operation at maximum speed takes 12 ticks and processes one recipe, never two. Rotary Macerator data-pack cooking times scale this duration. The Singularity Compressor consumes the full compression input count, or 1,000 mB water from an adjacent Pump to produce a snowball when the input is empty.

Craft the machines by surrounding the basic machine and an Advanced Machine Casing with seven Scrap Metal Blades, Electrolyzed Water Cells or obsidian respectively. Make a Scrap Metal Chunk by compressing eight Scrap Metal, then surround it with eight flint to craft a blade. The alternate Rotary Macerator recipe using stacked Obsidian Blades remains unported.

Battery charging tiers, side automation, wrench handling, saved progress and accumulated XP follow the advanced furnace implementation. JEI has separate categories showing the advanced EU cost and variable speed rather than the basic machine's fixed duration. Original upgrade slots, sounds and additional original machine recipe lists remain pending. These new machines have not yet been verified in-game.

Check compilation and all three standalone suites without packaging a JAR:

```powershell
.\gradlew.bat check --offline
```

### Rare-earth centrifuge and Charged Electrolyzer (0.60.0)

The Centrifugal Rare Earth Extractor has one input, one dust output and a battery discharge slot, a 5,120 EU buffer and a 128 EU packet limit. Speed increases by one per tick to 10,000 for 1 EU/t and decreases by four when unpowered; processing costs an additional 15 EU/t. Redstone maintains speed without input. The original 99-unit operation advances by speed / 300 each tick: one input takes three ticks at full speed. With insufficient EU, unfinished operation progress decays; accumulated material yield remains stored. Changing the dust type clears its partial material yield. Exact integer fractions preserve outputs such as two dusts from five soul sand or two clay balls, and survive save/reload. The GUI shows both speed and accumulated material.

Craft it with a Rare Earth Extractor, an Advanced Machine Casing and seven Electrolyzed Water Cells. The Charged Electrolyzer uses an Electrolyzer, an Advanced Machine Casing and seven LV Transformers. It processes every adjacent storage block each tick, with the original 70% charging and below-30% recovery thresholds and tier-based rates. The basic Electrolyzer continues to use one adjacent storage block. Existing electrolysis recipes, sided item automation and saved partial charge apply to both. Their persistent buffer now holds 24,000 EU so recovering energy beside a PESU cannot lose 4,000 EU on reload.

Both machines include original textures, crafting, translations, mining tags and JEI support. Original upgrade slots, sounds and additional recipe lists remain pending; in-game verification is still required. Gradle check now executes five standalone suites, including exact rare-earth accumulation and electrolyzer thresholds at billion-EU capacities. No JAR is required for these checks.

### Slow Grinder and Compacting Recycler (0.61.0)

The Slow Grinder is a recycler, not an ore macerator. Its nine input slots feed one active processing slot through an automatic rotation every ten ticks. It has one output slot, a battery discharge slot, a 1,000 EU buffer and a 128 EU packet limit. The mode button cycles speeds 1-5: each working tick costs 10/20/30/40/50 EU and advances work by 1/2/3/4/5. An ordinary recipe requires 100 work, so it takes 100/50/34/25/20 ticks. Original chance overrides are 100/84/71/59/50 percent for chance-based recycling recipes, including scrap metal. Guaranteed recipes remain guaranteed. Scrap-box crafting consumes nine scrap and requires 168 work. Mode, queue, inventory, EU and operation progress are saved. Adding items to waiting slots does not reset the active operation.

The Compacting Recycler stores 10,000 EU and accepts packets up to 128 EU. Its speed rises by one per tick to 10,000 for 1 EU/t, and processing costs another 10 EU/t. Redstone maintains the speed without input; without power it falls by four. At full speed ordinary recycling takes one tick with the original recycling chances. Packing nine scrap uses the original additive recipe-time modifier and takes 72 ticks at full speed. Both machines respect the recycler blacklist, support batteries and sided automation, and appear in JEI with their own costs and chances.

Primary original crafting chains are included: seven pistons upgrade a Recycler and Advanced Machine Casing into a Compacting Recycler. The Slow Grinder uses an Efficient Sawblade, two Advanced Circuits, a Recycler and an Advanced Machine Casing. Six refined iron and a Treetap make eight Mining Pipes; pipes, circuits and insulated wire produce Upgrade Bases in copper (three) or silver (five) variants. Sticks, a Scrap Metal Blade, pistons and an Upgrade Base make an Efficient Sawblade. Mining Pipes cannot be placed by hand, matching the original; Miner placement/retraction is not ported. The Efficient Sawblade currently serves as a crafting component: installing it as a Sawmill upgrade and its repair recipe remain pending.

All assets, mining tags, localization and saved-state fields are included. Six standalone suites run through Gradle check, including queued inventory layouts, rotation limits, mode probabilities, EU costs and both compacting durations. In-game verification, original sounds and installed machine upgrades remain pending.

### Pressure Alloy Furnace (0.62.0)

Build a 2x2x2 cube from one Pressure Alloy Furnace controller and seven Pressure Furnace Casings. Viewed from the controller's front, the controller is the upper front-left corner; the cube extends to the right, backward and downward. All four horizontal orientations are supported. The controller recipe consumes one casing, so two casing crafts (four each) supply the controller ingredient plus the seven placed casings.

The completed structure stores 10,000 EU and accepts packets up to 512 EU. EU, sided item automation and menu access work through the controller or any linked casing. One casing cannot belong to two complete furnaces. Breaking, unloading or rotating the structure into an invalid arrangement stops operation and clears heat/progress while preserving EU and inventory. Cached casing item handlers re-check their controller on each operation. No neighboring chunks are forcibly loaded.

Two ingredient slots feed one shared output and one battery discharge slot. Heating uses 1 EU/t, processing another 15 EU/t, with the same 0-10,000 heat range and redstone maintenance as the Induction Furnace. A standard alloy batch takes 12 ticks at full heat. It uses the Alloy Furnace recipe set, including recipes added in this version: two Netherite Scraps plus two gold ingots yield one Netherite Ingot (the original IC2 recipe), and one copper block plus three tin ingots yield twelve bronze ingots. Both furnaces now enforce and display the count of each ingredient and accept reversed input slots.

Primary controller and casing crafts, original texture references, translations, mining tags, wrench handling and JEI assembly guidance are included. Formed casings currently retain the original unformed machine-casing appearance rather than the original connected multiblock artwork. Original upgrades and sounds remain pending. Compilation and eight standalone suites pass through Gradle check; live multiblock behavior, relinking and chunk-boundary operation still require in-game validation.

### Solar Turbine and Slag Generator (0.64.0)

The Solar Turbine heats by 2 units/tick in sunlight up to 24,000 heat and cools by 1/tick without sunlight. Sunlight is sampled on load and every 128 ticks using the existing biome/weather/sky rules. Water and stored heat generate energy even at night. With the original default multiplier, output is `water * heat / 30,000,000` EU/t, up to 32 EU/t; fractional EU and water consumption are accumulated exactly and saved. Its nominal energy buffer is 100 EU; the original final generation can overshoot it to 131 EU. It charges LV items and emits packets up to 32 EU on every side.

As in the original, incoming water doubles into internal tank units: 20,000 mB input fills a 40,000-unit tank. At full heat it consumes four internal units/tick. Water is still consumed while the EU buffer is full. Fill using water containers in the input slot, right-click a container or use a fluid handler; extraction is disabled. Container remainders go to the output. Item access from above is disabled; horizontal sides accept water containers and expose the output/charging slot, while the bottom exposes the output and completed battery.

The Slag Generator emits 60 EU/t, stores 8,000 EU and charges items up to the 128 EU tier. Furnace fuel duration is divided by 16: coal lasts 100 ticks and yields 6,000 EU. Scrap retains its original 350 furnace burn ticks, then receives the additional tenfold slag-fuel reduction: two ticks / 120 EU, with no scrap byproduct. Lava is excluded by the original fuel lookup. Other fuels produce one Scrap every randomly selected 80-239 burning ticks. Once ignited, fuel continues burning at a full EU buffer; new fuel waits until at least 60 EU of space is available. Excess Scrap is discarded when its output is blocked, matching the original. Fuel containers and their remainders are preserved.

Both generators have original primary crafting recipes, textures, translations, mining tags, wrench support, charging slots, sided automation and JEI categories. The UI shows EU, production and heat/water or remaining fuel. The EU routing interface now passes the source storage-output value directly, allowing these generators to use the existing network without pretending to be another machine type. Original sounds and configurable production multipliers remain pending. Ten standalone suites pass through Gradle check; live generator, pipe and weather behavior still require in-game validation.

### Energy, steam and fusion (0.64.0)

Adds an adjustable transformer, three one-way EU-to-FE converters, plasmafier, hollow fusion reactor, fuel boiler and steam tunnel, structure ports, two basic tanks, superconductor/plasma cables and PESD/PESU/IV crafting. The fuel chain includes compression, extraction and refillable cans. Counted crafting consumes the original per-slot amounts for plasma cores and boiler casings.

See [assembly, operating values and current limitations](docs/energy-fusion.md). Tank/valve recipes and can filling are temporarily adapted; windmills, liquid-fuel generation, redirectors and charging infrastructure remain. Superconductor production still needs the unported refinery. Original presentation and in-world behavior require further validation.

### Addon API v1 (0.65.0)

Public ic2.api interfaces expose registry lookups, immutable server-side machine telemetry, compression/extraction recipe additions and EU network integration for addon machines. Read [API.md](API.md) for lifecycle rules, units, dependency setup and examples. A separate example addon is compile-checked by check and excluded from the IC2 JAR. scripts/Test-AddonApi.ps1 verifies the API with an actual addon on an isolated dedicated server.

### Windmill and liquid fuel generation (0.66.0)

Adds a windmill with six replaceable rotors, airflow sharing, rotor wear and a rotating sprite renderer, plus a liquid-fuel generator for wood gas, liquid biofuel and alcohol. Includes EU output, battery/container handling, persistence, recipes, JEI, fluid cells and API telemetry. [Operating values and remaining limitations](docs/wind-and-liquid-generators.md). Manufacturing the fuel liquids awaits gasifiers/refinery; client visuals and original sounds still need work.

### Gasifiers and refinery (0.67.0)

Adds solid-fuel/electric wood gasifiers and a refinery with two inputs and a shared layered fluid output. Closes wood gas, liquid biofuel, alcohol and polished-gold production. A whisky-only barrel foundation supplies the original alcohol dependency; its default server ageing reaches distillable whisky after seven loaded days. Includes flour processing, recipes, containers, automation, JEI and API telemetry. [Operation, timing and remaining scope](docs/gasifiers-and-refinery.md).
