package com.itstwoam.aoeqol.client;

import com.itstwoam.aoeqol.AoEQoL;
import java.io.File;
import java.lang.reflect.Field;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
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
 * BetterFPS's config file is not changed. Opt out with preventAsyncCreativeSearch=false in config/aoeqol.cfg.
 * The player is told once in chat; afterwards only the log says so.
 */
public final class BetterFpsFix {
    static final String CATEGORY = "betterfps";
    static final String MESSAGE = "[AoE-QoL] BetterFPS's \"Async\" creative search was switched to \"On\" because it can crash "
            + "the game (for example with Mekanism cables). Search stays fast. To use Async anyway, set "
            + "preventAsyncCreativeSearch=false in config/aoeqol.cfg. (Shown once.)";

    private static Configuration config;
    private static Property noticeShown;
    private boolean done;

    private BetterFpsFix() {}

    /** Client only. Does nothing if BetterFPS isn't installed. */
    public static void init(File configDir) {
        Class<?> helper;
        try {
            helper = Class.forName("guichaguri.betterfps.BetterFpsHelper");
        } catch (Throwable notInstalled) {
            return;
        }
        try {
            config = new Configuration(new File(configDir, "aoeqol.cfg"));
            Property prevent = config.get(CATEGORY, "preventAsyncCreativeSearch", true,
                    "BetterFPS's \"Async\" creative search can crash the game (other mods' tooltip code isn't safe to run on\n"
                  + "background threads). true: AoE-QoL uses BetterFPS's normal fast search instead (\"On\"); BetterFPS's own\n"
                  + "config file is left unchanged. false: leave BetterFPS as configured.");
            noticeShown = config.get(CATEGORY, "noticeShown", false,
                    "Set by AoE-QoL after it has mentioned the change in chat once.");
            if (config.hasChanged()) config.save();
            if (!prevent.getBoolean()) {
                AoEQoL.LOG.info("BetterFPS creative search left as configured (preventAsyncCreativeSearch=false)");
                return;
            }
            Object bfps = helper.getMethod("getConfig").invoke(null);
            Field async = bfps.getClass().getField("asyncSearch");
            if (!async.getBoolean(bfps)) {
                AoEQoL.LOG.info("BetterFPS async creative search is off, nothing to change");
                return;
            }
            async.setBoolean(bfps, false);
            AoEQoL.LOG.info("BetterFPS \"Async\" creative search switched to \"On\" to prevent crashes "
                    + "(set preventAsyncCreativeSearch=false in config/aoeqol.cfg to keep Async)");
            if (!noticeShown.getBoolean()) MinecraftForge.EVENT_BUS.register(new BetterFpsFix());
        } catch (Throwable t) {
            AoEQoL.LOG.warn("Could not change BetterFPS's creative search setting, leaving it as is", t);
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (done || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;
        done = true;
        mc.thePlayer.addChatMessage(new TextComponentString(TextFormatting.GOLD + MESSAGE));
        noticeShown.set(true);
        config.save();
    }
}
