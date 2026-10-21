package com.itstwoam.aoeqol.misc;

import java.util.Locale;

/**
 * Java's no-argument String.toLowerCase() follows the computer's language. In Turkish, a capital I becomes a
 * dotless "ı", so Compact Solar Arrays' internal names turn into "medıum_voltage" / "hıgh_voltage". Those names
 * are used for its block states and the solar hats' registry names, which then don't match its model files and
 * registry rules, and the game fails to start on a Turkish system.
 *
 * Fix: Compact Solar Arrays' toLowerCase() calls on those internal names use Locale.ROOT, giving the same result on
 * every system. Nothing shown to the player changes.
 */
public final class LocaleFix {
    private LocaleFix() {}

    public static String lower(String s) {
        return s.toLowerCase(Locale.ROOT);
    }
}
