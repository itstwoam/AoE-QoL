package com.itstwoam.aoeqol.jei;

import com.itstwoam.aoeqol.AoEQoL;
import ic2.api.item.IC2Items;
import ic2.api.recipe.ICannerBottleRecipeManager;
import ic2.api.recipe.ICannerEnrichRecipeManager;
import ic2.api.recipe.IRecipeInput;
import ic2.api.recipe.RecipeOutput;
import ic2.api.recipe.Recipes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.IModRegistry;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

/**
 * Adds the IC2 Canning Machine to JEI. IC2 2.6.252-ex110 registers JEI pages for its other machines but not this
 * one, so items made in it (uranium fuel rods, food cans, coolant...) show no recipe when you press R.
 *
 * Recipes are read from IC2's own recipe managers when JEI loads, so changes made by the pack's scripts are
 * included. Only loaded when IC2 is installed.
 */
public final class CannerJei {
    public static final String UID_SOLID = "aoeqol.ic2.canner.solid";
    public static final String UID_ENRICH = "aoeqol.ic2.canner.enrich";

    private CannerJei() {}

    static void register(IModRegistry registry) {
        IGuiHelper gui = registry.getJeiHelpers().getGuiHelper();
        List<CannerSolidRecipe> solid = solidRecipes(Recipes.cannerBottle);
        List<CannerEnrichRecipe> enrich = enrichRecipes(Recipes.cannerEnrich);

        registry.addRecipeCategories(new CannerSolidCategory(gui), new CannerEnrichCategory(gui));
        registry.addRecipeHandlers(new SimpleHandler<CannerSolidRecipe>(CannerSolidRecipe.class, UID_SOLID),
                new SimpleHandler<CannerEnrichRecipe>(CannerEnrichRecipe.class, UID_ENRICH));
        registry.addRecipes(solid);
        registry.addRecipes(enrich);

        ItemStack canner = null;
        try {
            canner = IC2Items.getItem("te", "canner");
        } catch (Throwable ignored) {
        }
        if (canner != null) registry.addRecipeCategoryCraftingItem(canner, UID_SOLID, UID_ENRICH);
        AoEQoL.LOG.info("Added IC2 Canning Machine to JEI: {} solid canning and {} fluid enriching recipes",
                solid.size(), enrich.size());
    }

    /** Solid canning recipes (container + filling -> item). Public for tests. */
    public static List<CannerSolidRecipe> solidRecipes(ICannerBottleRecipeManager manager) {
        List<CannerSolidRecipe> out = new ArrayList<CannerSolidRecipe>();
        if (manager == null || manager.getRecipes() == null) return out;
        for (Map.Entry<ICannerBottleRecipeManager.Input, RecipeOutput> e : manager.getRecipes().entrySet()) {
            if (e.getKey() == null || e.getValue() == null) continue;
            List<ItemStack> containers = stacks(e.getKey().container);
            List<ItemStack> fillings = stacks(e.getKey().fill);
            List<ItemStack> outputs = copies(e.getValue().items);
            if (containers.isEmpty() || fillings.isEmpty() || outputs.isEmpty()) continue;
            out.add(new CannerSolidRecipe(containers, fillings, outputs));
        }
        return out;
    }

    /** Fluid enriching recipes (fluid + additive -> fluid). Public for tests. */
    public static List<CannerEnrichRecipe> enrichRecipes(ICannerEnrichRecipeManager manager) {
        List<CannerEnrichRecipe> out = new ArrayList<CannerEnrichRecipe>();
        if (manager == null || manager.getRecipes() == null) return out;
        for (Map.Entry<ICannerEnrichRecipeManager.Input, FluidStack> e : manager.getRecipes().entrySet()) {
            if (e.getKey() == null || e.getKey().fluid == null || e.getValue() == null) continue;
            List<ItemStack> additives = stacks(e.getKey().additive);
            if (additives.isEmpty()) continue;
            out.add(new CannerEnrichRecipe(e.getKey().fluid.copy(), additives, e.getValue().copy()));
        }
        return out;
    }

    /** Every item an IC2 recipe input accepts, each showing the amount the recipe uses. */
    static List<ItemStack> stacks(IRecipeInput input) {
        List<ItemStack> out = new ArrayList<ItemStack>();
        if (input == null || input.getInputs() == null) return out;
        int amount = Math.max(1, input.getAmount());
        for (ItemStack s : input.getInputs()) {
            if (s == null) continue;
            ItemStack c = s.copy();
            c.stackSize = amount;
            out.add(c);
        }
        return out;
    }

    static List<ItemStack> copies(List<ItemStack> in) {
        List<ItemStack> out = new ArrayList<ItemStack>();
        if (in == null) return out;
        for (ItemStack s : in) if (s != null) out.add(s.copy());
        return out;
    }
}
