package com.itstwoam.aoeqol.ic2;

import com.itstwoam.aoeqol.AoEQoLConfig;

/**
 * IC2's EU-Splitter Cable stops passing EU while it gets a redstone signal, but when redstone dust or an Ender IO
 * redstone conduit asks whether it can connect, it says no: IC2's base class lets a block connect only while it
 * emits redstone itself, and the splitter never overrides that. Dust then doesn't bend into it, and conduits don't
 * attach or output to it unless forced with a Yeta Wrench.
 *
 * Fix: the splitter cable (only it) accepts redstone connections. Setting: splitterCableConnectsRedstone
 * (Mods -> AoE-QoL -> Config). Applied to the result of IC2's TileEntityBlock.connectRedstone(side).
 */
public final class SplitterRedstone {
    private static Class<?> splitter;
    private static boolean lookedUp;

    private SplitterRedstone() {}

    public static boolean connectRedstone(boolean original, Object tile) {
        if (original || AoEQoLConfig.splitterRedstone == null || !AoEQoLConfig.splitterRedstone.getBoolean()) return original;
        if (!lookedUp) {
            lookedUp = true;
            try {
                splitter = Class.forName("ic2.core.block.wiring.TileEntityCableSplitter", false, tile.getClass().getClassLoader());
            } catch (Throwable t) {
                System.out.println("[AoE-QoL] IC2 splitter cable not found, redstone connection fix off: " + t);
            }
        }
        return splitter != null && splitter.isInstance(tile);
    }
}
