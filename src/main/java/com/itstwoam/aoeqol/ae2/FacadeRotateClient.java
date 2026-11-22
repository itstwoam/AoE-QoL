package com.itstwoam.aoeqol.ae2;

import com.itstwoam.aoeqol.AoEQoLConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

/**
 * Client half of {@link FacadeRotate}. Catches the attack button press itself (before Minecraft turns it into a
 * click or a held dig), so one press is one turn, holding the button does nothing more, and the facade never starts
 * breaking. Anything else (other item, no facade under the cursor, a part in front of it, server without the
 * feature) passes through untouched.
 */
public final class FacadeRotateClient {
    @SubscribeEvent
    public void onMouse(MouseEvent event) {
        if (!event.isButtonstate() || event.getButton() < 0) return;
        if (!AoEQoLConfig.ON_AE2_ROTATE_FACADES || !FacadeRotate.serverOn) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen != null || mc.thePlayer == null || mc.theWorld == null || mc.thePlayer.isSpectator()) return;
        if (event.getButton() - 100 != mc.gameSettings.keyBindAttack.getKeyCode()) return;
        RayTraceResult hit = mc.objectMouseOver;
        if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK || hit.getBlockPos() == null || hit.hitVec == null) return;
        if (!FacadeRotate.isTool(mc.thePlayer.getHeldItemMainhand())) return;
        int side = FacadeRotate.facadeSideAt(mc.theWorld, hit.getBlockPos(), hit.hitVec);
        if (side < 0) return;
        event.setCanceled(true);
        mc.thePlayer.swingArm(EnumHand.MAIN_HAND);
        FacadeRotate.net.sendToServer(new FacadeRotate.RotateMsg(hit.getBlockPos(), side, hit.hitVec));
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        FacadeRotate.serverOn = false;
    }
}
