package com.itstwoam.aoeqol.jei;

import com.itstwoam.aoeqol.AoEQoL;
import ic2.api.item.IC2Items;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.IModRegistry;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

/**
 * Adds IC2's Reinforced Stone to JEI. It has no crafting recipe: you spray an Iron Scaffold with construction
 * foam, the foam hardens in place (IC2 BlockFoam, reinforced type) and you mine the result. The amounts here are
 * IC2's own: the CF Sprayer uses 100 mB of foam per block (ItemSprayer.getFluidPerFoam), a Reinforced Iron
 * Scaffold also drops its Iron Fence when sprayed, and reinforced foam hardens into Reinforced Stone.
 */
public final class FoamJei {
    public static final String UID = "aoeqol.ic2.foam";
    static final int FOAM_PER_BLOCK = 100;

    private FoamJei() {}

    static void register(IModRegistry registry) {
        IGuiHelper gui = registry.getJeiHelpers().getGuiHelper();
        List<FoamRecipe> recipes = recipes();
        if (recipes.isEmpty()) {
            AoEQoL.LOG.warn("IC2 construction foam or scaffold items not found; Reinforced Stone JEI page not added");
            return;
        }
        String title = recipes.get(0).foam.getLocalizedName();
        registry.addRecipeCategories(new FoamCategory(gui, title));
        registry.addRecipeHandlers(new SimpleHandler<FoamRecipe>(FoamRecipe.class, UID));
        registry.addRecipes(recipes);
        ItemStack sprayer = item("foam_sprayer", null), pack = item("cf_pack", null);
        if (sprayer != null) registry.addRecipeCategoryCraftingItem(sprayer, UID);
        if (pack != null) registry.addRecipeCategoryCraftingItem(pack, UID);
        AoEQoL.LOG.info("Added IC2 Reinforced Stone (construction foam) to JEI: {} recipes", recipes.size());
    }

    /** Public for tests. */
    public static List<FoamRecipe> recipes() {
        List<FoamRecipe> out = new ArrayList<FoamRecipe>();
        Fluid fluid = FluidRegistry.getFluid("ic2construction_foam");
        ItemStack stone = item("resource", "reinforced_stone");
        if (fluid == null || stone == null) return out;
        ItemStack iron = item("scaffold", "iron"), reinforced = item("scaffold", "reinforced_iron"),
                fence = item("fence", "iron");
        if (iron != null) {
            out.add(new FoamRecipe(iron, new FluidStack(fluid, FOAM_PER_BLOCK), Collections.singletonList(stone.copy())));
        }
        if (reinforced != null) {
            List<ItemStack> outputs = fence != null ? Arrays.asList(stone.copy(), fence) : Collections.singletonList(stone.copy());
            out.add(new FoamRecipe(reinforced, new FluidStack(fluid, FOAM_PER_BLOCK), outputs));
        }
        return out;
    }

    private static ItemStack item(String name, String variant) {
        try {
            ItemStack s = IC2Items.getItem(name, variant);
            return s != null ? s.copy() : null;
        } catch (Throwable t) {
            return null;
        }
    }
}
