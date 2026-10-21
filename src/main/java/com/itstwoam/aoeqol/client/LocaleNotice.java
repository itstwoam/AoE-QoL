package com.itstwoam.aoeqol.client;

import com.itstwoam.aoeqol.AoEQoLConfig;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Tells a Turkish/Azerbaijani player once in chat about {@link com.itstwoam.aoeqol.asm.TurkishLocaleFix}. */
public final class LocaleNotice {
    static final String MESSAGE = "[AoE-QoL] Your computer's language is Turkish or Azerbaijani, which makes several "
            + "mods in this pack crash on startup (a capital I becomes a dotless ı in their internal names). AoE-QoL "
            + "switched Java's text case rules to neutral to prevent that. Minecraft's language and number formats "
            + "are not affected. (Shown once.)";

    private boolean done;

    private LocaleNotice() {}

    /** Client only, after {@link AoEQoLConfig#init}. */
    public static void init() {
        String lang = System.getProperty("user.language", "");
        boolean applied = ("tr".equals(lang) || "az".equals(lang)) && Locale.ROOT.equals(Locale.getDefault());
        if (applied && !AoEQoLConfig.localeNoticeShown.getBoolean()) MinecraftForge.EVENT_BUS.register(new LocaleNotice());
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (done || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;
        done = true;
        mc.thePlayer.addChatMessage(new TextComponentString(TextFormatting.GOLD + MESSAGE));
        AoEQoLConfig.localeNoticeShown.set(true);
        AoEQoLConfig.config.save();
    }
}
