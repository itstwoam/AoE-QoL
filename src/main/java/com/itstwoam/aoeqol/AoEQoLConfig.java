package com.itstwoam.aoeqol;

import java.io.File;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

/**
 * AoE-QoL's own settings, config/aoeqol.cfg, also editable in game: Mods -> AoE-QoL -> Config.
 * (The Just Enough Calculation lists live in JEC's own config file.)
 */
public final class AoEQoLConfig {
    public static final String BETTERFPS = "betterfps", RFTOOLS = "rftools", IC2 = "ic2", LOCALE = "locale", MINECRAFT = "minecraft";

    public static Configuration config;
    public static Property preventAsyncSearch, noticeShown, scannerJeiSync, splitterRedstone, localeNoticeShown, ignoreMouseJump;

    private AoEQoLConfig() {}

    public static void init(File configDir) {
        config = new Configuration(new File(configDir, "aoeqol.cfg"));
        ConfigCategory bfps = config.getCategory(BETTERFPS);
        bfps.setComment("BetterFPS: creative inventory search.");
        bfps.setLanguageKey("aoeqol.config.betterfps");
        preventAsyncSearch = config.get(BETTERFPS, "preventAsyncCreativeSearch", true,
                "BetterFPS's \"Async\" creative search can crash the game (other mods' tooltip code isn't safe to run on\n"
              + "background threads). true: AoE-QoL uses BetterFPS's normal fast search instead (\"On\"); BetterFPS's own\n"
              + "config file is left unchanged. false: leave BetterFPS as configured.")
                .setLanguageKey("aoeqol.config.preventAsyncCreativeSearch");
        noticeShown = config.get(BETTERFPS, "noticeShown", false,
                "Set by AoE-QoL after it has mentioned the change in chat once.").setShowInGui(false);
        ConfigCategory rft = config.getCategory(RFTOOLS);
        rft.setComment("RFTools: Storage Scanner.");
        rft.setLanguageKey("aoeqol.config.rftools");
        scannerJeiSync = config.get(RFTOOLS, "syncStorageScannerWithJei", true,
                "Keep the Storage Scanner's search box and JEI's search box in sync, like AE2's \"JEI synchronized\"\n"
              + "search mode: typing in either one fills in the other while the scanner is open.")
                .setLanguageKey("aoeqol.config.syncStorageScannerWithJei");
        ConfigCategory ic2 = config.getCategory(IC2);
        ic2.setComment("IndustrialCraft 2.");
        ic2.setLanguageKey("aoeqol.config.ic2");
        splitterRedstone = config.get(IC2, "splitterCableConnectsRedstone", true,
                "Let redstone dust and Ender IO redstone conduits connect to the EU-Splitter Cable (it reacts to redstone,\n"
              + "but IC2 refuses the connection, so conduits need a Yeta Wrench and dust doesn't bend into it).\n"
              + "Existing dust and conduits pick up the change when a block next to them changes.")
                .setLanguageKey("aoeqol.config.splitterCableConnectsRedstone");
        ConfigCategory minecraft = config.getCategory(MINECRAFT);
        minecraft.setComment("Minecraft itself.");
        minecraft.setLanguageKey("aoeqol.config.minecraft");
        ignoreMouseJump = config.get(MINECRAFT, "ignoreLeftoverMouseJump", true,
                "Now and then, closing a screen (inventory, chest, machine) spins your view: the cursor's distance from\n"
              + "the middle of the window is read as one big mouse movement. true: that one leftover reading is ignored,\n"
              + "real mouse movement is unchanged. false: Minecraft's normal behaviour.")
                .setLanguageKey("aoeqol.config.ignoreLeftoverMouseJump");
        config.getCategory(LOCALE).setComment("Turkish/Azerbaijani system language: always fixed, nothing to set.");
        localeNoticeShown = config.get(LOCALE, "noticeShown", false,
                "Set by AoE-QoL after it has mentioned the Turkish text case fix in chat once.").setShowInGui(false);
        if (config.hasChanged()) config.save();
    }
}
