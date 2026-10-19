package com.itstwoam.aoeqol.jei;

import java.util.Arrays;
import java.util.List;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.BlankRecipeWrapper;
import net.minecraft.item.ItemStack;

/** Canning Machine, solid canning mode: container + filling -> output (e.g. empty fuel rod + uranium fuel). */
public final class CannerSolidRecipe extends BlankRecipeWrapper {
    final List<ItemStack> containers, fillings, outputs;

    public CannerSolidRecipe(List<ItemStack> containers, List<ItemStack> fillings, List<ItemStack> outputs) {
        this.containers = containers;
        this.fillings = fillings;
        this.outputs = outputs;
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        ingredients.setInputLists(ItemStack.class, Arrays.asList(containers, fillings));
        ingredients.setOutputs(ItemStack.class, outputs);
    }
}
