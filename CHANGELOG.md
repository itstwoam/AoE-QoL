# Changelog

Releases come out on Fridays and include everything since the previous release.

## 1.1.0 (Friday 2026-10-23)

### Just Enough Calculation
- The calculator step list shows every output of a multi-output recipe (e.g. both tiny U-235 and U-238 from a Thermal Centrifuge).

### IndustrialCraft 2
- New JEI page for Reinforced Stone (Iron Scaffold + construction foam).

### RFTools (crafting grid and Crafter)
- Charged batteries and energy crystals, and worn tools, are recognized (e.g. crafting an MFE).
- The right variant is picked when it's stored in NBT (cable insulation, Mekanism tiers).
- Other mods' ore dictionary equivalents are used before other variants of the same item (e.g. IE iron plates for the Basic Machine Casing).
- The Crafter's ghost slots accept charged or worn copies of the set item.

### Ender IO
- Fixed a crash when a mod charges a creative capacitor bank, e.g. a Draconic Evolution capacitor in "charge held item" or "charge inventory" mode.

### BetterFPS
- BetterFPS's "Async" creative search is switched to "On" because it can crash the game (e.g. with Mekanism cables); search stays fast. You're told once in chat. To keep Async, set `preventAsyncCreativeSearch=false` in `config/aoeqol.cfg`.

### Other
- Credits: author name capitalized (Itstwoam), Age of Engineering credited to davqvist, RFTools, Ender IO and BetterFPS added.

## 1.0.0

First release: Just Enough Calculation fixes (ignored tags and damage, `/jectags`, ore dictionary unification, invisible-items fix) and JEI pages for the IC2 canning machines.
