package com.itstwoam.aoeqol.client;

import com.itstwoam.aoeqol.AoEQoL;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Fixes items going invisible in the main inventory after using Just Enough Calculation.
 *
 * Opening any container screen makes it the client's "open container". Vanilla resets that to the player's
 * inventory when the screen is closed with {@code EntityPlayerSP.closeScreen()}, but JEC's screens close with
 * a plain {@code displayGuiScreen(null)}, so the client keeps thinking JEC's container (window ID 100) is open.
 * The server keeps sending inventory updates for window 0; the client applies hotbar slots unconditionally but
 * drops every other slot whose window ID doesn't match its open container. Items picked up into the main
 * inventory (e.g. from Storage Drawers with a full hotbar) are then invisible until something forces a resync.
 *
 * JEC's containers are client-only (its GUI handler never creates a server container), so whenever one is still
 * the open container while no screen is shown, it is stale and safe to reset - exactly what closeScreen() does.
 */
public final class JecContainerFix {
    private static final String JEC_PACKAGE = "pers.towdium.just_enough_calculation.";
    private boolean logged;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;
        if (player == null || mc.currentScreen != null) return;
        Container open = player.openContainer;
        if (open == null || open == player.inventoryContainer) return;
        if (!open.getClass().getName().startsWith(JEC_PACKAGE)) return;
        player.openContainer = player.inventoryContainer;
        if (!logged) {
            logged = true;
            AoEQoL.LOG.info("Reset a leftover JEC container after its screen closed (prevents invisible inventory items)");
        }
    }
}
