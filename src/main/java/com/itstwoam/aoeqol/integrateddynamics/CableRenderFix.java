package com.itstwoam.aoeqol.integrateddynamics;

import java.lang.reflect.Field;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Integrated Dynamics' logic cables keep a ready-made description of how to draw themselves (which sides have an
 * arm, which parts sit on them) and only rebuild it when an update from the server changes the connections.
 * When you place a cable or a cable-connecting block next to one, your game works out the new connection itself
 * right away but keeps the old description, so the redraw that follows leaves the new arm out (only its selection
 * outline shows). The server's update a moment later has the same connections, so nothing asks for another
 * redraw; the arm stays invisible until something else redraws that area.
 *
 * Fix: whenever a cable updates its connections, its stored description is dropped, and on the client the block
 * is redrawn. Display only; nothing about the network changes.
 *
 * Called at the end of CableDefault.updateConnections().
 */
public final class CableRenderFix {
    private static Field tileField, cacheField;
    private static boolean broken;

    private CableRenderFix() {}

    public static void afterUpdateConnections(Object cable) {
        if (broken) return;
        try {
            Object tile = tile(cable);
            if (!(tile instanceof TileEntity)) return;
            Field cache = cache(tile.getClass());
            if (cache == null) return;
            cache.set(tile, null);
            TileEntity te = (TileEntity) tile;
            World world = te.getWorld();
            if (world != null && world.isRemote) {
                BlockPos pos = te.getPos();
                world.markBlockRangeForRenderUpdate(pos, pos);
            }
        } catch (Throwable t) {
            broken = true;
            System.out.println("[AoE-QoL] Integrated Dynamics cable redraw fix disabled, please report: " + t);
        }
    }

    private static Object tile(Object cable) throws Exception {
        if (tileField == null) {
            Class<?> c = cable.getClass();
            while (c != null && !c.getName().equals("org.cyclops.integrateddynamics.capability.cable.CableTile")) c = c.getSuperclass();
            if (c == null) return null;   // another kind of cable (part cables etc.): not cached this way
            Field f = c.getDeclaredField("tile");
            f.setAccessible(true);
            tileField = f;
        }
        if (!tileField.getDeclaringClass().isInstance(cable)) return null;
        return tileField.get(cable);
    }

    private static Field cache(Class<?> tileClass) {
        Field f = cacheField;
        if (f != null) return f.getDeclaringClass().isAssignableFrom(tileClass) ? f : null;
        for (Class<?> c = tileClass; c != null; c = c.getSuperclass()) {
            if (!c.getName().equals("org.cyclops.integrateddynamics.core.tileentity.TileMultipartTicking")) continue;
            try {
                f = c.getDeclaredField("cachedState");
                f.setAccessible(true);
                cacheField = f;
                return f;
            } catch (NoSuchFieldException e) {
                return null;
            }
        }
        return null;
    }
}
