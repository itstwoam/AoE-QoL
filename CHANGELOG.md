# Changelog

Releases come out on Fridays and include everything since the previous release.

## 1.1.0 (Friday 2026-10-23)

### Just Enough Calculation
- The calculator step list shows every output of a multi-output recipe (e.g. both tiny U-235 and U-238 from a Thermal Centrifuge).

### IndustrialCraft 2
- New JEI page for Reinforced Stone (Iron Scaffold + construction foam).
- The canning machine's Fluid Enriching page also shows the empty cell -> filled cell slots, so Coolant Cells (for 10k Coolant Cells and Overclocker Upgrades) show their recipe in JEI.
- Redstone dust and Ender IO redstone conduits connect to the EU-Splitter Cable by themselves (no Yeta Wrench needed); can be turned off.
- Comparators (and Super Circuit Maker inventory scanners) next to a BatBox, CESU, MFE or MFSU now follow its fill level instead of keeping an old value.

### RFTools (crafting grid and Crafter)
- Charged batteries and energy crystals, and worn tools, are recognized (e.g. crafting an MFE).
- The right variant is picked when it's stored in NBT (cable insulation, Mekanism tiers).
- Other mods' ore dictionary equivalents are used before other variants of the same item (e.g. IE iron plates for the Basic Machine Casing).
- When the wrong variant is all you have (e.g. uninsulated cable for an EU-Splitter Cable), the grid marks that ingredient red instead of only the result.
- The Crafter's ghost slots accept charged or worn copies of the set item.
- The Storage Scanner's search box and JEI's search box stay in sync, like AE2's "JEI synchronized" search (can be turned off).

### Ender IO
- Fixed a crash when a mod charges a creative capacitor bank, e.g. a Draconic Evolution capacitor in "charge held item" or "charge inventory" mode.

### Turkish system language
- Fixed the game failing to start when the computer's language is Turkish or Azerbaijani (Extreme Reactors, Mekanism, Compact Solar Arrays and other mods). Number formats and Minecraft's language are unchanged. You're told once in chat.

### BetterFPS
- BetterFPS's "Async" creative search is switched to "On" because it can crash the game (e.g. with Mekanism cables); search stays fast. You're told once in chat. To keep Async, turn it off in the settings.

### Other
- In-game settings: Mods → AoE-QoL → Config (also `config/aoeqol.cfg`).
- Credits: author name capitalized (Itstwoam), Age of Engineering credited to davqvist, RFTools, Ender IO and BetterFPS added.

## 1.0.0

First release: Just Enough Calculation fixes (ignored tags and damage, `/jectags`, ore dictionary unification, invisible-items fix) and JEI pages for the IC2 canning machines.
