package com.itstwoam.aoeqol.asm;

import java.util.Locale;
import org.apache.logging.log4j.LogManager;

/**
 * Java's no-argument String.toLowerCase() / toUpperCase() follow the computer's language. In Turkish and
 * Azerbaijani a capital I lowercases to a dotless "ı" (and i uppercases to "İ"). Many mods in the pack build
 * internal names that way (Extreme Reactors, Mekanism, Compact Solar Arrays, AE2, Draconic Evolution and others,
 * and Minecraft itself for mod IDs), so "IDLE" becomes "ıdle", which Minecraft rejects, and the game crashes
 * while starting.
 *
 * Fix: before any mod loads, a Turkish/Azerbaijani default locale is replaced by the neutral one, which only
 * changes how text is lowercased and uppercased. Number and date formats (and the display language) stay as
 * they were. Minecraft's own in-game language is chosen separately and isn't affected. On any other language
 * nothing changes. This is not a setting: turning it off would only bring the crash back.
 */
public final class TurkishLocaleFix {
    /** The locale that was replaced, or null if nothing was changed. Read by the one-time chat notice. */
    public static volatile Locale replaced;

    private TurkishLocaleFix() {}

    static void apply() {
        try {
            Locale original = Locale.getDefault();
            String lang = original.getLanguage();
            if (!"tr".equals(lang) && !"az".equals(lang)) return;
            Locale format = Locale.getDefault(Locale.Category.FORMAT);
            Locale display = Locale.getDefault(Locale.Category.DISPLAY);
            Locale.setDefault(Locale.ROOT);                       // what toLowerCase()/toUpperCase() use
            Locale.setDefault(Locale.Category.FORMAT, format);    // numbers, dates: unchanged
            Locale.setDefault(Locale.Category.DISPLAY, display);
            replaced = original;
            LogManager.getLogger("AoE-QoL").info("System language is {} ({}): text case rules switched to neutral so "
                    + "mods' internal names don't get a dotless i (prevents startup crashes). Number formats are unchanged.",
                    original.getDisplayLanguage(Locale.ENGLISH), original);
        } catch (Throwable t) {
            LogManager.getLogger("AoE-QoL").warn("Could not adjust the Turkish text case rules", t);
        }
    }
}
