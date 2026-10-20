package com.itstwoam.aoeqol.client;

import com.itstwoam.aoeqol.AoEQoL;
import com.itstwoam.aoeqol.AoEQoLConfig;
import java.lang.reflect.Field;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * BetterFPS's "Async" creative search runs the search on a new background thread for every key typed, and each
 * of those threads asks every item for its tooltip, i.e. runs dozens of mods' tooltip code off the main thread
 * and at the same time as each other. None of that code is thread-safe; in Age of Engineering it crashes the
 * game, e.g. while Mekanism draws its cables in the search results.
 *
 * AoE-QoL switches BetterFPS's setting from "Async" to "On" for the session (BetterFPS reads it each time the
 * creative inventory opens). The search keeps BetterFPS's fast incremental filtering, just on the main thread.
 * BetterFPS's config file is not changed. Opt out with preventAsyncCreativeSearch=false (config/aoeqol.cfg or
 * Mods -> AoE-QoL -> Config); changing it in game applies the next time the creative inventory opens.
 * The player is told once in chat; afterwards only the log says so.
 */
public final class BetterFpsFix {
    static final String MESSAGE = "[AoE-QoL] BetterFPS's \"Async\" creative search was switched to \"On\" because it can crash "
            + "the game (for example with Mekanism cables). Search stays fast. To use Async anyway, turn off "
            + "\"Prevent async creative search\" in Mods > AoE-QoL > Config. (Shown once.)";

    private static Object bfpsConfig;
    private static Field async;
    private static boolean originalAsync;
    private boolean done;

    private BetterFpsFix() {}

    public static boolean installed() {
        try {
            Class.forName("guichaguri.betterfps.BetterFpsHelper", false, BetterFpsFix.class.getClassLoader());
            return true;
        } catch (Throwable notInstalled) {
            return false;
        }
    }

    /** Client only, after {@link AoEQoLConfig#init}. Does nothing if BetterFPS isn't installed. */
    public static void init() {
        if (!installed()) return;
        try {
            bfpsConfig = Class.forName("guichaguri.betterfps.BetterFpsHelper").getMethod("getConfig").invoke(null);
            async = bfpsConfig.getClass().getField("asyncSearch");
            originalAsync = async.getBoolean(bfpsConfig);
        } catch (Throwable t) {
            AoEQoL.LOG.warn("Could not read BetterFPS's creative search setting, leaving it as is", t);
            return;
        }
        if (apply() && !AoEQoLConfig.noticeShown.getBoolean()) MinecraftForge.EVENT_BUS.register(new BetterFpsFix());
    }

    /** Applies the current setting; called at startup and when the setting is changed in game. True if Async was switched off. */
    public static boolean apply() {
        if (async == null) return false;
        try {
            if (!originalAsync) {
                AoEQoL.LOG.info("BetterFPS async creative search is off, nothing to change");
                return false;
            }
            if (!AoEQoLConfig.preventAsyncSearch.getBoolean()) {
                async.setBoolean(bfpsConfig, true);
                AoEQoL.LOG.info("BetterFPS creative search left as configured (Async) - preventAsyncCreativeSearch is off");
                return false;
            }
            async.setBoolean(bfpsConfig, false);
            AoEQoL.LOG.info("BetterFPS \"Async\" creative search switched to \"On\" to prevent crashes "
                    + "(turn off preventAsyncCreativeSearch in Mods > AoE-QoL > Config to keep Async)");
            return true;
        } catch (Throwable t) {
            AoEQoL.LOG.warn("Could not change BetterFPS's creative search setting", t);
            return false;
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (done || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;
        done = true;
        mc.thePlayer.addChatMessage(new TextComponentString(TextFormatting.GOLD + MESSAGE));
        AoEQoLConfig.noticeShown.set(true);
        AoEQoLConfig.config.save();
    }
}
