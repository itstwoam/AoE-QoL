package com.itstwoam.aoeqol.jec;

import pers.towdium.just_enough_calculation.util.helpers.ItemStackHelper;
import pers.towdium.just_enough_calculation.util.helpers.PlayerRecordHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import net.minecraft.command.ICommandSender;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.oredict.OreDictionary;
import pers.towdium.just_enough_calculation.JECConfig;
import pers.towdium.just_enough_calculation.core.Recipe;

/**
 * Ore dictionary unification for JEC 2.6.5 (client side only).
 *
 * When an ore dictionary preference is chosen (JEI transfer picker or the calculator's oreDict
 * screen), every saved recipe item that has exactly the same ore dictionary names as the chosen
 * item is replaced by the chosen item, keeping its amount. Example: choosing IC2's tin plate turns
 * Immersive Engineering and Base Metals tin plates in all recipes into IC2 tin plates.
 *
 * Safety rules:
 *  - names are compared ignoring case (plateTin == platetin)
 *  - the two items must have the SAME set of names (red dye {dye, dyeRed} never replaces
 *    blue dye {dye, dyeBlue})
 *  - every name must match ListOreUnifyNames in JustEnoughCalculation.cfg (empty list = off)
 *  - every change is written to config/JustEnoughCalculation-OreUnify-log.txt
 *
 * Every recipe saved afterwards is unified automatically (log entry only, no chat).
 * "/jecunify" applies all existing preferences at once.
 */
public final class OreUnify {
    private static final String LOG = "[AoE-QoL] ";
    private static final String KEY = "ListOreUnifyNames";
    private static final String[] DEFAULTS = {"ingot*", "plate*", "dust*", "nugget*", "gear*", "rod*", "stick*", "wire*", "block*", "gem*"};
    private static final String COMMENT =
        "Ore dictionary names for which choosing a preferred item (JEI picker or the calculator's oreDict screen)\n"
      + "also rewrites every saved recipe, replacing other mods' versions with the chosen one (amounts are kept).\n"
      + "Items are only replaced when they have exactly the same ore names as the chosen item (case ignored).\n"
      + "Use * at the start or end as a wildcard, e.g. plate* or *Tin. Remove all lines to turn this off.\n"
      + "Wood is deliberately not included (logWood would turn spruce logs into oak); add logWood/plankWood if you want that.\n"
      + "Run /jecunify to apply all existing preferences at once. Changes are listed in JustEnoughCalculation-OreUnify-log.txt";
    private static final String LOG_NAME = "JustEnoughCalculation-OreUnify-log.txt";

    private static Property prop;
    private static String[] cachedSrc;
    private static List<String> patterns = new ArrayList<String>();

    private OreUnify() {}

    /** Called from NbtCompat.init(). */
    public static void init() {
        prop = JECConfig.config.get("general", KEY, DEFAULTS, COMMENT);
        refresh();
    }

    private static void refresh() {
        if (prop == null) {
            if (JECConfig.config == null) return;
            prop = JECConfig.config.get("general", KEY, DEFAULTS, COMMENT);
        }
        String[] s = prop.getStringList();
        if (s == cachedSrc) return;
        cachedSrc = s;
        List<String> p = new ArrayList<String>();
        if (s != null) for (String e : s) {
            if (e == null) continue;
            String t = e.trim().toLowerCase(Locale.ROOT);
            if (!t.isEmpty() && !t.startsWith("#")) p.add(t);
        }
        patterns = p;
    }

    private static boolean allowed(String lowerName) {
        for (String p : patterns) {
            boolean head = p.endsWith("*"), tail = p.startsWith("*");
            String core = p.substring(tail ? 1 : 0, p.length() - (head && p.length() > 1 ? 1 : 0));
            if (core.isEmpty()) return true;                       // "*"
            if (head && tail ? lowerName.contains(core)
                : head ? lowerName.startsWith(core)
                : tail ? lowerName.endsWith(core)
                : lowerName.equals(core)) return true;
        }
        return false;
    }

    /** Lower-cased, sorted ore names of a stack ("" if none). */
    private static String names(ItemStack stack) {
        try {
            if (stack == null || stack.getItem() == null) return "";
            int[] ids = OreDictionary.getOreIDs(stack);
            TreeSet<String> set = new TreeSet<String>();
            for (int id : ids) set.add(OreDictionary.getOreName(id).toLowerCase(Locale.ROOT));
            return String.join(",", set);
        } catch (Throwable t) {
            return "";
        }
    }

    /** The unification key, or null if the item's names aren't all covered by ListOreUnifyNames. */
    private static String key(ItemStack stack) {
        String n = names(stack);
        if (n.isEmpty()) return null;
        for (String s : n.split(",")) if (!allowed(s)) return null;
        return n;
    }

    private static String describe(ItemStack s) {
        String name;
        try { name = s.getDisplayName(); } catch (Throwable t) { name = "?"; }
        String reg;
        try { reg = String.valueOf(s.getItem().getRegistryName()); } catch (Throwable t) { reg = "?"; }
        return name + " (" + reg + ":" + s.getItemDamage() + ")";
    }

    /** Copy of the preferred item carrying the original stack's JEC amount/type data. */
    private static ItemStack replacement(ItemStack preferred, ItemStack original) {
        ItemStack r = preferred.copy();
        r.stackSize = 1;
        NBTTagCompound tag = r.getTagCompound();
        if (tag == null) { tag = new NBTTagCompound(); r.setTagCompound(tag); }
        tag.removeTag("je_calculation");
        NBTTagCompound orig = original.getTagCompound();
        if (orig != null && orig.hasKey("je_calculation")) {
            tag.setTag("je_calculation", orig.getCompoundTag("je_calculation").copy());
        }
        return r;
    }

    private static final class Result { int recipes, stacks; List<String> lines = new ArrayList<String>(); List<String> nearMisses = new ArrayList<String>(); }

    /** Rewrites all saved recipes so items with the same ore names as {@code preferred} become {@code preferred}. */
    private static Result unify(ItemStack preferred, String key, Result res) {
        String prefNames = names(preferred);
        TreeSet<String> prefSet = new TreeSet<String>();
        for (String s : prefNames.split(",")) prefSet.add(s);
        int groups = PlayerRecordHelper.getSizeGroup();
        List<String> groupNames = new ArrayList<String>();
        for (int g = 0; g < groups; g++) groupNames.add(PlayerRecordHelper.getGroupName(g));
        for (String group : groupNames) {
            List<Recipe> list = PlayerRecordHelper.getRecipeInGroup(group);
            if (list == null) continue;
            for (int i = 0; i < list.size(); i++) {
                Recipe recipe = list.get(i);
                List<List<ItemStack>> parts = new ArrayList<List<ItemStack>>();
                parts.add(recipe.getOutput()); parts.add(recipe.getCatalyst()); parts.add(recipe.getInput());
                int changed = 0;
                for (List<ItemStack> part : parts) {
                    for (int k = 0; k < part.size(); k++) {
                        ItemStack s = part.get(k);
                        if (s == null || ItemStackHelper.isItemEqual(s, preferred)) continue;
                        String n = names(s);
                        if (n.isEmpty()) continue;
                        if (n.equals(prefNames)) {
                            res.lines.add("  [" + group + " #" + (i + 1) + "] " + describe(s) + "  ->  " + describe(preferred));
                            part.set(k, replacement(preferred, s));
                            changed++;
                        } else {
                            for (String one : n.split(",")) {
                                if (prefSet.contains(one)) {
                                    String miss = describe(s) + " has [" + n + "], " + describe(preferred) + " has [" + prefNames + "]";
                                    if (!res.nearMisses.contains(miss)) res.nearMisses.add(miss);
                                    break;
                                }
                            }
                        }
                    }
                }
                if (changed > 0) {
                    Recipe updated = new Recipe(parts.get(0).toArray(new ItemStack[0]), parts.get(1).toArray(new ItemStack[0]),
                                                parts.get(2).toArray(new ItemStack[0]));
                    bulk = true;
                    try { PlayerRecordHelper.setRecipe(group, group, i, updated); } finally { bulk = false; }
                    res.recipes++;
                    res.stacks += changed;
                }
            }
        }
        return res;
    }

    /** True while unify() itself is storing a rewritten recipe, so onRecipeSaved doesn't run again. */
    private static boolean bulk = false;

    /**
     * Prepended to PlayerRecordHelper.addRecipe / setRecipe: every recipe the player saves is unified
     * against the existing ore dictionary preferences before JEC stores it. Silent: log file only.
     */
    public static Recipe onRecipeSaved(Recipe recipe, String group) {
        if (bulk || recipe == null) return recipe;
        try {
            refresh();
            if (patterns.isEmpty()) return recipe;
            Map<String, ItemStack> prefs = new LinkedHashMap<String, ItemStack>();
            java.util.Set<String> conflicts = new java.util.HashSet<String>();
            for (ItemStack p : PlayerRecordHelper.getOreDictPref()) {
                String k = key(p);
                if (k == null) continue;
                if (prefs.containsKey(k)) conflicts.add(k); else prefs.put(k, p);
            }
            if (prefs.isEmpty()) return recipe;
            List<List<ItemStack>> parts = new ArrayList<List<ItemStack>>();
            parts.add(recipe.getOutput()); parts.add(recipe.getCatalyst()); parts.add(recipe.getInput());
            Result res = new Result();
            for (List<ItemStack> part : parts) {
                for (int k = 0; k < part.size(); k++) {
                    ItemStack s = part.get(k);
                    if (s == null) continue;
                    String key = key(s);
                    if (key == null || conflicts.contains(key)) continue;
                    ItemStack p = prefs.get(key);
                    if (p == null || ItemStackHelper.isItemEqual(s, p)) continue;
                    res.lines.add("  [" + group + ", on save] " + describe(s) + "  ->  " + describe(p));
                    part.set(k, replacement(p, s));
                    res.stacks++;
                }
            }
            if (res.stacks == 0) return recipe;
            res.recipes = 1;
            Recipe updated = new Recipe(parts.get(0).toArray(new ItemStack[0]), parts.get(1).toArray(new ItemStack[0]),
                                        parts.get(2).toArray(new ItemStack[0]));
            writeLog("Auto-unified a saved recipe in " + group + ": changed " + res.stacks + " item(s)", res);
            return updated;
        } catch (Throwable t) {
            System.out.println(LOG + "Auto-unify on save failed (recipe saved unchanged): " + t);
            return recipe;
        }
    }

    /** Prepended to PlayerRecordHelper.addOreDictPref: runs whenever a preference is chosen. */
    public static void onPreferenceSet(ItemStack preferred) {
        try {
            refresh();
            if (preferred == null || patterns.isEmpty()) return;
            String key = key(preferred);
            if (key == null) return;
            // drop older preferences for the same ore names so they can't win over the new choice
            for (ItemStack old : PlayerRecordHelper.getOreDictPref()) {
                if (!ItemStackHelper.isItemEqual(old, preferred) && key.equals(key(old))) {
                    PlayerRecordHelper.removeOreDictPref(old);
                }
            }
            Result r = unify(preferred, key, new Result());
            report(r, describe(preferred), false);
        } catch (Throwable t) {
            System.out.println(LOG + "Ore unification failed: " + t);
        }
    }

    /** "/jecunify": apply every saved preference to all recipes. */
    public static void applyAll(ICommandSender sender) {
        try {
            refresh();
            if (patterns.isEmpty()) { say(sender, "Ore unification is turned off (ListOreUnifyNames is empty)."); return; }
            Map<String, List<ItemStack>> byKey = new LinkedHashMap<String, List<ItemStack>>();
            for (ItemStack p : PlayerRecordHelper.getOreDictPref()) {
                String k = key(p);
                if (k == null) continue;
                List<ItemStack> l = byKey.get(k);
                if (l == null) { l = new ArrayList<ItemStack>(); byKey.put(k, l); }
                l.add(p);
            }
            Result r = new Result();
            for (Map.Entry<String, List<ItemStack>> e : byKey.entrySet()) {
                if (e.getValue().size() > 1) {
                    StringBuilder b = new StringBuilder();
                    for (ItemStack s : e.getValue()) b.append(b.length() > 0 ? ", " : "").append(describe(s));
                    say(sender, "\u00a7cSkipped [" + e.getKey() + "]: you have more than one preference for it: " + b
                            + ". Remove the one you don't want in the calculator's oreDict screen.");
                    continue;
                }
                unify(e.getValue().get(0), e.getKey(), r);
            }
            report(r, byKey.size() + " ore dictionary preference(s)", true);
            if (sender != null) {
                say(sender, "\u00a7aChanged " + r.stacks + " item(s) in " + r.recipes + " recipe(s). Details: config/" + LOG_NAME);
            }
        } catch (Throwable t) {
            say(sender, "Ore unification failed: " + t);
        }
    }

    private static void report(Result r, String what, boolean fromCommand) {
        if (r.stacks == 0 && r.nearMisses.isEmpty()) return;
        String head = "Unified to " + what + ": changed " + r.stacks + " item(s) in " + r.recipes + " recipe(s)";
        System.out.println(LOG + head);
        if (!fromCommand && r.stacks > 0) chat("\u00a7a[JEC] " + head + ". Details: config/" + LOG_NAME);
        writeLog(head, r);
    }

    private static void writeLog(String head, Result r) {
        if (!head.startsWith("Unified")) System.out.println(LOG + head);
        Writer w = null;
        try {
            File cfg = JECConfig.config == null ? null : JECConfig.config.getConfigFile();
            File dir = cfg == null ? new File("config") : cfg.getParentFile();
            w = new OutputStreamWriter(new FileOutputStream(new File(dir, LOG_NAME), true), StandardCharsets.UTF_8);
            String nl = System.getProperty("line.separator");
            w.write("== " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "  " + head + nl);
            for (String l : r.lines) w.write(l + nl);
            if (!r.nearMisses.isEmpty()) {
                w.write("  Not changed - shares an ore name but the full name sets differ (check these by hand):" + nl);
                for (String l : r.nearMisses) w.write("    " + l + nl);
            }
            w.write(nl);
        } catch (Throwable t) {
            System.out.println(LOG + "Could not write " + LOG_NAME + ": " + t);
        } finally {
            try { if (w != null) w.close(); } catch (Throwable ignored) {}
        }
    }

    private static void chat(String s) {
        try { ClientChat.show(s); } catch (Throwable ignored) {}
    }

    private static void say(ICommandSender sender, String s) {
        if (sender == null) { System.out.println(LOG + s); return; }
        sender.addChatMessage(new TextComponentString(s));
    }
}
