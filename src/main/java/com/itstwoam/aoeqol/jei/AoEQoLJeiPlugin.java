package com.itstwoam.aoeqol.jei;

import com.itstwoam.aoeqol.AoEQoL;
import mezz.jei.api.BlankModPlugin;
import mezz.jei.api.IJeiRuntime;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import net.minecraftforge.fml.common.Loader;

/** JEI entry point. Only touches IC2 (through {@link CannerJei} and {@link FoamJei}) when IC2 is installed. */
@JEIPlugin
public class AoEQoLJeiPlugin extends BlankModPlugin {
    /** JEI's runtime once it's ready (used by the Storage Scanner search sync). */
    public static IJeiRuntime runtime;

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    @Override
    public void register(IModRegistry registry) {
        if (!Loader.isModLoaded("IC2")) return;
        try {
            CannerJei.register(registry);
        } catch (Throwable t) {
            AoEQoL.LOG.error("Could not add the IC2 Canning Machine to JEI", t);
        }
        try {
            FoamJei.register(registry);
        } catch (Throwable t) {
            AoEQoL.LOG.error("Could not add IC2 Reinforced Stone to JEI", t);
        }
    }
}
