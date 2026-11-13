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
    public static final String BETTERFPS = "betterfps", RFTOOLS = "rftools", IC2 = "ic2", LOCALE = "locale", MINECRAFT = "minecraft", JEC = "jec", ENDERIO = "enderio", ADVSOLAR = "advsolar";

    public static Configuration config;
    public static Property preventAsyncSearch, noticeShown, scannerJeiSync, splitterRedstone, localeNoticeShown, ignoreMouseJump,
            jecConditionTags, jecOreUnify, jecPageArrows, rftoolsMatching, rftoolsEmptyTag, ic2SplitNetworks, quitFreezeFix,
            eioStrictFilters, mtQuartzToCertus;

    /**
     * Current values, read by the patched code (some of it runs very often, so it reads a field rather than parsing
     * the property each time). On until the config is loaded; updated by {@link #sync()} whenever settings change.
     */
    public static volatile boolean ON_JEC_CONDITION_TAGS = true, ON_JEC_ORE_UNIFY = true, ON_JEC_PAGE_ARROWS = true, ON_RFTOOLS_MATCHING = true, ON_RFTOOLS_EMPTY_TAG = true,
            ON_IC2_SPLIT_NETWORKS = true, ON_QUIT_FREEZE = true, ON_MOUSE_JUMP = true,
            ON_EIO_STRICT_FILTERS = true, ON_MT_QUARTZ_TO_CERTUS = true;

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
        rft.setComment("RFTools: Storage Scanner, crafting grid and Crafter.");
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
        ConfigCategory jec = config.getCategory(JEC);
        jec.setComment("Just Enough Calculation.");
        jec.setLanguageKey("aoeqol.config.jec");
        jecConditionTags = config.get(JEC, "ignoreConditionTags", true,
                "Items that only differ in condition (heat, charge, wear) count as the same item, so recipes using them\n"
              + "chain, e.g. IC2 reactor parts, batteries and energy crystals. Which tags count is set by ListIgnoredNBTKeys\n"
              + "and ListIgnoredDamage in JEC's own config. false: JEC's normal exact matching.")
                .setLanguageKey("aoeqol.config.ignoreConditionTags");
        jecOreUnify = config.get(JEC, "oreDictUnification", true,
                "Choosing an ore dictionary preference rewrites your saved recipes to use it, and recipes saved later are\n"
              + "unified automatically; /jecunify applies all preferences. With the calculator's inventory check, other\n"
              + "mods' versions in your inventory count as the preferred item. false: saved recipes are never changed.")
                .setLanguageKey("aoeqol.config.oreDictUnification");
        jecPageArrows = config.get(JEC, "pageArrowsRightWayRound", true,
                "On the calculator, item picker and ore dictionary screens JEC's page arrows are swapped (\"<\" goes to the\n"
              + "next page). true: \"<\" goes back and \">\" goes forward, like the recipe list. Applies the next time a\n"
              + "screen opens. false: JEC's own arrows.")
                .setLanguageKey("aoeqol.config.pageArrowsRightWayRound");
        rftoolsMatching = config.get(RFTOOLS, "smarterIngredientMatching", true,
                "Crafting grid and Crafter: charged batteries/crystals and worn tools are recognised, the exact variant is\n"
              + "taken first, other mods' ore dictionary equivalents come before other variants, and a wrong variant is\n"
              + "marked red. false: RFTools' normal matching.")
                .setLanguageKey("aoeqol.config.smarterIngredientMatching");
        rftoolsEmptyTag = config.get(RFTOOLS, "takeOutBrandNewItems", true,
                "Storage Scanner: brand-new IC2 electric items (an empty Re-Battery, crystal, drill...) can be taken out.\n"
              + "Showing such an item's charge gives the copy on your screen an empty data tag that the stored item\n"
              + "doesn't have, so the scanner found no match and handed over nothing. true: an empty tag counts the same\n"
              + "as none. false: RFTools' exact matching.")
                .setLanguageKey("aoeqol.config.takeOutBrandNewItems");
        ic2SplitNetworks = config.get(IC2, "fixSplitNetworks", true,
                "When a cable network is split (cable broken, splitter switching off, part of it unloading), the new pieces\n"
              + "get their routes calculated right away. false: IC2's normal behaviour, where a split-off piece can stay\n"
              + "without power until something is placed in it or the world is reloaded.")
                .setLanguageKey("aoeqol.config.fixSplitNetworks");
        ConfigCategory eio = config.getCategory(ENDERIO);
        eio.setComment("Ender IO: item filters.");
        eio.setLanguageKey("aoeqol.config.enderio");
        eioStrictFilters = config.get(ENDERIO, "filtersDoWhatTheirModeSays", true,
                "Item and Species filters (conduits, Vacuum Chest, Dimensional Transceiver): an empty whitelist lets\n"
              + "nothing through (Ender IO let everything through, like an empty blacklist). New filter items still start as\n"
              + "an empty whitelist. A new Dimensional Transceiver's built-in filters start as blacklist, so it still passes\n"
              + "everything until set up. false: Ender IO's normal behaviour.")
                .setLanguageKey("aoeqol.config.filtersDoWhatTheirModeSays");
        ConfigCategory asp = config.getCategory(ADVSOLAR);
        asp.setComment("Advanced Solar Panels: Molecular Transformer recipes.");
        asp.setLanguageKey("aoeqol.config.advsolar");
        mtQuartzToCertus = config.get(ADVSOLAR, "transformerQuartzToCertus", true,
                "Molecular Transformer turns nether quartz into certus quartz. The recipe is in ASP's recipe file but uses\n"
              + "ore dictionary names nothing in the pack registers, so ASP skips it; true: those names are swapped for the\n"
              + "ones the pack's mods register (gemQuartz, crystalCertusQuartz, dustQuartz). Recipes load at start-up, so a\n"
              + "change needs a restart. false: ASP's normal behaviour.")
                .setLanguageKey("aoeqol.config.transformerQuartzToCertus").setRequiresMcRestart(true);
        ConfigCategory minecraft = config.getCategory(MINECRAFT);
        minecraft.setComment("Minecraft itself.");
        minecraft.setLanguageKey("aoeqol.config.minecraft");
        ignoreMouseJump = config.get(MINECRAFT, "ignoreLeftoverMouseJump", true,
                "Now and then, closing a screen (inventory, chest, machine) spins your view: the cursor's distance from\n"
              + "the middle of the window is read as one big mouse movement. true: that one leftover reading is ignored,\n"
              + "real mouse movement is unchanged. false: Minecraft's normal behaviour.")
                .setLanguageKey("aoeqol.config.ignoreLeftoverMouseJump");
        quitFreezeFix = config.get(MINECRAFT, "fixQuitFreeze", true,
                "\"Save and Quit to Title\" can freeze for good (more often after opening to LAN). true: once the built-in\n"
              + "server has stopped, the game stops waiting for it. false: Minecraft's normal behaviour.")
                .setLanguageKey("aoeqol.config.fixQuitFreeze");
        config.getCategory(LOCALE).setComment("Turkish/Azerbaijani system language: always fixed, nothing to set.");
        localeNoticeShown = config.get(LOCALE, "noticeShown", false,
                "Set by AoE-QoL after it has mentioned the Turkish text case fix in chat once.").setShowInGui(false);
        if (config.hasChanged()) config.save();
        sync();
    }

    /** Copies the settings into the fields the patched code reads. Called at start-up and when settings change. */
    public static void sync() {
        ON_JEC_CONDITION_TAGS = jecConditionTags.getBoolean();
        ON_JEC_ORE_UNIFY = jecOreUnify.getBoolean();
        ON_JEC_PAGE_ARROWS = jecPageArrows.getBoolean();
        ON_RFTOOLS_MATCHING = rftoolsMatching.getBoolean();
        ON_RFTOOLS_EMPTY_TAG = rftoolsEmptyTag.getBoolean();
        ON_IC2_SPLIT_NETWORKS = ic2SplitNetworks.getBoolean();
        ON_QUIT_FREEZE = quitFreezeFix.getBoolean();
        ON_MOUSE_JUMP = ignoreMouseJump.getBoolean();
        ON_EIO_STRICT_FILTERS = eioStrictFilters.getBoolean();
        ON_MT_QUARTZ_TO_CERTUS = mtQuartzToCertus.getBoolean();
    }
}
