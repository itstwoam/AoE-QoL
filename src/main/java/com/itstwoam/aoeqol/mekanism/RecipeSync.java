package com.itstwoam.aoeqol.mekanism;

import java.lang.reflect.Method;
import java.util.List;

import net.minecraftforge.fml.common.FMLCommonHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Mekanism (and Mekanism Generators) remove and re-add the crafting recipes of enabled/disabled machines whenever
 * their config is synced or changed in game. The config sync from the server is handled on a network thread. In
 * single player the game and the built-in server share one crafting recipe list, so that rewrite ran while the
 * server was reading the list (any machine that looks up a recipe: Ender IO fake players, the Mechanical Crafter
 * and others) and the game crashed ("Ticking block entity", ConcurrentModificationException in CraftingManager).
 *
 * Fix: while a built-in server runs in this game, the game side leaves the list alone. The server already set up
 * the recipes at startup from the same config (single player shares Mekanism's config too). Connected to another
 * server, the rewrite still happens, on the game's main thread instead of the network thread.
 */
public final class RecipeSync {
    private RecipeSync() {}

    private static final Logger LOG = LogManager.getLogger("AoE-QoL");
    private static Method update;
    private static boolean logged;

    /** Replaces TypeConfigManager.updateConfigRecipes(List, TypeConfigManager) in Mekanism's config sync/change. */
    public static void updateConfigRecipes(List<?> types, Object manager) {
        try {
            boolean client = FMLCommonHandler.instance().getSide().isClient();
            if (client && FMLCommonHandler.instance().getMinecraftServerInstance() != null) {
                if (!logged) {
                    logged = true;
                    LOG.info("Mekanism config sync: recipe list left alone (single player shares it with the built-in server; "
                            + "machine recipe changes made in game apply after a restart)");
                }
                return;
            }
            if (client && RecipeSyncClient.later(types, manager)) return;
        } catch (Throwable t) {
            LOG.warn("Mekanism config sync check failed, using Mekanism's own code", t);
        }
        run(types, manager);
    }

    static void run(List<?> types, Object manager) {
        try {
            Method m = update;
            if (m == null) {
                Class<?> c = Class.forName("mekanism.common.config.TypeConfigManager");
                m = c.getMethod("updateConfigRecipes", List.class, c);
                update = m;
            }
            m.invoke(null, types, manager);
        } catch (Throwable t) {
            LOG.warn("Mekanism config sync: could not update machine recipes", t);
        }
    }
}
