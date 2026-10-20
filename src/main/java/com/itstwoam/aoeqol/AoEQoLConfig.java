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
    public static final String BETTERFPS = "betterfps", RFTOOLS = "rftools";

    public static Configuration config;
    public static Property preventAsyncSearch, noticeShown, scannerJeiSync;

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
        if (config.hasChanged()) config.save();
    }
}
