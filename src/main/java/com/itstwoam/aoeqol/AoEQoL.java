package com.itstwoam.aoeqol;

import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * AoE-QoL: quality-of-life fixes for the Age of Engineering modpack (MC 1.10.2).
 *
 * <ul>
 *   <li>Just Enough Calculation fixes: applied in memory by {@link com.itstwoam.aoeqol.asm.JecTransformer}.</li>
 *   <li>JEI pages for the IC2 Canning Machine: {@link com.itstwoam.aoeqol.jei.AoEQoLJeiPlugin}.</li>
 * </ul>
 * Every part switches itself off when the mod it works with isn't installed.
 */
@Mod(modid = AoEQoL.MODID, name = AoEQoL.NAME, version = AoEQoL.VERSION,
     acceptedMinecraftVersions = "[1.10.2]", acceptableRemoteVersions = "*",
     dependencies = "after:je_calculation;after:IC2;after:JEI")
public class AoEQoL {
    public static final String MODID = "aoeqol";
    public static final String NAME = "AoE-QoL";
    public static final String VERSION = "@VERSION@";
    public static final Logger LOG = LogManager.getLogger(NAME);

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        LOG.info("JEC fixes: {}", Loader.isModLoaded("je_calculation") ? "active" : "off (Just Enough Calculation not installed)");
        LOG.info("JEI Canning Machine pages: {}",
                Loader.isModLoaded("JEI") && Loader.isModLoaded("IC2") ? "active" : "off (needs JEI and IC2)");
    }
}
