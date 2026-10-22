package com.itstwoam.aoeqol;

import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * AoE-QoL: quality-of-life fixes for the Age of Engineering modpack (MC 1.10.2).
 *
 * <ul>
 *   <li>Fixes to Just Enough Calculation, RFTools and Ender IO: applied in memory by
 *       {@link com.itstwoam.aoeqol.asm.PatchTransformer}, which calls into the {@code jec}, {@code rftools} and
 *       {@code enderio} packages.</li>
 *   <li>JEI pages for the IC2 canning machines and Reinforced Stone: {@link com.itstwoam.aoeqol.jei.AoEQoLJeiPlugin}.</li>
 *   <li>Client fix for JEC leaving its container "open" (invisible inventory items): {@link com.itstwoam.aoeqol.client.JecContainerFix}.</li>
 *   <li>JEC recipes shown again after leaving a server: {@link com.itstwoam.aoeqol.client.JecServerSwitchFix}.</li>
 *   <li>BetterFPS "Async" creative search switched to "On" (crashes): {@link com.itstwoam.aoeqol.client.BetterFpsFix}.</li>
 *   <li>Turkish/Azerbaijani system language no longer crashes startup: {@link com.itstwoam.aoeqol.asm.TurkishLocaleFix}.</li>
 *   <li>Storage Scanner search synced with JEI: {@link com.itstwoam.aoeqol.client.ScannerJeiSync}.</li>
 *   <li>Settings: {@link AoEQoLConfig}, in game through {@link com.itstwoam.aoeqol.client.ConfigGuiFactory}.</li>
 * </ul>
 * Every part switches itself off when the mod it works with isn't installed.
 */
@Mod(modid = AoEQoL.MODID, name = AoEQoL.NAME, version = AoEQoL.VERSION,
     acceptedMinecraftVersions = "[1.10.2]", acceptableRemoteVersions = "*",
     dependencies = "after:je_calculation;after:IC2;after:JEI;after:rftools",
     guiFactory = "com.itstwoam.aoeqol.client.ConfigGuiFactory")
public class AoEQoL {
    public static final String MODID = "aoeqol";
    public static final String NAME = "AoE-QoL";
    public static final String VERSION = "@VERSION@";
    public static final Logger LOG = LogManager.getLogger(NAME);

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // client-only classes are only touched on the client
        if (event.getSide().isClient() && Loader.isModLoaded("je_calculation")) {
            MinecraftForge.EVENT_BUS.register(new com.itstwoam.aoeqol.client.JecContainerFix());
            MinecraftForge.EVENT_BUS.register(new com.itstwoam.aoeqol.client.JecServerSwitchFix());
        }
        AoEQoLConfig.init(event.getModConfigurationDirectory());
        if (event.getSide().isClient()) {
            com.itstwoam.aoeqol.client.BetterFpsFix.init();
            com.itstwoam.aoeqol.client.LocaleNotice.init();
            MinecraftForge.EVENT_BUS.register(new com.itstwoam.aoeqol.client.ConfigGuiFactory.ChangeHandler());
            if (Loader.isModLoaded("rftools") && Loader.isModLoaded("JEI")) {
                MinecraftForge.EVENT_BUS.register(new com.itstwoam.aoeqol.client.ScannerJeiSync());
            }
        }
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        LOG.info("JEC fixes: {}", Loader.isModLoaded("je_calculation") ? "active" : "off (Just Enough Calculation not installed)");
        LOG.info("JEI Canning Machine pages: {}",
                Loader.isModLoaded("JEI") && Loader.isModLoaded("IC2") ? "active" : "off (needs JEI and IC2)");
    }
}
