package com.itstwoam.aoeqol.client;

import com.itstwoam.aoeqol.AoEQoL;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import pers.towdium.just_enough_calculation.JustEnoughCalculation;
import pers.towdium.just_enough_calculation.network.PlayerHandlerSP;
import pers.towdium.just_enough_calculation.util.helpers.PlayerRecordHelper;

/**
 * Just Enough Calculation keeps your recipes in one object in single player, and its screens read from it. On a
 * server, the server sends your recipes for that server and JEC points its screens at a new object made from them,
 * but it never points them back when you leave. Back in a single player world the world loads your recipes into the
 * original object as usual (and saves from it), while the calculator keeps showing the server's list, so your
 * recipes seem to be gone until the game is restarted.
 *
 * Fix: when you disconnect from a server, JEC's screens are pointed back at your single player recipes.
 */
public final class JecServerSwitchFix {
    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        // fired on the network thread; switch on the game thread like JEC's own packet handling does
        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                restore();
            }
        });
    }

    static void restore() {
        try {
            Object own = JustEnoughCalculation.proxy == null ? null : JustEnoughCalculation.proxy.getPlayerHandler();
            if (own instanceof PlayerHandlerSP) PlayerRecordHelper.setPlayerHandler((PlayerHandlerSP) own);
        } catch (Throwable t) {
            AoEQoL.LOG.warn("Could not point JEC back at the single player recipes", t);
        }
    }
}
