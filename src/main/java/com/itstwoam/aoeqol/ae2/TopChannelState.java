package com.itstwoam.aoeqol.ae2;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;

/**
 * The One Probe shows "Device Online / Device Missing Channel / Device Offline" only for AE2 things that implement
 * IPowerChannelState: every part, plus the Wireless Access Point, Molecular Assembler, Crafting CPU blocks and Quartz
 * Growth Accelerator. Blocks that need a channel but don't implement it (Interface, ME Drive, ME Chest, IO Port,
 * Security Station, Spatial IO Port, Spatial Pylon) show nothing, so a missing channel is invisible.
 *
 * Runs first in AE2's tile PowerStateInfoProvider.addProbeInfo (server side). For a block that doesn't implement
 * IPowerChannelState but whose network connection needs a channel, it adds the same line AE2 uses, from the network
 * connection's own active / powered state. Anything else, or any error, adds nothing and AE2 carries on as before.
 */
public final class TopChannelState {
    private TopChannelState() {}

    private static volatile boolean broken;
    private static Class<?> powerChannelState;
    private static Method getProxy, getFlags, isActive, isPowered, getLocal, text;
    private static Object online, missing, offline;

    public static void addState(Object tile, Object probeInfo) {
        if (broken || tile == null || probeInfo == null) return;
        try {
            if (powerChannelState == null) init(tile.getClass().getClassLoader());
        } catch (Throwable t) {
            broken = true; // AE2 or TOP classes not as expected: never try again
            return;
        }
        try {
            if (powerChannelState.isInstance(tile)) return; // AE2 handles it
            Method gp = findGetProxy(tile.getClass());
            if (gp == null) return;
            Object proxy = gp.invoke(tile);
            if (proxy == null) return;
            if (!needsChannel((Collection<?>) getFlags.invoke(proxy))) return;
            boolean active = (Boolean) isActive.invoke(proxy);
            boolean powered = (Boolean) isPowered.invoke(proxy);
            Object key = active && powered ? online : powered ? missing : offline;
            text.invoke(probeInfo, getLocal.invoke(key));
        } catch (Throwable t) {
            // this block only: add nothing, AE2 carries on
        }
    }

    private static boolean needsChannel(Collection<?> flags) {
        if (flags == null) return false;
        for (Object f : flags) if ("REQUIRE_CHANNEL".equals(((Enum<?>) f).name())) return true;
        return false;
    }

    private static Method findGetProxy(Class<?> c) {
        try {
            Method m = c.getMethod("getProxy");
            return m.getReturnType().getName().equals("appeng.me.helpers.AENetworkProxy") ? m : null;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static synchronized void init(ClassLoader cl) throws Exception {
        if (powerChannelState != null) return;
        Class<?> proxy = Class.forName("appeng.me.helpers.AENetworkProxy", false, cl);
        getFlags = proxy.getMethod("getFlags");
        isActive = proxy.getMethod("isActive");
        isPowered = proxy.getMethod("isPowered");
        Class<?> txt = Class.forName("appeng.integration.modules.theoneprobe.TheOneProbeText", true, cl);
        getLocal = txt.getMethod("getLocal");
        online = field(txt, "DEVICE_ONLINE");
        missing = field(txt, "DEVICE_MISSING_CHANNEL");
        offline = field(txt, "DEVICE_OFFLINE");
        text = Class.forName("mcjty.theoneprobe.api.IProbeInfo", false, cl).getMethod("text", String.class);
        powerChannelState = Class.forName("appeng.api.implementations.IPowerChannelState", false, cl);
    }

    private static Object field(Class<?> c, String name) throws Exception {
        Field f = c.getField(name);
        return f.get(null);
    }
}
