package com.itstwoam.aoeqol.enderio;

import com.itstwoam.aoeqol.AoEQoLConfig;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ender IO item filters (Basic/Advanced/Limited, Mod, Species) do what their mode says.
 *
 * Ender IO treats an empty Item or Species filter as "no filter": an empty whitelist lets everything through, the
 * same as an empty blacklist. That reaches every user of the filter: item conduits (extract, insert, routing), the
 * Vacuum Chest and the Dimensional Transceiver. With the setting on, an empty whitelist lets nothing through. New
 * filters start as blacklist, so a filter that was never set up (a transceiver's built-in ones, a fresh filter
 * item) still lets everything through. Filters already in a world keep the mode they were saved with.
 */
public final class FilterFix {
    private FilterFix() {}

    private static final Map<Class<?>, Method[]> METHODS = new ConcurrentHashMap<Class<?>, Method[]>();

    /** [isValid, isBlacklist, setBlacklist], looked up once per filter class. */
    private static Method[] methods(Class<?> c) throws NoSuchMethodException {
        Method[] m = METHODS.get(c);
        if (m == null) {
            m = new Method[] {c.getMethod("isValid"), c.getMethod("isBlacklist"), c.getMethod("setBlacklist", boolean.class)};
            METHODS.put(c, m);
        }
        return m;
    }

    /** ItemFilter/SpeciesItemFilter.doesItemPassFilter return value: an empty whitelist passes nothing. */
    public static boolean pass(boolean result, Object filter) {
        if (!result || !AoEQoLConfig.ON_EIO_STRICT_FILTERS || filter == null) return result;
        try {
            Method[] m = methods(filter.getClass());
            if (!(Boolean) m[0].invoke(filter) && !(Boolean) m[1].invoke(filter)) return false;
        } catch (Throwable t) {
            // leave Ender IO's answer
        }
        return result;
    }

    /** End of the Item/Mod/Species filter constructors: a new filter starts as blacklist. Saved filters load their own mode afterwards. */
    public static void created(Object filter) {
        if (!AoEQoLConfig.ON_EIO_STRICT_FILTERS || filter == null) return;
        try {
            methods(filter.getClass())[2].invoke(filter, true);
        } catch (Throwable t) {
            // leave Ender IO's default
        }
    }
}
