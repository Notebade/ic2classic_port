# GUI integration into main — 2026-11-14

Merged `gui-only` at `c5d84bf` into main after `2c31d4e` for manual review.

- Kept main's advanced-machine upgrade slots, energy capacity and packet synchronization; placed variant and high heat in separate data fields.
- Included both upgrade slots in the recipe transfer inventory boundary and hid them outside the Work tab.
- Kept variable food-can quantities and processing times in the recipe browser.
- Combined GUI translations with the restored item catalog names and models.

Validation: offline Gradle build passed; full `Test-AddonApi.ps1` runtime suite passed, including all 480 item names and 790 model dependencies. These checks do not establish visual correctness of every merged screen. Manual GUI review uses the existing `IC2-Manual-Test` world with its 148 placed mechanisms and supply chests. Earlier gallery results in other documents describe the GUI branch before integration.
