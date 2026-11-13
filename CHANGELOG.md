# Changelog

Releases come out on Fridays and include everything since the previous release.

## 1.2.0 (unreleased)

### Just Enough Calculation
- The calculator step list is in crafting order: every step comes after the steps that make its ingredients. Before, an item made in batches could be listed after something that needs it (e.g. Iron Tubing, 16 per craft, after the Pressure Valve made from it).
- Actually Additions battery charge (`actuallyadditions:*@Energy`) is ignored by default, so recipes with batteries chain (e.g. the Triple Battery). Only new configs get the default; existing ones can add the line to `ListIgnoredNBTKeys`.
- With the calculator's inventory check, other mods' versions of an item in your inventory count toward recipes unified to your preferred one (e.g. IC2 copper plates when the recipes use IE's). Before, only the preferred item was counted (setting: Ore dictionary unification).
- The page arrows on the calculator, item picker and ore dictionary screens go the right way: "<" goes back a page and ">" goes forward, like the recipe list (setting: Page arrows the right way round).

### RFTools
- The Storage Scanner no longer counts the contents of vanilla double chests twice (64 clay dust showed as 128).
- Brand-new IC2 electric items (e.g. an empty Re-Battery) can be taken out of the Storage Scanner; clicking one used to hand over nothing (setting: Take out brand-new items).

### Advanced Solar Panels
- The Molecular Transformer turns nether quartz into certus quartz. The recipe was in its recipe file but used ore dictionary names nothing in the pack registers, so it was skipped at startup.

### Ender IO
- Item filters do what their mode says: an empty whitelist lets nothing through (before, it let everything through, like an empty blacklist; e.g. a Vacuum Chest with a blank whitelist filter picked up every item). New filter items still start as an empty whitelist, so they pass nothing until set up; a new Dimensional Transceiver's built-in filters start as blacklist, so it still passes everything until set up. Applies to conduits, the Vacuum Chest and the Dimensional Transceiver (setting: Filters do what their mode says).

### Minecraft
- The view-spin fix also catches jumps where the cursor moved a little just before the screen closed (one was missed by a single pixel on a 4K window).

### AoE-QoL settings
- Fixed setting changes being lost: pressing Done on a section (Minecraft, IC2, RFTools...) now saves right away. Before, a change was only kept if you also pressed Done on the main AoE-QoL screen afterwards.
- Escape on the settings screens now keeps your changes, like Done, instead of throwing them away.

## 1.1.0 (Friday 2026-10-23)

### Just Enough Calculation
- Fixed JEC showing no recipes (or another server's) in single player after you had been on a server; your recipes were never lost, only hidden until a restart.
- The calculator step list shows every output of a multi-output recipe (e.g. both tiny U-235 and U-238 from a Thermal Centrifuge).

### IndustrialCraft 2
- New JEI page for Reinforced Stone (Iron Scaffold + construction foam).
- The canning machine's Fluid Enriching page also shows the empty cell -> filled cell slots, so Coolant Cells (for 10k Coolant Cells and Overclocker Upgrades) show their recipe in JEI.
- Redstone dust and Ender IO redstone conduits connect to the EU-Splitter Cable by themselves (no Yeta Wrench needed); can be turned off.
- Comparators (and Super Circuit Maker inventory scanners) next to a BatBox, CESU, MFE or MFSU now follow its fill level instead of keeping an old value.
- Fixed cable networks going dead (no power until a cable was replaced or the world reloaded) after a network was split by breaking a cable, a splitter switching off, or part of it unloading.

### RFTools (crafting grid and Crafter)
- Charged batteries and energy crystals, and worn tools, are recognized (e.g. crafting an MFE).
- The right variant is picked when it's stored in NBT (cable insulation, Mekanism tiers).
- Other mods' ore dictionary equivalents are used before other variants of the same item (e.g. IE iron plates for the Basic Machine Casing).
- When the wrong variant is all you have (e.g. uninsulated cable for an EU-Splitter Cable), the grid marks that ingredient red instead of only the result, also when other ingredients are missing.
- The Crafter's ghost slots accept charged or worn copies of the set item.
- The Storage Scanner's search box and JEI's search box stay in sync, like AE2's "JEI synchronized" search (can be turned off).

### Integrated Dynamics and Extra Utilities 2
- Fixed a server crash when an Integrated Dynamics Redstone Reader faces an Extra Utilities 2 block such as the Scanner.
- Fixed logic cable arms showing only as an outline after connecting a cable or block to them.

### QuantumStorage
- Fixed a server crash when an Extra Utilities 2 transfer or retrieval node connects to a QuantumStorage Quantum Tank (nodes now use the tank's bucket slots).

### Ender IO
- Fixed a crash when a mod charges a creative capacitor bank, e.g. a Draconic Evolution capacitor in "charge held item" or "charge inventory" mode.

### Minecraft
- Fixed the view sometimes spinning wildly when closing an inventory, chest or machine screen (setting: Ignore leftover mouse jump, on by default).
- Fixed the game occasionally freezing on "Save and Quit to Title" (more often after opening the world to LAN). Your world was always saved; now the game also gets back to the title screen.
- Fixed the game failing to start when the computer's language is Turkish or Azerbaijani (Extreme Reactors, Mekanism, Compact Solar Arrays and other mods). Number formats and Minecraft's language are unchanged. You're told once in chat.

### BetterFPS
- BetterFPS's "Async" creative search is switched to "On" because it can crash the game (e.g. with Mekanism cables); search stays fast. You're told once in chat. To keep Async, turn it off in the settings.

### Other
- New settings, all on by default: the JEC condition-tag matching and ore dictionary unification, the RFTools ingredient matching, the IC2 split-network fix and the quit-freeze fix can now each be turned off. Crash fixes stay always on.
- In-game settings: Mods → AoE-QoL → Config (also `config/aoeqol.cfg`).
- Works on servers too: the server uses the same jar as the client. Neither side requires the other; each side gets the fixes that run on it.
- Credits: author name capitalized (Itstwoam), Age of Engineering credited to davqvist, RFTools, Ender IO, Integrated Dynamics, Extra Utilities 2, QuantumStorage and BetterFPS added.

## 1.0.0

First release: Just Enough Calculation fixes (ignored tags and damage, `/jectags`, ore dictionary unification, invisible-items fix) and JEI pages for the IC2 canning machines.
