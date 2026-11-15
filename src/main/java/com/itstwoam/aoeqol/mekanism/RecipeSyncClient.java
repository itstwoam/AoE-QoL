package com.itstwoam.aoeqol.mekanism;

import java.util.List;

import net.minecraft.client.Minecraft;

/** Client-only part of {@link RecipeSync}; only loaded on the game side. */
final class RecipeSyncClient {
    private RecipeSyncClient() {}

    /** Off the game's main thread: queues the recipe update there and returns true. */
    static boolean later(final List<?> types, final Object manager) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.isCallingFromMinecraftThread()) return false;
        mc.addScheduledTask(new Runnable() {
            @Override public void run() { RecipeSync.run(types, manager); }
        });
        return true;
    }
}
