package com.itstwoam.aoeqol.enderio;

import com.itstwoam.aoeqol.AoEQoLConfig;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ender IO item filters (Basic/Advanced/Limited, Species) do what their mode says.
 *
 * Ender IO treats an empty Item or Species filter as "no filter": an empty whitelist lets everything through, the
 * same as an empty blacklist. That reaches every user of the filter: item conduits (extract, insert, routing), the
 * Vacuum Chest and the Dimensional Transceiver. With the setting on, an empty whitelist lets nothing through; new
 * filter items still start as an empty whitelist (Ender IO's default), so they pass nothing until set up. Only the
 * transceiver's built-in filters start as blacklist, so a new transceiver still passes everything. Filters already
 * in a world keep the mode they were saved with.
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

    private static volatile java.lang.reflect.Field sendField, receiveField;

    /**
     * End of the TileTransceiver constructor: its two built-in filters (not filter items) start as blacklist, so a
     * new transceiver passes everything until set up. A saved transceiver loads its own mode afterwards (copyFrom).
     */
    public static void transceiverCreated(Object te) {
        if (!AoEQoLConfig.ON_EIO_STRICT_FILTERS || te == null) return;
        try {
            if (sendField == null) {
                java.lang.reflect.Field a = te.getClass().getDeclaredField("sendItemFilter"), b = te.getClass().getDeclaredField("recieveItemFilter");
                a.setAccessible(true); b.setAccessible(true);
                receiveField = b; sendField = a;
            }
            for (Object f : new Object[] {sendField.get(te), receiveField.get(te)})
                if (f != null) methods(f.getClass())[2].invoke(f, true);
        } catch (Throwable t) {
            // leave Ender IO's default
        }
    }
}
