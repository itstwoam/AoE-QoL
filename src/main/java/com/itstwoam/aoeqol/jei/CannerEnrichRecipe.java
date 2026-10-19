package com.itstwoam.aoeqol.jei;

import java.util.Collections;
import java.util.List;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.BlankRecipeWrapper;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

/** Canning Machine, fluid enriching mode: fluid + additive -> fluid (e.g. distilled water + lapis -> coolant). */
public final class CannerEnrichRecipe extends BlankRecipeWrapper {
    final FluidStack input, output;
    final List<ItemStack> additives;

    public CannerEnrichRecipe(FluidStack input, List<ItemStack> additives, FluidStack output) {
        this.input = input;
        this.additives = additives;
        this.output = output;
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        ingredients.setInput(FluidStack.class, input);
        ingredients.setInputLists(ItemStack.class, Collections.singletonList(additives));
        ingredients.setOutput(FluidStack.class, output);
    }
}
