# AoE-QoL

Quality-of-life fixes for the **Age of Engineering** modpack (Minecraft **1.10.2**, Forge **12.18.3.2511**), as one small add-on jar.

Drop `AoE-QoL-<version>.jar` into your `mods` folder. Nothing else needs replacing: each part switches itself on only when the mod it works with is installed. Fixes to other mods are applied **in memory while they load** (a small coremod); their jars on disk are never modified, and if a mod version doesn't have what a fix expects, that fix is skipped and logged instead of crashing.

New versions come out on **Fridays** and bundle everything since the previous one; see [CHANGELOG.md](CHANGELOG.md).

| Part | Needs | What it does |
|---|---|---|
| [Just Enough Calculation](#just-enough-calculation) | JEC 1.10.2-2.6.5 | Chains recipes for items that carry state, finds the tag that blocks a chain, unifies ore dictionary variants, full step list, invisible-items fix |
| [IC2](#ic2) | IC2 2.6.252-ex110 (+ JEI) | JEI pages for the canning machines and Reinforced Stone; comparators follow energy storage levels; networks no longer go dead after being split |
| [RFTools](#rftools) | RFTools 1.1x-5.92 | Crafting grid and Crafter find charged/worn items, the right variant, and other mods' equivalents; Storage Scanner search synced with JEI |
| [Ender IO](#ender-io) | Ender IO 1.10.2-3.1.193 | No crash when another mod tries to charge a creative capacitor bank |
| [Minecraft](#minecraft) | — | No crash on startup on Turkish/Azerbaijani systems; no freeze on "Save and Quit to Title" |
| [BetterFPS](#betterfps) | BetterFPS 1.4.5 | Switches the crash-prone "Async" creative search to "On" (still fast) |

> Written for and tested against the exact mod versions in Age of Engineering 1.1.2. Tests run outside the game (see [Testing](#testing)); please [report](#issues-and-requests) anything that misbehaves in game.

---

## Just Enough Calculation

* **Items that only differ in condition now match.** JEC treated items as different if any NBT tag or damage value differed, so IC2 reactor parts (heat), batteries and crystals (charge) never chained. Two config lists decide what to ignore (see below); IC2 is covered by default.
* **Finding the tag that blocks a chain:** `/jectags`, a report file and F3+H tooltips (see below).
* **Ore dictionary unification.** Choosing an ore dictionary preference (during a JEI transfer, or on the calculator's *oreDict* screen) rewrites every saved recipe to use it; recipes saved later are unified automatically, and `/jecunify` applies all preferences at once. Items are only swapped when they have exactly the same set of ore names (`S:ListOreUnifyNames`, default `ingot* plate* dust* nugget* gear* rod* stick* wire* block* gem*`). Every change is logged to `config/JustEnoughCalculation-OreUnify-log.txt`. There's no undo, so back up `saves/<world>/playerdata/*.jeca` before the first `/jecunify`.
* **Every output in the step list.** A step using a multi-output recipe (e.g. a Thermal Centrifuge) lists all its outputs, totalled over all runs, not just the one it was run for.
* **Items invisible in your inventory after using JEC.** JEC left its closed screen's container active on the client, so items picked up into the main inventory didn't show until you clicked a slot. Fixed.

### Ignoring condition tags and damage

Two lists in `config/JustEnoughCalculation.cfg` (also in game: Mods → Just Enough Calculation → Config):

```
S:ListIgnoredNBTKeys <      # <item>@<tag>
    ic2:*@advDmg
    ic2:*@charge
 >
S:ListIgnoredDamage <       # <item>
    ic2:*
 >
```

`<item>` is `modid:itemname`, `modid:*` (whole mod) or `*` (everything, use with care).

* **Only list tags that store condition** (heat, charge, energy, wear, fill level). Tags that say *what* an item is (cable type, material, wood, fluid, mob, JEC label names) must not be listed, or different items will merge.
* Damage is only ignored for items that can actually take damage (tools, batteries). Items that use damage values for variants (plates, dusts, cables, wool...) are never affected.

### Finding the tag that blocks a recipe chain

When two copies of the **same** item fail to match only because of a tag or damage value, AoE-QoL records the item, the tag and both values.

* **`/jectags`** lists them in chat (client-side: works in single player without cheats and on servers). **`/jectags clear`** starts a fresh list.
* **`config/JustEnoughCalculation-TagReport.txt`** has the same list with ready-to-paste config lines, for one item or the whole mod.
* **Advanced tooltips (F3+H)** show an item's tags under its name, e.g. `Tags: charge=1000000.0d (ignored)`.

Rule of thumb: a tag that one copy has and the other doesn't, or a number that changes as you use or charge the item, is usually condition. Both copies having the tag with different names or ids (`type: 0b` vs `4b`, `"oak"` vs `"spruce"`) means they really are different items; leave it alone.

---

## IC2

* **Canning machines in JEI.** IC2 never gave the canning machines JEI pages. AoE-QoL adds *Solid Canning* (e.g. empty fuel rod + uranium fuel -> fuel rod) and *Fluid Enriching* (e.g. distilled water + lapis -> coolant, with an empty cell filled into a Coolant Cell, which is how `R` on a Coolant Cell finds it), drawn with the Fluid/Solid Canning Machine's own GUI and read from IC2's recipe lists, so pack scripts are included. If JEC's transfer button doesn't appear on the new pages, restart once more.
* **Reinforced Stone in JEI.** It has no crafting recipe: spray an Iron Scaffold with construction foam (100 mB) and mine the hardened block. A Reinforced Iron Scaffold also gives back its Iron Fence. Hover the arrow for hardening times (about 10 minutes in full light, slower in the dark, instant with sand).
* **EU-Splitter Cable and redstone.** The splitter stops EU while it gets a redstone signal, but IC2 refused redstone connections to it, so Ender IO redstone conduits didn't output to it without a Yeta Wrench and redstone dust didn't bend into it. It now accepts redstone connections (setting: *EU-Splitter Cable connects to redstone*). Other IC2 blocks are unchanged. Existing dust and conduits pick it up when a block next to them changes.
* **Comparators on energy storage.** BatBoxes, CESUs, MFEs and MFSUs (and their chargepads) report their fill level to comparators but never told the world when it changed, so comparators and things like Super Circuit Maker inventory scanners kept showing an old value until a block next to them changed. They now update whenever the level changes, and once when the world loads.
* **Networks going dead after a split.** IC2 keeps a list of routes (generator to machine) for each cable network and only recalculates a network when it's told it changed. When a cable, splitter or machine leaves a network and splits it in two, IC2 keeps one piece as the old network (recalculated) and moves the rest into new networks that it never calculates. Their generators and machines stayed connected but got no power until something was placed in them or the world was reloaded. Splits happen when a cable is broken, when an EU-Splitter Cable in the middle of a run switches off, or when part of a network unloads. New networks are now calculated right away.

---

## RFTools

### Crafting

Applies to the crafting grid in the Storage Scanner and Modular Storage, and to the Crafter block. RFTools looks for each ingredient with a strict test (same item and damage value) and, if that fails, a loose one (same item in any variant, or a shared ore dictionary name); the mod's own recipe check then decides.

* **Charged batteries and crystals, worn tools:** the strict test ignores the damage value of items that can take damage, since for those it's only charge or wear (e.g. crafting an MFE no longer reports the crystals missing).
* **The right variant:** items whose NBT matches the recipe's exactly are taken first (2× insulated gold cable, a Basic Energy Cube), ignoring the tags on `ListIgnoredNBTKeys`.
* **Other mods' equivalents:** in the loose test, items sharing an ore dictionary name (IE iron plates for IC2's) come before other variants of the same item (IC2 copper plates).
* **Showing what's wrong:** when the grid finds everything but the recipe still refuses it, RFTools could only mark the result red. If a slot was filled with a different variant than the recipe shows (e.g. uninsulated cable where the EU-Splitter needs fully insulated HV cable), that slot is now marked red too.
* The Crafter's ghost-slot filter accepts charged or worn copies of the set item.

A wrong pick can only make a craft fail, never change it: the recipe check still runs. IC2 crystal charge isn't carried into an MFE (that's IC2's recipe), so drain them first.

### Storage Scanner search synced with JEI

Like AE2's "JEI synchronized" search: while the Storage Scanner is open, typing in its search box fills in JEI's search box and the other way round, and opening the scanner takes over what JEI's box already holds. The scanner's own item filter and container highlighting work as usual. Turn it off with **Sync Storage Scanner search with JEI** in the [settings](#settings).

---

## Ender IO

* **Creative capacitor bank crash.** Ender IO's creative bank item claimed to have an energy capability but then handed out none, so mods that try to charge it crashed, e.g. a Draconic Evolution capacitor in "charge held item" or "charge inventory" mode while holding or carrying the bank. The bank now consistently reports no capability; other banks are unchanged.

---

## Minecraft

* **Freeze on "Save and Quit to Title".** When the world closes, the game hands its built-in server one last job (log out anyone still connected) and waits for it with no time limit. Your disconnect also makes the server shut down, and now and then the server stops a split second before picking the job up, so the game waits forever. It happens more often after opening the world to LAN. The game now runs the job itself once the server has fully stopped, which is what Minecraft already does when the job arrives later. By then everyone has been logged out and saved, so nothing is skipped and normal quits are unchanged.
* **Crash on a Turkish or Azerbaijani system.** Many mods in the pack (Extreme Reactors, Mekanism, Compact Solar Arrays, AE2, Draconic Evolution and more, and Minecraft itself for mod IDs) build internal names by lower-casing with the computer's language rules. In Turkish and Azerbaijani a capital I becomes a dotless "ı", so e.g. `IDLE` turns into `ıdle`, which Minecraft rejects, and the game fails to start. Before any mod loads, AoE-QoL switches Java's text case rules to neutral on those systems. Number and date formats stay as they are, and Minecraft's in-game language isn't affected. The player is told once in chat. This isn't a setting, since turning it off would only bring the crash back.

---

## BetterFPS

* **Creative search crash.** BetterFPS's "Async" creative search starts a background thread for every key you type, and each one runs every mod's tooltip code off the main thread. That code isn't thread-safe, and in Age of Engineering it crashes the game (for example while Mekanism draws its cables in the search results). AoE-QoL switches the setting to "On" for the session: BetterFPS's fast search still runs, just on the main thread. BetterFPS's own config file isn't changed, and you're told once in chat.
* **To keep Async anyway**, turn off **Prevent async creative search** in the [settings](#settings); it applies the next time you open the creative inventory.

---

## Settings

**Mods → AoE-QoL → Config** in game (changes apply without a restart), or `config/aoeqol.cfg`:

| Setting | Default | |
|---|---|---|
| Prevent async creative search (`preventAsyncCreativeSearch`) | on | BetterFPS creative search uses "On" instead of "Async" |
| Sync Storage Scanner search with JEI (`syncStorageScannerWithJei`) | on | Storage Scanner and JEI share one search |
| EU-Splitter Cable connects to redstone (`splitterCableConnectsRedstone`) | on | Redstone dust and conduits connect to the splitter cable |

A section only shows in game when the mod it's about is installed. The Just Enough Calculation lists live in JEC's own config (Mods → Just Enough Calculation → Config).

---

## Issues and requests

Found a recipe that won't chain in JEC, a missing or wrong JEI page, a crash, or have another quality-of-life change you'd like to see for Age of Engineering? Post it in the **Comments** on the mod's CurseForge page, or send Itstwoam a message on CurseForge. Every report gets reviewed.

Copy this into your comment and fill in what applies:

```
Problem:   (recipe won't chain / wrong or missing JEI page / crash / idea / other)
Item, mod: (e.g. Uranium Fuel Rod, IndustrialCraft 2)
What happens:
What you expected:
/jectags lines: (for recipe problems)
AoE-QoL version: (e.g. 1.1.0)
Pack: (Age of Engineering version, plus any mods you added or updated)
```

For a crash, attach the file from the `crash-reports` folder (or `logs/latest.log` if there is none).

## Building

Needs **Java 8**. The first build downloads and decompiles Minecraft (several minutes).

```
./gradlew setupDecompWorkspace   # once
./gradlew build                  # -> build/libs/AoE-QoL-<version>.jar
```

JEI, IC2 and JEC are compile-only dependencies; none of them is included in the jar. ForgeGradle 2.2 with Gradle 2.14.1 prints an "unsupported version" warning, which is expected.

## Testing

Every fix is tested outside the game: the transformer is run over the unmodified JEC, RFTools, Ender IO and IC2 jars, the Turkish fix is run under Turkish, Azerbaijani and English locales, the IC2 network split is replayed on IC2's real energy-net classes (one half goes dead without the fix, both stay powered with it), the quit fix is run against a server that stops early and one that doesn't, the BetterFPS change is run against BetterFPS's real config code (only the intended methods change, and the patched classes pass type-checking bytecode verification against the real libraries), and the new code runs against stand-in Minecraft classes under full JVM verification, with and without the other mods installed. Every Minecraft method and field the jar uses is checked against the MCP mappings for Forge 12.18.3.2511.

## License

[LGPL-3.0](LICENSE) (the same license as Just Enough Calculation), which builds on the GPL-3.0 text in [COPYING.GPL](COPYING.GPL).

## Credits

* Written by Itstwoam with Claude (Anthropic).
* [Just Enough Calculation](https://github.com/Towdium/JustEnoughCalculation) by Towdium, [IndustrialCraft 2](https://www.curseforge.com/minecraft/mc-mods/industrial-craft) by the IC2 team, [JEI](https://github.com/mezz/JustEnoughItems) by mezz, RFTools by McJty, Ender IO by the Ender IO team, BetterFPS by Guichaguri, and Age of Engineering by davqvist. This project is not affiliated with any of them.
