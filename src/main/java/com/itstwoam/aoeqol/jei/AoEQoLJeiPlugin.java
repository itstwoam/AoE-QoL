package com.itstwoam.aoeqol.jei;

import com.itstwoam.aoeqol.AoEQoL;
import mezz.jei.api.BlankModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import net.minecraftforge.fml.common.Loader;

/** JEI entry point. Only touches IC2 (through {@link CannerJei}) when IC2 is installed. */
@JEIPlugin
public class AoEQoLJeiPlugin extends BlankModPlugin {
    @Override
    public void register(IModRegistry registry) {
        if (!Loader.isModLoaded("IC2")) return;
        try {
            CannerJei.register(registry);
        } catch (Throwable t) {
            AoEQoL.LOG.error("Could not add the IC2 Canning Machine to JEI", t);
        }
    }
}
