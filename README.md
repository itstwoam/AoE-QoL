# AoE-QoL

Quality-of-life fixes for the **Age of Engineering** modpack (Minecraft 1.10.2, Forge 12.18.3.2511).

Work in progress. Planned contents of the single add-on jar:

- **Just Enough Calculation (2.6.5) fixes**, applied in memory at load time (the official JEC jar stays untouched; everything stays off if JEC isn't installed):
  - per-mod/per-item lists of NBT tags and damage values to ignore when matching items (fixes IC2 reactor components, batteries, etc.)
  - `/jectags`: find which tag is stopping a recipe from chaining
  - ore dictionary unification of saved recipes (`/jecunify`, automatic on save)
  - clearer calculator step list for recipes with several outputs
- **JEI page for the IC2 Canning Machine** (IC2 2.6.252-ex110 doesn't register one), so `R` on a fuel rod shows how it's made.

License: LGPL-3.0 (see `LICENSE`, which builds on the GPL-3.0 text in `COPYING.GPL`).
