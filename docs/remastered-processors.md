# Implemented processor GUI coverage

All listed machines use ProcessingMachineScreen and the shared search, ScrollPanel and recipe loading interface.

| # | Block ID | Gallery X | Gallery Z |
|---|---|---:|---:|
| 1 | ic2:stone_macerator | 1000 | 1000 |
| 2 | ic2:iron_furnace | 1006 | 1000 |
| 3 | ic2:macerator | 1012 | 1000 |
| 4 | ic2:electric_furnace | 1018 | 1000 |
| 5 | ic2:electric_blast_furnace | 1000 | 1006 |
| 6 | ic2:electric_smoker | 1006 | 1006 |
| 7 | ic2:sawmill | 1012 | 1006 |
| 8 | ic2:alloy_smelter | 1018 | 1006 |
| 9 | ic2:extractor | 1000 | 1012 |
| 10 | ic2:compressor | 1006 | 1012 |
| 11 | ic2:recycler | 1012 | 1012 |
| 12 | ic2:rare_earth_extractor | 1018 | 1012 |
| 13 | ic2:uranium_enricher | 1000 | 1018 |
| 14 | ic2:canner | 1006 | 1018 |
| 15 | ic2:vacuum_canner | 1012 | 1018 |
| 16 | ic2:electrolyzer | 1018 | 1018 |
| 17 | ic2:charged_electrolyzer | 1000 | 1024 |
| 18 | ic2:mass_fabricator | 1006 | 1024 |
| 19 | ic2:pump | 1012 | 1024 |
| 20 | ic2:induction_furnace | 1018 | 1024 |
| 21 | ic2:induction_blast_furnace | 1000 | 1030 |
| 22 | ic2:induction_smoker | 1006 | 1030 |
| 23 | ic2:rotary_macerator | 1012 | 1030 |
| 24 | ic2:centrifugal_extractor | 1018 | 1030 |
| 25 | ic2:singularity_compressor | 1000 | 1036 |
| 26 | ic2:centrifugal_rare_earth_extractor | 1006 | 1036 |
| 27 | ic2:compacting_recycler | 1012 | 1036 |
| 28 | ic2:slow_grinder | 1018 | 1036 |
| 29 | ic2:pressure_alloy_furnace | 1000 | 1042 |
| 30 | ic2:wood_gassifier | 1006 | 1042 |
| 31 | ic2:electric_wood_gassifier | 1012 | 1042 |
| 32 | ic2:refinery | 1018 | 1042 |

Gallery: runs/processing-gallery-final/saves/verification. Floor Y=80, machine Y=81. Start at X=1008, Y=81, Z=994. Numbered signs stand in front of every machine.

Generators, storage, reactors, monitors and unported machines are excluded. Fluid-only requirements are displayed with fluid icons and tooltips; Create does not fabricate or move tank fluids. Pump and refinery recipes require their existing fluid supply.

Validation: 32 work screens and 32 recipe screens opened in the real client. Thirty processors with item-loadable recipes passed exact batch and Shift batch loading through server menus, including item conservation. Two fluid-only routes correctly reject inventory-only loading. The alloy regression suite also checks search, duplicate requests, rollback and actual scrollbar dragging.
