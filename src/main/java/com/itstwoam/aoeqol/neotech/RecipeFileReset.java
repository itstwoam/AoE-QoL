package com.itstwoam.aoeqol.neotech;

import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;

/**
 * NeoTech writes its machine recipes to config/NeoTech/Registries/*Recipes.json once and keeps using them. A line
 * that names an item or fluid that isn't installed (mods removed or swapped since the file was made) crashes the
 * server: the Crusher crashes on any item it can't process, the Solidifier on any fluid. NeoTech's advice is to
 * delete the config so it writes fresh defaults.
 *
 * This does that automatically, per file, for the lines that crash: a Crusher input item, a Solidifier input fluid or
 * an Alloyer input fluid that isn't installed (the Crucible, Centrifuge and fuel values check for missing things, and
 * a missing output only matters when that exact input is processed; the pack's own files name a few of those and
 * must not be reset). Just before NeoTech loads such a file, it is renamed to <name>Recipes-stale-<date>.json, and NeoTech, finding no file, writes defaults for
 * the installed mods. Good files and NeoTech.cfg are left alone. A file NeoTech has just generated is fingerprinted
 * (<name>Recipes.json.aoeqol) and never reset, even if its defaults name something missing, so it can't loop.
 */
public final class RecipeFileReset {
    private RecipeFileReset() {}

    /** Files reset during this start, so afterLoad fingerprints only what NeoTech has just regenerated. */
    private static final java.util.Set<String> RESET = java.util.Collections.synchronizedSet(new java.util.HashSet<String>());

    /** Start of NeoTech's AbstractRecipeHandler.loadHandler(). */
    public static void beforeLoad(Object handler) {
        try {
            File file = recipeFile(handler);
            if (file == null || !file.isFile()) return;
            if (sha1(file).equals(fingerprint(file))) return;   // NeoTech's own defaults for this setup
            List<String> missing = missing(file, (String) call(handler, "getBaseName"));
            if (missing.isEmpty()) return;
            File backup = backupName(file);
            if (!file.renameTo(backup)) {
                System.out.println("[AoE-QoL] NeoTech: could not rename " + file + " (names " + summary(missing) + ")");
                return;
            }
            RESET.add(file.getAbsolutePath());
            System.out.println("[AoE-QoL] NeoTech: " + file.getName() + " names " + summary(missing)
                + ", not installed; moved to " + backup.getName() + " so NeoTech writes fresh defaults");
        } catch (Throwable t) {
            System.out.println("[AoE-QoL] NeoTech recipe file check skipped: " + t);
        }
    }

    /** End of loadHandler(): remember a freshly generated file so it is never reset. */
    public static void afterLoad(Object handler) {
        try {
            File file = recipeFile(handler);
            if (file == null || !file.isFile()) return;
            if (!RESET.remove(file.getAbsolutePath())) return;   // only files reset this start
            File mark = new File(file.getPath() + ".aoeqol");
            Files.write(mark.toPath(), sha1(file).getBytes(StandardCharsets.UTF_8));
        } catch (Throwable t) {
            // only the loop guard; nothing to do
        }
    }

    static File recipeFile(Object handler) throws Exception {
        String base = (String) call(handler, "getBaseFolderLocation");
        String name = (String) call(handler, "getBaseName");
        if (base == null || name == null) return null;
        return new File(base + File.separator + "Registries" + File.separator + name + "Recipes.json");
    }

    private static Object call(Object o, String m) throws Exception {
        Method mm = o.getClass().getMethod(m);
        return mm.invoke(o);
    }

    /** Input fields whose missing item or fluid crashes the machine, per recipe file (NeoTech's base names). */
    static String[] inputFields(String baseName) {
        if ("crusher".equals(baseName) || "solidifier".equals(baseName)) return new String[] {"input"};
        if ("alloyer".equals(baseName)) return new String[] {"fluidOne", "fluidTwo"};
        return new String[0];
    }

    /** Missing items ("mod:name:meta") and fluids ("name:amount") in the file's crash-relevant input fields. */
    static List<String> missing(File file, String baseName) throws Exception {
        List<String> fields = java.util.Arrays.asList(inputFields(baseName));
        List<String> out = new ArrayList<String>();
        if (fields.isEmpty()) return out;
        JsonElement root;
        try (Reader r = new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8)) {
            root = new JsonParser().parse(r);
        } catch (Exception e) {
            return out;   // unreadable: NeoTech already regenerates those itself
        }
        if (!root.isJsonArray()) return out;
        for (JsonElement e : (JsonArray) root) {
            if (!e.isJsonObject()) continue;
            for (Map.Entry<String, JsonElement> f : ((JsonObject) e).entrySet()) {
                if (!fields.contains(f.getKey())) continue;
                JsonElement v = f.getValue();
                if (!v.isJsonPrimitive() || !v.getAsJsonPrimitive().isString()) continue;
                String s = v.getAsString();
                String bad = check(f.getKey(), s);
                if (bad != null && !out.contains(bad)) out.add(bad);
            }
        }
        return out;
    }

    /** The two lookups, the same ones NeoTech uses (replaceable for tests). */
    interface Lookup { boolean item(String mod, String name); boolean fluid(String name); }

    static Lookup lookup = new Lookup() {
        @SuppressWarnings("deprecation")
        @Override public boolean item(String mod, String name) { return GameRegistry.findItem(mod, name) != null; }
        @Override public boolean fluid(String name) { return FluidRegistry.getFluid(name) != null; }
    };

    /** Null if fine (or not an item/fluid reference), else what's missing. */
    static String check(String key, String s) {
        if (s.isEmpty()) return null;
        String[] p = s.split(":");
        if (p.length == 3 && isInt(p[2])) {
            return !lookup.item(p[0], p[1]) ? "item " + p[0] + ":" + p[1] : null;
        }
        if (p.length == 2 && isInt(p[1])) {
            return !lookup.fluid(p[0]) ? "fluid " + p[0] : null;
        }
        return null;   // ore dictionary names and anything else
    }

    private static boolean isInt(String s) {
        if (s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) if (!Character.isDigit(s.charAt(i))) return false;
        return true;
    }

    static String summary(List<String> missing) {
        if (missing.size() <= 3) return String.join(", ", missing);
        return String.join(", ", missing.subList(0, 3)) + " and " + (missing.size() - 3) + " more";
    }

    static File backupName(File file) {
        String stem = file.getName().replaceFirst("\\.json$", "");
        String day = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        File b = new File(file.getParentFile(), stem + "-stale-" + day + ".json");
        for (int n = 2; b.exists(); n++) b = new File(file.getParentFile(), stem + "-stale-" + day + "-" + n + ".json");
        return b;
    }

    private static String fingerprint(File file) {
        try {
            File mark = new File(file.getPath() + ".aoeqol");
            return mark.isFile() ? new String(Files.readAllBytes(mark.toPath()), StandardCharsets.UTF_8).trim() : "";
        } catch (Exception e) {
            return "";
        }
    }

    static String sha1(File f) throws Exception {
        byte[] d = MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(f.toPath()));
        StringBuilder sb = new StringBuilder();
        for (byte b : d) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
