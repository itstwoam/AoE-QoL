# AoE-QoL

Quality-of-life fixes for the **Age of Engineering** modpack (Minecraft **1.10.2**, Forge **12.18.3.2511**), as one small add-on jar.

Drop `AoE-QoL-<version>.jar` into your `mods` folder. Nothing else needs replacing: every part switches itself on only when the mod it works with is installed, and does nothing otherwise.

| Part | Needs | What it does |
|---|---|---|
| [Just Enough Calculation fixes](#just-enough-calculation-fixes) | JEC 1.10.2-2.6.5 | Makes the calculator chain recipes for items that carry state (IC2 reactor parts, batteries...), helps you find the tag that blocks a chain, unifies ore dictionary variants, cleans up the step list, and fixes items going invisible in your inventory after using JEC |
| [IC2 Canning Machine in JEI](#ic2-canning-machine-in-jei) | IC2 2.6.252-ex110 + JEI | Adds the canning machines' recipes to JEI, drawn like the machines' own screens, so pressing `R` on a uranium fuel rod, food can or coolant shows how it's made |

> **Status:** written for and tested against the exact mod versions in Age of Engineering 1.1.2. Tests run outside the game (see [Testing](#testing)); please [report](#issues-and-requests) anything that misbehaves in game.

---

## Just Enough Calculation fixes

JEC is patched **in memory while it loads** (a small coremod). The official JEC jar on disk is never modified, and if JEC isn't installed the patch simply never runs. If a different JEC version doesn't have a method the patch expects, that one change is skipped and logged instead of crashing.

### 1. Items that only differ in condition now match

JEC treats two items as different if their NBT tags or damage values differ at all. Some mods store an item's *condition* there, for example IC2's heat on vents, heat exchangers, plating, condensators, coolant cells and fuel rods (`advDmg`), and IC2's stored energy (`charge`, plus the damage value on batteries and crystals). A freshly crafted copy and one taken from JEI then never match, so the calculator can't chain those recipes.

Two lists are added to `config/JustEnoughCalculation.cfg` (also editable in game: Mods → Just Enough Calculation → Config):

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
* Damage is only ignored for items that can actually take damage (tools, batteries). Items that use damage values for variants (plates, dusts, cables, wool...) are never affected, even if they match an entry.

### 2. Finding the tag that blocks a recipe chain

The patch watches JEC's comparisons. When two copies of the **same** item fail to match only because of a tag or damage value, it records the item, the tag and both values.

* **`/jectags`**: lists them in chat (a client-side command: works in single player without cheats and on servers). **`/jectags clear`** starts a fresh list.
* **`config/JustEnoughCalculation-TagReport.txt`**: the same list with ready-to-paste config lines, for one item or the whole mod.
* **Advanced tooltips (F3+H)**: items show their tags under the name, e.g. `Tags: charge=1000000.0d (ignored)`.

Rule of thumb: a tag that one copy has and the other doesn't, or a number that changes as you use or charge the item, is usually condition. Both copies having the tag with different names/ids (`type: 0b` vs `4b`, `"oak"` vs `"spruce"`) means they really are different items; leave it alone.

### 3. Ore dictionary unification

JEC's own ore dictionary feature only picks *one* item when you transfer a recipe from JEI. Saved recipes hold exact items, so an Immersive Engineering tin plate never counts for a recipe that wants IC2's.

* Choosing a **preference** (the picker during a JEI transfer, or the calculator's *oreDict* screen) rewrites every saved recipe: items with exactly the same ore dictionary names as your choice become your choice, keeping their amounts. Older preferences for the same names are removed.
* Every recipe you **save or edit** afterwards is unified the same way, silently.
* **`/jecunify`** applies all your existing preferences to all saved recipes at once.

Setting: `S:ListOreUnifyNames` (default `ingot* plate* dust* nugget* gear* rod* stick* wire* block* gem*`; `*` works at the start or end; empty = off).

Safety rules: names are compared ignoring case (`plateTin` = `platetin`); both items must have the **same full set** of names (red dye `{dye, dyeRed}` never replaces blue dye `{dye, dyeBlue}`); wood isn't in the default list (picking oak would turn spruce logs into oak). Every change, and every item that was close but not changed, is written to `config/JustEnoughCalculation-OreUnify-log.txt`. There's no undo button, so back up `saves/<world>/playerdata/*.jeca` before the first `/jecunify`.

### 4. Clearer calculator step list

JEC labels each step with the item it was run for. For a recipe with several outputs (e.g. a Thermal Centrifuge giving 2 tiny U-235 + 5 U-238) that splits one machine's runs over several lines, and a line like "2 Tiny U-235" looks like extra crafting. Now every step that uses a multi-output recipe is listed under that recipe's **first output slot**, totalled over all runs ("12 Tiny U-235" = 6 centrifuge runs). Reorder a recipe's outputs to change the label.

### 5. Items invisible in your inventory after using JEC

After closing JEC's calculator, items that went into your **main inventory** without a screen open (for example taken from Storage Drawers with a full hotbar) were invisible until you clicked a slot or opened a chest. The hotbar was unaffected.

Cause: JEC closes its screens without telling the client the screen's container is gone, so the client keeps thinking JEC's container is open. Minecraft always applies hotbar updates from the server, but drops updates for other inventory slots that don't match the open container. AoE-QoL notices a leftover JEC container once you're back in the game with no screen open and switches back to your inventory, which is what closing the screen properly would have done. JEC's containers only exist on the client, and nothing else is ever touched.

---

## IC2 Canning Machine in JEI

IC2 2.6.252-ex110 (the last IC2 release for 1.10.2) builds its JEI pages from each machine's GUI layout file. The canning machines have older hand-coded GUIs with no layout file, so they never got pages. AoE-QoL adds two, drawn with the machines' own GUI textures and with every slot, tank and progress arrow where the real machine has it:

* **Fluid/Solid Canning Machine (Solid Canning)**: `[container] -> [filling] -> [output]`, drawn like the machine in its solid canning mode, e.g. empty fuel rod + uranium fuel -> uranium fuel rod, or tin cans + food -> filled tin can. These recipes belong to the Fluid/Solid Canning Machine; the Solid Canning Machine runs the same list, so both are listed as stations.
* **Fluid/Solid Canning Machine (Fluid Enriching)**: `[input tank] + [additive] -> [output tank]`, e.g. distilled water + lapis -> coolant, with IC2's own tank frames and gauge marks.

Page titles use IC2's translated machine names. Recipes are read from IC2's own recipe lists when JEI loads, so changes made by the pack's scripts are included. JEC records which JEI recipe types exist the first time the game starts; restart once more if JEC's transfer button doesn't appear on the new pages.

---

## Issues and requests

Found a recipe that won't chain in JEC, a missing or wrong JEI page, or have another quality-of-life change you'd like to see for Age of Engineering? Post it in the **Comments** on the mod's CurseForge page, or send itstwoam a message on CurseForge. Every report gets reviewed.

For a recipe problem, it helps to include the item and mod involved, what JEC or JEI shows, and the relevant lines from `/jectags` or `config/JustEnoughCalculation-TagReport.txt`.

Copy this into your comment and fill in what applies:

```
Problem:   (recipe won't chain / wrong or missing JEI page / items invisible / idea / other)
Item, mod: (e.g. Uranium Fuel Rod, IndustrialCraft 2)
What happens:
What you expected:
/jectags lines: (for recipe problems)
AoE-QoL version: (e.g. 1.0.0)
Pack: (Age of Engineering version, plus any mods you added or updated)
```

## Building

Needs **Java 8**. The first build downloads and decompiles Minecraft (several minutes).

```
./gradlew setupDecompWorkspace   # once
./gradlew build                  # -> build/libs/AoE-QoL-<version>.jar
```

JEI, IC2 and JEC are fetched from their Maven repositories as compile-only dependencies; none of them is included in the jar. This uses ForgeGradle 2.2 with Gradle 2.14.1, the toolchain for Minecraft 1.10.2; it prints an "unsupported version" warning, which is expected.

## Testing

The jar's real classes were tested outside the game against stand-in Minecraft classes, with full JVM bytecode verification:

* the transformer applied to the unmodified JEC 2.6.5 jar: all nine changes applied to the expected seven classes, nothing else touched;
* item matching, tag/damage rules, the mismatch report, `/jectags`, tooltips, ore unification (on preference, on save, `/jecunify`), and JEC's real calculator on an IC2 uranium chain;
* the canner recipe reader with IC2's real API classes, and the two JEI pages' texture crops and slot positions;
* the leftover-container fix (JEC screen open, JEI opened from JEC, JEC closed, other mods' containers);
* running without JEC and without IC2 (everything stays off), and a JEC version missing the expected methods (patches skipped, no crash);
* every Minecraft method and field referenced by the jar checked against the official MCP mappings for Forge 12.18.3.2511.

## License

[LGPL-3.0](LICENSE) (the same license as Just Enough Calculation), which builds on the GPL-3.0 text in [COPYING.GPL](COPYING.GPL).

## Credits

* Written by itstwoam with Claude (Anthropic).
* [Just Enough Calculation](https://github.com/Towdium/JustEnoughCalculation) by Towdium, [IndustrialCraft 2](https://www.curseforge.com/minecraft/mc-mods/industrial-craft) by the IC2 team, [JEI](https://github.com/mezz/JustEnoughItems) by mezz, and Age of Engineering by ErnstLustig. This project is not affiliated with any of them.
