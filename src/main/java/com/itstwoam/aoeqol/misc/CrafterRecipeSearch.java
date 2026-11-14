package com.itstwoam.aoeqol.misc;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.tileentity.TileEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Extra Utilities 2's Mechanical Crafter, when it has no recipe yet (always after a world loads), walks Minecraft's
 * whole crafting recipe list to find the one its grid makes. If the list changes during that walk (in single player
 * the game and the built-in server share one list, and mods change it while a world is joined), Java stops the walk
 * with an error and the game crashed ("Ticking block entity", ConcurrentModificationException in
 * TileCrafter.preOperate).
 *
 * Fix: a search interrupted by a change is thrown away and the crafter tries again next tick, on the settled list.
 * After {@value #RETRIES} interrupted searches in a row, the crafter's next search walks a copy of the list instead.
 */
public final class CrafterRecipeSearch {
    private CrafterRecipeSearch() {}

    private static final Logger LOG = LogManager.getLogger("AoE-QoL");
    static final String CRAFTER = "com.rwtema.extrautils2.tile.TileCrafter";
    static final int RETRIES = 3;

    private static Method preOperate;
    private static Field curRecipe;
    private static final Map<Object, Integer> FAILS = new WeakHashMap<>();
    /** Set while a crafter's search should walk a copy of the recipe list (server thread only). */
    private static boolean copyNext;

    /** Replaces this.preOperate() in TileAdvInteractor.update (all Extra Utilities 2 interactor machines). */
    public static boolean preOperate(Object tile) throws Throwable {
        Method m = preOperate;
        if (m == null) {
            m = findPreOperate(tile.getClass());
            preOperate = m;
        }
        boolean crafter = CRAFTER.equals(tile.getClass().getName());
        Integer fails = crafter ? FAILS.get(tile) : null;
        boolean copy = fails != null && fails >= RETRIES;
        copyNext = copy;
        try {
            boolean result = (Boolean) m.invoke(tile);
            if (fails != null) FAILS.remove(tile);
            return result;
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (!crafter || !(cause instanceof ConcurrentModificationException)) throw cause;
            int n = fails == null ? 1 : fails + 1;
            FAILS.put(tile, n);
            resetRecipe(tile);
            LOG.info("Mechanical Crafter at {}: the recipe list changed during its search, trying again next tick{}",
                where(tile), n >= RETRIES ? " (on a copy of the list)" : "");
            return false;
        } finally {
            copyNext = false;
        }
    }

    /** Replaces CraftingManager.getRecipeList() in TileCrafter.preOperate. */
    public static List<?> recipeList(Object manager) {
        List<?> list = ((net.minecraft.item.crafting.CraftingManager) manager).getRecipeList();
        if (!copyNext) return list;
        for (int i = 0; i < RETRIES; i++) {
            try {
                ArrayList<Object> copy = new ArrayList<>(list);
                copy.removeIf(r -> r == null);
                return copy;
            } catch (RuntimeException ignored) {
                // changed while copying: try again
            }
        }
        return list;
    }

    static Method findPreOperate(Class<?> c) throws NoSuchMethodException {
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            if (k.getName().equals("com.rwtema.extrautils2.tile.TileAdvInteractor")) {
                Method m = k.getDeclaredMethod("preOperate");
                m.setAccessible(true);
                return m;
            }
        }
        Method m = c.getMethod("preOperate");
        m.setAccessible(true);
        return m;
    }

    /** Clears the half-found recipe so the next tick starts the search from scratch. */
    private static void resetRecipe(Object tile) {
        try {
            Field f = curRecipe;
            if (f == null) {
                f = tile.getClass().getDeclaredField("curRecipe");
                f.setAccessible(true);
                curRecipe = f;
            }
            f.set(tile, null);
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOG.warn("Mechanical Crafter: could not reset its recipe", e);
        }
    }

    private static String where(Object tile) {
        try {
            TileEntity te = (TileEntity) tile;
            return te.getPos().getX() + "," + te.getPos().getY() + "," + te.getPos().getZ();
        } catch (RuntimeException e) {
            return "?";
        }
    }
}
