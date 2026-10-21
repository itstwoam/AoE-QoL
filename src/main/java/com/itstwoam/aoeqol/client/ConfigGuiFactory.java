package com.itstwoam.aoeqol.client;

import com.itstwoam.aoeqol.AoEQoL;
import com.itstwoam.aoeqol.AoEQoLConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.fml.client.IModGuiFactory;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Mods -> AoE-QoL -> Config: AoE-QoL's settings in game. Changes apply without a restart. */
public final class ConfigGuiFactory implements IModGuiFactory {
    @Override public void initialize(Minecraft mc) {}
    @Override public Class<? extends GuiScreen> mainConfigGuiClass() { return Screen.class; }
    @Override public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() { return null; }
    @Override public RuntimeOptionGuiHandler getHandlerFor(RuntimeOptionCategoryElement element) { return null; }

    public static final class Screen extends GuiConfig {
        public Screen(GuiScreen parent) {
            super(parent, elements(), AoEQoL.MODID, false, false, "AoE-QoL");
        }

        private static List<IConfigElement> elements() {
            List<IConfigElement> list = new ArrayList<IConfigElement>();
            if (BetterFpsFix.installed()) list.add(new ConfigElement(AoEQoLConfig.config.getCategory(AoEQoLConfig.BETTERFPS)));
            if (Loader.isModLoaded("IC2")) list.add(new ConfigElement(AoEQoLConfig.config.getCategory(AoEQoLConfig.IC2)));
            if (Loader.isModLoaded("rftools") && Loader.isModLoaded("JEI")) list.add(new ConfigElement(AoEQoLConfig.config.getCategory(AoEQoLConfig.RFTOOLS)));
            return list;
        }
    }

    /** Saves and applies settings changed on the config screen. */
    public static final class ChangeHandler {
        @SubscribeEvent
        public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (!AoEQoL.MODID.equals(event.getModID())) return;
            AoEQoLConfig.config.save();
            BetterFpsFix.apply();
        }
    }
}
