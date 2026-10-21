package com.itstwoam.aoeqol.asm;

import java.util.Map;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

/**
 * Registers {@link PatchTransformer}, which patches Just Enough Calculation's, RFTools', Ender IO's and IC2's classes in memory while
 * they load, and applies {@link TurkishLocaleFix} first. The jars on disk are never modified. If a mod isn't installed, its classes never load and nothing
 * happens.
 */
@IFMLLoadingPlugin.Name("AoE-QoL")
@IFMLLoadingPlugin.MCVersion("1.10.2")
@IFMLLoadingPlugin.TransformerExclusions({"com.itstwoam.aoeqol.asm."})
@IFMLLoadingPlugin.SortingIndex(1001)
public class LoadingPlugin implements IFMLLoadingPlugin {
    static {
        // runs while Forge discovers coremods, before any mod's classes are loaded
        TurkishLocaleFix.apply();
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[] {PatchTransformer.class.getName()};
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
