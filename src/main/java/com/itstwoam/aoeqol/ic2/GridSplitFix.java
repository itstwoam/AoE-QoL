package com.itstwoam.aoeqol.ic2;

import java.lang.reflect.Field;

/**
 * IC2's energy net keeps a cached list of routes (source to machine) per network and only recalculates a network
 * that is marked "dirty". When a cable, splitter or machine leaves a network and that splits it in two, IC2 keeps
 * one piece as the old network (marked dirty) and moves the other pieces into brand-new networks, which are never
 * marked. A network that has never been calculated has no routes, so its generators and machines stay connected
 * but dead until something is placed in it or the world is reloaded. Splits happen when a cable is broken, when an
 * EU-Splitter Cable in the middle of a run switches off, or when part of a network unloads.
 *
 * Fix: every new network starts out marked dirty, so IC2 calculates its routes in the same update. Networks IC2
 * creates in other places are marked by IC2 itself right away, so nothing else changes.
 *
 * Called at the end of IC2's Grid constructor.
 */
public final class GridSplitFix {
    private static volatile Field dirty;
    private static boolean broken;

    private GridSplitFix() {}

    public static void created(Object grid) {
        if (broken || !com.itstwoam.aoeqol.AoEQoLConfig.ON_IC2_SPLIT_NETWORKS) return;
        try {
            Field f = dirty;
            if (f == null) {
                f = grid.getClass().getDeclaredField("dirty");
                f.setAccessible(true);
                dirty = f;
            }
            f.setBoolean(grid, true);
        } catch (Throwable t) {
            broken = true;
            System.out.println("[AoE-QoL] IC2 energy net split fix disabled, please report: " + t);
        }
    }
}
