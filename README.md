# AoE-QoL

Quality-of-life fixes for the **Age of Engineering** modpack (Minecraft **1.10.2**, Forge **12.18.3.2511**), as one small add-on jar.

Drop `AoE-QoL-<version>.jar` into your `mods` folder. Nothing else needs replacing: every part switches itself on only when the mod it works with is installed, and does nothing otherwise.

| Part | Needs | What it does |
|---|---|---|
| [Just Enough Calculation fixes](#just-enough-calculation-fixes) | JEC 1.10.2-2.6.5 | Makes the calculator chain recipes for items that carry state (IC2 reactor parts, batteries...), helps you find the tag that blocks a chain, unifies ore dictionary variants, lists every output of multi-output recipes in the step list, and fixes items going invisible in your inventory after using JEC |
| [IC2 Canning Machine in JEI](#ic2-canning-machine-in-jei) | IC2 2.6.252-ex110 + JEI | Adds the canning machines' recipes to JEI, drawn like the machines' own screens, so pressing `R` on a uranium fuel rod, food can or coolant shows how it's made |
| [IC2 Reinforced Stone in JEI](#ic2-reinforced-stone-in-jei) | IC2 2.6.252-ex110 + JEI | Shows how Reinforced Stone is made (scaffold + construction foam), which has no crafting recipe |
| [RFTools crafting fixes](#rftools-crafting-fixes) | RFTools 1.1x-5.92 | The crafting grid (Storage Scanner, Modular Storage) and the Crafter now find charged batteries/crystals and worn tools, and pick the right variant (cable insulation, Mekanism tier) |

> **Status:** written for and tested against the exact mod versions in Age of Engineering 1.1.2. Tests run outside the game (see [Testing](#testing)); please [report](#issues-and-requests) anything that misbehaves in game.

---

## Just Enough Calculation fixes

JEC is patched **in memory while it loads** (a small coremod, which also carries the RFTools fix). The official JEC jar on disk is never modified, and if JEC isn't installed the patch simply never runs. If a different JEC version doesn't have a method the patch expects, that one change is skipped and logged instead of crashing.

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

### 4. Every output in the calculator step list

JEC labels each step with the one item it was run for. For a recipe with several outputs, such as a Thermal Centrifuge giving tiny U-235 and U-238 from the same runs, only that item appeared, so U-238 for a uranium fuel rod never showed up in the steps even though the next recipe needs it. Now every step that uses a multi-output recipe lists **all** of the recipe's outputs, each totalled over all runs (e.g. "12 Tiny U-235" and "30 U-238" for 6 centrifuge runs). Anything the next steps don't use still shows up under leftovers.

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

## IC2 Reinforced Stone in JEI

Reinforced Stone has no crafting recipe: you spray an **Iron Scaffold** with construction foam using a CF Sprayer, the foam hardens in place, and you mine the result. AoE-QoL adds a JEI page for it, so pressing `R` on Reinforced Stone (or `U` on an Iron Scaffold or construction foam) shows:

* **Iron Scaffold + 100 mB construction foam -> Reinforced Stone**
* **Reinforced Iron Scaffold + 100 mB construction foam -> Reinforced Stone + Iron Fence** (the fence pops off when sprayed)

Hovering over the arrow explains the hardening: about 10 minutes in full light and much slower in the dark, or right-click the foam with sand to harden it instantly. Mined before it hardens, it drops the scaffold back. The amounts are IC2's own (100 mB of foam per block). The CF Sprayer and CF Pack are listed as the tools. Wooden scaffolds turn into normal construction foam walls instead, so they aren't listed.

---

## RFTools crafting fixes

The crafting grid in RFTools' Storage Scanner and Modular Storage, and the RFTools Crafter block, look for each ingredient in two passes:

1. **strict:** same item and same damage value (NBT is never looked at);
2. **loose**, only if the strict pass left something missing: same item with any damage value (the grid also accepts a shared ore dictionary name).

The mod's own recipe check then decides whether the collected items work. Two kinds of items trip this:

* **Wear or charge in the damage value** (IC2 batteries and energy crystals, worn tools such as the IC2 Forge Hammer or shears): a different charge or wear fails the strict pass. The loose pass then picks *any* variant of the other ingredients (the wrong IC2 cable or machine casing), the recipe check fails, and the grid shows the crystals as missing, for example when crafting an MFE. **Now:** the strict pass ignores the damage value of items that can take damage. Those items have no variants, so their damage is always wear or charge.
* **Variant in NBT** (IC2 cable insulation, Mekanism energy cube / tank / bin tiers, IC2 fluid cells): the strict pass can't tell them apart and takes whichever comes first, e.g. an uninsulated gold cable for a recipe that needs 2× insulated. The craft then fails without any slot marked missing. **Now:** each pass first looks for an item whose NBT matches the recipe's exactly, ignoring the tags on `ListIgnoredNBTKeys` (IC2's charge and heat by default), and only then falls back to RFTools' own choice. If the recipe's item has no NBT, nothing changes.

* **Ore dictionary equivalents behind same-item variants:** when JEI's `+` fills the grid from an ore dictionary recipe, it puts one example in each slot, e.g. an IC2 iron plate for the Basic Machine Casing. If you only have Immersive Engineering iron plates, RFTools' loose pass took the first item that was *either* the same item in any variant *or* shared an ore name, so an IC2 copper plate listed first won and the craft failed. **Now:** the loose pass takes items sharing an ore dictionary name (the IE iron plate) before other variants of the same item.

The Crafter's ghost-slot filter (input slots set to one item) also accepts a charged or worn copy of that item now.

Nothing can be crafted that wasn't craftable before: the recipe check still runs on whatever is picked, so a wrong pick only fails a craft. Charge on IC2 crystals is not carried into an MFE; that is how IC2's recipe works, so drain them first. Without JEC installed, the default ignore list applies. Retrieving items through RFTools screen modules (Storage Control) still matches without NBT; the Storage Scanner's own list and retrieval already match exactly.

---

## Issues and requests

Found a recipe that won't chain in JEC, a missing or wrong JEI page, or have another quality-of-life change you'd like to see for Age of Engineering? Post it in the **Comments** on the mod's CurseForge page, or send Itstwoam a message on CurseForge. Every report gets reviewed.

For a recipe problem, it helps to include the item and mod involved, what JEC or JEI shows, and the relevant lines from `/jectags` or `config/JustEnoughCalculation-TagReport.txt`.

Copy this into your comment and fill in what applies:

```
Problem:   (recipe won't chain / wrong or missing JEI page / items invisible / idea / other)
Item, mod: (e.g. Uranium Fuel Rod, IndustrialCraft 2)
What happens:
What you expected:
/jectags lines: (for recipe problems)
AoE-QoL version: (e.g. 1.1.0)
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

* the transformer applied to the unmodified JEC 2.6.5 jar: all nine changes applied to the expected seven classes, nothing else touched; and to the unmodified RFTools 5.92 jar: only the crafting grid's and the Crafter's item search changed (five changes in two classes), and both patched classes pass type-checking bytecode verification against RFTools, McJtyLib and Forge;
* the RFTools search: 2× insulated gold cable picked over an uninsulated one listed first, Mekanism Basic cube over an Ultimate one, charged crystals and worn shears accepted, IE's NBT-worn hammer still accepted, copper cable never taken for gold in the strict pass, an IE iron plate taken before an IC2 copper plate in the loose pass, stack counts spread over several slots, the Crafter's keep-one setting, its ghost-slot filter, and no crash on unexpected input, with and without JEC;
* item matching, tag/damage rules, the mismatch report, `/jectags`, tooltips, ore unification (on preference, on save, `/jecunify`), and JEC's real calculator on an IC2 uranium chain;
* the canner recipe reader with IC2's real API classes, and the two JEI pages' texture crops and slot positions;
* the leftover-container fix (JEC screen open, JEI opened from JEC, JEC closed, other mods' containers);
* running without JEC and without IC2 (everything stays off), and a JEC version missing the expected methods (patches skipped, no crash);
* every Minecraft method and field referenced by the jar checked against the official MCP mappings for Forge 12.18.3.2511.

## License

[LGPL-3.0](LICENSE) (the same license as Just Enough Calculation), which builds on the GPL-3.0 text in [COPYING.GPL](COPYING.GPL).

## Credits

* Written by Itstwoam with Claude (Anthropic).
* [Just Enough Calculation](https://github.com/Towdium/JustEnoughCalculation) by Towdium, [IndustrialCraft 2](https://www.curseforge.com/minecraft/mc-mods/industrial-craft) by the IC2 team, [JEI](https://github.com/mezz/JustEnoughItems) by mezz, RFTools by McJty, and Age of Engineering by davqvist. This project is not affiliated with any of them.
