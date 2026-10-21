package com.itstwoam.aoeqol.jec;

import pers.towdium.just_enough_calculation.util.helpers.ItemStackHelper;
import pers.towdium.just_enough_calculation.util.helpers.PlayerRecordHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.command.ICommandSender;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import pers.towdium.just_enough_calculation.JECConfig;

/**
 * Compatibility patch for Just Enough Calculation 2.6.5 (MC 1.10.2).
 *
 * 1. Ignore rules: lets the player list, per mod or per item, NBT tags and damage values that only
 *    describe an item's condition (heat, charge, wear), so JEC doesn't treat e.g. a fresh and a
 *    crafted IC2 Heat Vent as different items. Configured in JustEnoughCalculation.cfg:
 *      ListIgnoredNBTKeys  entries "<item>@<tag>"   e.g. ic2:*@advDmg
 *      ListIgnoredDamage   entries "<item>"         e.g. ic2:*
 *    <item> is "modid:itemname", "modid:*" or "*". JEC's own "je_calculation" tag is always ignored.
 *
 * 2. Mismatch detection: whenever JEC compares two copies of the SAME item and they only fail to
 *    match because of a tag or the damage value, that is recorded. The findings are written to
 *    config/JustEnoughCalculation-TagReport.txt, logged, and shown by the client command "/jectags".
 *
 * 3. Tooltip: with advanced tooltips on (F3+H), items show their top-level NBT tags.
 */
public final class NbtCompat {
    private static final String LOG = "[AoE-QoL] ";
    private static final String CATEGORY = "general";
    private static final String KEY_NBT = "ListIgnoredNBTKeys";
    private static final String KEY_DAMAGE = "ListIgnoredDamage";
    private static final String[] DEFAULT_NBT = {"ic2:*@advDmg", "ic2:*@charge"};
    private static final String[] DEFAULT_DAMAGE = {"ic2:*"};
    private static final String COMMENT_NBT =
        "NBT tags to ignore when JEC compares items, so items that only differ in condition count as the same item.\n"
      + "Format: <item>@<tag>, where <item> is modid:itemname, modid:* or * (any item). One per line.\n"
      + "Only add tags that store condition (heat, charge, energy, wear). Tags that say WHAT an item is\n"
      + "(cable type, material, fluid, mob, label name) must not be listed or different items will merge.\n"
      + "Not sure which tag? Run the calculator, then use /jectags or open JustEnoughCalculation-TagReport.txt.\n"
      + "Defaults: ic2:*@advDmg (IC2 reactor component heat / custom damage), ic2:*@charge (IC2 stored EU)";
    private static final String COMMENT_DAMAGE =
        "Items whose damage value JEC should ignore. Format: modid:itemname, modid:* or *. One per line.\n"
      + "Only applies to items that can take damage (tools, batteries); items that use damage values for\n"
      + "variants (plates, dusts, cables, wool ...) are never affected.\n"
      + "Default: ic2:* (IC2 batteries and crystals store their charge level in the damage value)";
    private static final String REPORT_NAME = "JustEnoughCalculation-TagReport.txt";
    private static final int MAX_REPORT_ITEMS = 300;
    private static final int MAX_VALUE_LEN = 48;

    // ------------------------------------------------------------------ ignore rules

    private static final class Rule {
        final String mod;   // null = any mod
        final String item;  // null = any item of that mod
        final String tag;   // null for damage rules
        Rule(String mod, String item, String tag) { this.mod = mod; this.item = item; this.tag = tag; }
        boolean matches(String regName) {
            if (mod == null) return true;
            if (regName == null) return false;
            int i = regName.indexOf(':');
            if (i < 0) return false;
            if (!mod.equals(regName.substring(0, i))) return false;
            return item == null || item.equals(regName.substring(i + 1));
        }
    }

    private static Property propNbt, propDamage;
    private static String[] cachedNbtSrc, cachedDamageSrc;
    private static List<Rule> nbtRules = new ArrayList<Rule>(), damageRules = new ArrayList<Rule>();

    private NbtCompat() {}

    /** Called from JECConfig.handleInit() so the options are written to the config file at startup. */
    public static void init() {
        propNbt = JECConfig.config.get(CATEGORY, KEY_NBT, DEFAULT_NBT, COMMENT_NBT);
        propDamage = JECConfig.config.get(CATEGORY, KEY_DAMAGE, DEFAULT_DAMAGE, COMMENT_DAMAGE);
        refresh();
        try { OreUnify.init(); } catch (Throwable t) { System.out.println(LOG + "Ore unify config failed: " + t); }
    }

    /** Re-parses the lists if they were changed (e.g. through the in-game config screen). */
    private static synchronized void refresh() {
        if (propNbt == null || propDamage == null) {
            if (JECConfig.config == null) return;
            init();
            return;
        }
        String[] n = propNbt.getStringList();
        if (n != cachedNbtSrc) {
            cachedNbtSrc = n;
            nbtRules = parse(n, true);
            forgetResolved();
        }
        String[] d = propDamage.getStringList();
        if (d != cachedDamageSrc) {
            cachedDamageSrc = d;
            damageRules = parse(d, false);
            forgetResolved();
        }
    }

    private static List<Rule> parse(String[] entries, boolean withTag) {
        List<Rule> out = new ArrayList<Rule>();
        if (entries == null) return out;
        for (String raw : entries) {
            if (raw == null) continue;
            String s = raw.trim();
            if (s.isEmpty() || s.startsWith("#")) continue;
            String tag = null;
            if (withTag) {
                int at = s.lastIndexOf('@');
                if (at <= 0 || at == s.length() - 1) { warn(raw); continue; }
                tag = s.substring(at + 1).trim();
                s = s.substring(0, at).trim();
                if (tag.isEmpty() || tag.equals("*")) { warn(raw); continue; }
            }
            if (s.equals("*")) { out.add(new Rule(null, null, tag)); continue; }
            int c = s.indexOf(':');
            if (c <= 0 || c == s.length() - 1) { warn(raw); continue; }
            String mod = s.substring(0, c);
            String item = s.substring(c + 1);
            out.add(new Rule(mod, item.equals("*") ? null : item, tag));
        }
        return out;
    }

    private static void warn(String entry) {
        System.out.println(LOG + "Ignoring malformed config entry: \"" + entry + "\"");
    }

    private static String regName(ItemStack stack) {
        Item item = stack.getItem();
        if (item == null) return null;
        ResourceLocation rl = item.getRegistryName();
        return rl == null ? null : rl.toString();
    }

    private static boolean isTagIgnored(String regName, String key) {
        if ("je_calculation".equals(key)) return true;
        for (Rule r : nbtRules) if (key.equals(r.tag) && r.matches(regName)) return true;
        return false;
    }

    private static boolean isDamageIgnored(String regName) {
        for (Rule r : damageRules) if (r.matches(regName)) return true;
        return false;
    }

    private static NBTTagCompound strip(NBTTagCompound tag, String regName, boolean itemKnown) {
        NBTTagCompound copy = tag == null ? new NBTTagCompound() : tag.copy();
        copy.removeTag("je_calculation");
        for (Rule r : nbtRules) {
            if (itemKnown ? r.matches(regName) : r.mod == null) copy.removeTag(r.tag);
        }
        return copy;
    }

    /**
     * The stack's NBT with the tags on ListIgnoredNBTKeys (and JEC's own label) removed; never null.
     * Used by the RFTools crafting fixes to compare item variants.
     */
    public static NBTTagCompound strippedTag(ItemStack stack) {
        refresh();
        String name = regName(stack);
        if (propNbt == null) {
            NBTTagCompound copy = stack.getTagCompound() == null ? new NBTTagCompound() : stack.getTagCompound().copy();
            for (Rule r : parse(DEFAULT_NBT, true)) if (r.matches(name)) copy.removeTag(r.tag);
            return copy;
        }
        return strip(stack.getTagCompound(), name, true);
    }

    /** Replacement for ItemStackHelper.isItemEqual. */
    public static boolean isItemEqual(ItemStack one, ItemStack two) {
        if (one == null || two == null) return false;
        Item item = one.getItem();
        if (item != two.getItem()) return false;
        refresh();
        String name = regName(one);
        boolean damageMismatch = false;
        if (one.getMetadata() != two.getMetadata()) {
            // Different damage on an item that doesn't wear down = a different variant; never report.
            if (item == null || !item.isDamageable()) return false;
            damageMismatch = !isDamageIgnored(name);
        }
        NBTTagCompound a = strip(one.getTagCompound(), name, true);
        NBTTagCompound b = strip(two.getTagCompound(), name, true);
        boolean tagMismatch = !a.equals(b);
        if (damageMismatch || tagMismatch) {
            record(name, damageMismatch ? one.getMetadata() + " vs " + two.getMetadata() : null, tagMismatch ? a : null, b);
            return false;
        }
        return ItemStackHelper.NBT.getFluid(one) == ItemStackHelper.NBT.getFluid(two);
    }

    /** Replacement for ItemStackHelper.NBT.equalsIgnoreJEC (no item known: only "*" rules apply). */
    public static boolean equalsIgnoreJEC(NBTTagCompound a, NBTTagCompound b) {
        refresh();
        return strip(a, null, false).equals(strip(b, null, false));
    }

    // ------------------------------------------------------------------ mismatch detection

    private static final class Finding {
        String damage;                                                    // sample "26 vs 0", or null
        final Map<String, String> tags = new LinkedHashMap<String, String>(); // tag -> sample "x vs y"
    }

    private static final Map<String, Finding> findings = new LinkedHashMap<String, Finding>();
    private static boolean reportDirty = false;

    private static String show(NBTBase v) {
        if (v == null) return "(missing)";
        String s = String.valueOf(v);
        return s.length() > MAX_VALUE_LEN ? s.substring(0, MAX_VALUE_LEN - 3) + "..." : s;
    }

    private static synchronized void record(String name, String damageSample, NBTTagCompound a, NBTTagCompound b) {
        if (name == null) return;
        try {
            Finding f = findings.get(name);
            boolean isNew = false;
            if (f == null) {
                if (findings.size() >= MAX_REPORT_ITEMS) return;
                f = new Finding();
                findings.put(name, f);
            }
            if (damageSample != null && f.damage == null) {
                f.damage = damageSample;
                isNew = true;
                System.out.println(LOG + name + " differs in damage value (" + damageSample + "). If that is wear or charge, add \""
                        + name + "\" to ListIgnoredDamage.");
            }
            if (a != null) {
                Set<String> keys = new TreeSet<String>(a.getKeySet());
                keys.addAll(b.getKeySet());
                for (String k : keys) {
                    if (f.tags.containsKey(k)) continue;
                    NBTBase va = a.getTag(k), vb = b.getTag(k);
                    if (va == null ? vb == null : va.equals(vb)) continue;
                    String sample = show(va) + " vs " + show(vb);
                    f.tags.put(k, sample);
                    isNew = true;
                    System.out.println(LOG + name + " differs in tag \"" + k + "\" (" + sample + "). If that is condition, add \""
                            + name + "@" + k + "\" to ListIgnoredNBTKeys.");
                }
            }
            if (isNew) { reportDirty = true; writeReport(); }
        } catch (Throwable t) {
            // detection is best-effort; never break the calculator because of it
        }
    }

    /** Drop findings that the current rules now cover (called when the lists change). */
    private static void forgetResolved() {
        synchronized (NbtCompat.class) {
            boolean changed = false;
            for (java.util.Iterator<Map.Entry<String, Finding>> it = findings.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<String, Finding> e = it.next();
                Finding f = e.getValue();
                if (f.damage != null && isDamageIgnored(e.getKey())) { f.damage = null; changed = true; }
                for (java.util.Iterator<String> k = f.tags.keySet().iterator(); k.hasNext(); ) {
                    if (isTagIgnored(e.getKey(), k.next())) { k.remove(); changed = true; }
                }
                if (f.damage == null && f.tags.isEmpty()) it.remove();
            }
            if (changed) { reportDirty = true; writeReport(); }
        }
    }

    private static List<String> reportLines() {
        List<String> out = new ArrayList<String>();
        for (Map.Entry<String, Finding> e : findings.entrySet()) {
            String name = e.getKey();
            Finding f = e.getValue();
            String mod = name.indexOf(':') > 0 ? name.substring(0, name.indexOf(':')) : name;
            out.add(name);
            if (f.damage != null) {
                out.add("    damage value differs: " + f.damage);
                out.add("        ListIgnoredDamage line:  " + name + "     (whole mod: " + mod + ":*)");
            }
            for (Map.Entry<String, String> t : f.tags.entrySet()) {
                out.add("    tag \"" + t.getKey() + "\" differs: " + t.getValue());
                out.add("        ListIgnoredNBTKeys line: " + name + "@" + t.getKey() + "     (whole mod: " + mod + ":*@" + t.getKey() + ")");
            }
        }
        return out;
    }

    private static File reportFile() {
        File cfg = JECConfig.config == null ? null : JECConfig.config.getConfigFile();
        File dir = cfg == null ? new File("config") : cfg.getParentFile();
        return new File(dir, REPORT_NAME);
    }

    private static synchronized void writeReport() {
        if (!reportDirty) return;
        reportDirty = false;
        Writer w = null;
        try {
            w = new OutputStreamWriter(new FileOutputStream(reportFile()), StandardCharsets.UTF_8);
            String nl = System.getProperty("line.separator");
            w.write("Just Enough Calculation - tag mismatch report (written by the JEC NBT patch)" + nl);
            w.write("=============================================================================" + nl + nl);
            w.write("Each entry below is an item where JEC compared two copies and they did NOT count as the same" + nl);
            w.write("item only because of the tag or damage value shown. This list is rebuilt every time the game starts." + nl + nl);
            w.write("How to decide:" + nl);
            w.write("  * CONDITION (safe to ignore): heat, charge, energy, wear, fill level. Usually a number that" + nl);
            w.write("    changes as you use the item, or a tag one copy has and the other doesn't." + nl);
            w.write("  * IDENTITY (do NOT ignore): what kind of item it is - cable type, material, wood, fluid, mob," + nl);
            w.write("    upgrade/tier, label name. Both copies have the tag but with different words or ids." + nl);
            w.write("  Some entries here are correct behaviour (e.g. two different cable types); just leave those alone." + nl + nl);
            w.write("To apply: copy the suggested line into config/JustEnoughCalculation.cfg under the matching list" + nl);
            w.write("(or Mods > Just Enough Calculation > Config in game), then re-run the calculation." + nl + nl);
            List<String> lines = reportLines();
            if (lines.isEmpty()) w.write("(no mismatches found yet - open the calculator and calculate something)" + nl);
            for (String l : lines) w.write(l + nl);
        } catch (Throwable t) {
            System.out.println(LOG + "Could not write " + REPORT_NAME + ": " + t);
        } finally {
            try { if (w != null) w.close(); } catch (Throwable ignored) {}
        }
    }

    /** Runs the client command "/jectags [clear]". */
    public static void handleCommand(ICommandSender sender, String[] args) {
        try {
            refresh();
            if (args != null && args.length > 0 && "clear".equals(args[0])) {
                synchronized (NbtCompat.class) { findings.clear(); reportDirty = true; writeReport(); }
                say(sender, "JEC tag report cleared. Run the calculation again to collect fresh results.");
                return;
            }
            List<String> lines;
            synchronized (NbtCompat.class) { lines = reportLines(); }
            if (lines.isEmpty()) {
                say(sender, "No tag mismatches recorded yet. Open the calculator, run the calculation that isn't chaining, then try again.");
                return;
            }
            say(sender, "\u00a7eItems that failed to match only because of a tag or damage value:");
            for (String l : lines) {
                if (l.startsWith("        ")) continue;                  // suggestions are in the file
                say(sender, l.startsWith("    ") ? "\u00a77" + l.trim() : "\u00a7f" + l);
            }
            say(sender, "\u00a77Only ignore tags that store condition (heat, charge, energy, wear).");
            say(sender, "\u00a77Ready-to-paste config lines: config/" + REPORT_NAME + "   |   /jectags clear  to reset");
        } catch (Throwable t) {
            say(sender, "JEC tag report failed: " + t);
        }
    }

    private static void say(ICommandSender sender, String s) {
        sender.addChatMessage(new TextComponentString(s));
    }

    // ------------------------------------------------------------------ tooltip

    private static boolean tooltipBroken = false;

    /** Prepended to JEC's tooltip handler. With F3+H on, lists an item's top-level NBT tags. */
    public static void onTooltip(ItemTooltipEvent event) {
        if (tooltipBroken || !com.itstwoam.aoeqol.AoEQoLConfig.ON_JEC_CONDITION_TAGS) return;
        try {
            if (!event.isShowAdvancedItemTooltips()) return;
            ItemStack stack = event.getItemStack();
            if (stack == null || stack.getTagCompound() == null) return;
            NBTTagCompound tag = stack.getTagCompound();
            String name = regName(stack);
            refresh();
            StringBuilder line = new StringBuilder();
            List<String> out = new ArrayList<String>();
            for (String k : new TreeSet<String>(tag.getKeySet())) {
                if ("je_calculation".equals(k)) continue;
                String part = k + "=" + show(tag.getTag(k)) + (isTagIgnored(name, k) ? " (ignored)" : "");
                if (line.length() > 0 && line.length() + part.length() > 50) {
                    out.add(line.toString());
                    line.setLength(0);
                }
                line.append(line.length() > 0 ? ", " : "").append(part);
            }
            if (line.length() > 0) out.add(line.toString());
            if (out.isEmpty()) return;
            List<String> tip = event.getToolTip();
            int shown = 0;
            for (String l : out) {
                if (shown == 3) { tip.add("\u00a78   ..."); break; }
                tip.add((shown == 0 ? "\u00a78Tags: " : "\u00a78   ") + l);
                shown++;
            }
        } catch (Throwable t) {
            tooltipBroken = true;
            System.out.println(LOG + "Tooltip tag display disabled: " + t);
        }
    }
}
