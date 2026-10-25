package com.itstwoam.aoeqol.client;

import com.itstwoam.aoeqol.AoEQoL;
import com.itstwoam.aoeqol.AoEQoLConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.fml.client.IModGuiFactory;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.GuiConfigEntries;
import net.minecraftforge.fml.client.config.IConfigElement;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

/** Mods -> AoE-QoL -> Config: AoE-QoL's settings in game. Changes apply without a restart. */
public final class ConfigGuiFactory implements IModGuiFactory {
    @Override public void initialize(Minecraft mc) {}
    @Override public Class<? extends GuiScreen> mainConfigGuiClass() { return Screen.class; }
    @Override public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() { return null; }
    @Override public RuntimeOptionGuiHandler getHandlerFor(RuntimeOptionCategoryElement element) { return null; }

    public static final class Screen extends SavingScreen {
        public Screen(GuiScreen parent) {
            super(parent, elements(), null, false, false, "AoE-QoL", null);
        }

        private static List<IConfigElement> elements() {
            List<IConfigElement> list = new ArrayList<IConfigElement>();
            list.add(section(AoEQoLConfig.MINECRAFT));
            if (BetterFpsFix.installed()) list.add(section(AoEQoLConfig.BETTERFPS));
            if (Loader.isModLoaded("IC2")) list.add(section(AoEQoLConfig.IC2));
            if (Loader.isModLoaded("je_calculation")) list.add(section(AoEQoLConfig.JEC));
            if (Loader.isModLoaded("rftools")) list.add(section(AoEQoLConfig.RFTOOLS));
            return list;
        }

        private static IConfigElement section(String category) {
            return new ConfigElement(AoEQoLConfig.config.getCategory(category).setConfigEntryClass(Section.class));
        }
    }

    /** A settings screen where Escape works like Done: it keeps your changes instead of throwing them away. */
    public static class SavingScreen extends GuiConfig {
        private static final GuiButton DONE = new GuiButton(2000, 0, 0, "");   // GuiConfig's Done button id

        SavingScreen(GuiScreen parent, List<IConfigElement> elements, String configID, boolean worldRestart,
                     boolean mcRestart, String title, String titleLine2) {
            super(parent, elements, AoEQoL.MODID, configID, worldRestart, mcRestart, title, titleLine2);
        }

        @Override
        protected void keyTyped(char typedChar, int keyCode) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                actionPerformed(DONE);
            } else {
                super.keyTyped(typedChar, keyCode);
            }
        }
    }

    /**
     * A section button (Minecraft, IC2, RFTools...). Forge's own section screens don't save when you press Done on
     * them: the changes are only kept until Done is also pressed on the main AoE-QoL screen, and leaving that one
     * with Escape threw them away. This section screen saves on its own Done (and on Escape).
     */
    public static final class Section extends GuiConfigEntries.CategoryEntry {
        public Section(GuiConfig owningScreen, GuiConfigEntries owningEntryList, IConfigElement configElement) {
            super(owningScreen, owningEntryList, configElement);
        }

        @Override
        protected GuiScreen buildChildScreen() {
            // Runs while the main screen is still being built, so take nothing but the restart flags from it.
            return new SavingScreen(owningScreen, configElement.getChildElements(), AoEQoL.MODID,
                    configElement.requiresWorldRestart(), configElement.requiresMcRestart(), "AoE-QoL", name);
        }
    }

    /** Saves and applies settings changed on the config screen. */
    public static final class ChangeHandler {
        @SubscribeEvent
        public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (!AoEQoL.MODID.equals(event.getModID())) return;
            AoEQoLConfig.config.save();
            AoEQoLConfig.sync();
            BetterFpsFix.apply();
        }
    }
}
