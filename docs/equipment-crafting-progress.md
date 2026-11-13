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
