package com.itstwoam.aoeqol.advsolar;

import java.util.HashMap;
import java.util.Map;
import net.minecraftforge.oredict.OreDictionary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Advanced Solar Panels reads its Molecular Transformer recipes from config/advanced_solar_panels_MTRecipes.cfg.
 * The default file asks for ore dictionary names nothing in the pack registers, e.g.
 * "OreDict:gemNetherQuartz; OreDict:gemCertusQuartz": vanilla quartz is "gemQuartz" and AE2's certus quartz
 * crystal is "crystalCertusQuartz". ASP then skips the line ("output cannot be resolved") and nether quartz
 * can't be turned into certus quartz.
 *
 * Fix: before ASP decodes a line, each "OreDict:name" with no items registered is swapped for a known alias
 * that has items. Names that resolve are left alone, so a hand-edited config keeps working.
 *
 * Setting: transformerQuartzToCertus (needs a restart, recipes load once).
 *
 * Called at the start of ASP's Recipes.decodeLine(int, String[]) and replaces its parts argument.
 */
public final class MtRecipeFix {
    private static final Logger LOG = LogManager.getLogger("AoE-QoL");
    private static final String PREFIX = "OreDict:";
    private static final Map<String, String> ALIASES = new HashMap<String, String>();
    static {
        ALIASES.put("gemNetherQuartz", "gemQuartz");
        ALIASES.put("gemCertusQuartz", "crystalCertusQuartz");
        ALIASES.put("dustNetherQuartz", "dustQuartz");
    }

    private MtRecipeFix() {}

    public static String[] fixLine(int line, String[] parts) {
        try {
            if (parts == null || !com.itstwoam.aoeqol.AoEQoLConfig.ON_MT_QUARTZ_TO_CERTUS) return parts;
            for (int i = 0; i < parts.length && i < 2; i++) {
                String fixed = fix(parts[i]);
                if (fixed != null) {
                    LOG.info("Molecular Transformer recipe line {}: {} -> {}", line, parts[i].trim(), fixed);
                    parts[i] = fixed;
                }
            }
        } catch (Throwable t) {
            LOG.warn("Molecular Transformer recipe line {}: alias check failed, line left as is", line, t);
        }
        return parts;
    }

    /** The entry with its ore name swapped for an alias, or null when nothing should change. */
    static String fix(String entry) {
        if (entry == null) return null;
        String s = entry.trim();
        if (!s.startsWith(PREFIX)) return null;
        String rest = s.substring(PREFIX.length()).trim();
        int star = rest.indexOf('*');
        String name = (star < 0 ? rest : rest.substring(0, star)).trim();
        String amount = star < 0 ? "" : rest.substring(star);
        String alias = ALIASES.get(name);
        if (alias == null || hasOres(name) || !hasOres(alias)) return null;
        return PREFIX + alias + amount;
    }

    private static boolean hasOres(String name) {
        return OreDictionary.doesOreNameExist(name) && !OreDictionary.getOres(name, false).isEmpty();
    }
}
