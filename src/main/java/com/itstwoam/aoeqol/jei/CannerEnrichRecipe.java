package com.itstwoam.aoeqol.jei;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.BlankRecipeWrapper;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

/**
 * Canning Machine, fluid enriching mode: fluid + additive -> fluid (e.g. distilled water + lapis -> coolant).
 * In that mode the machine also fills an empty cell put in its left slot from the output tank, so the page shows
 * empty cell -> filled cell as well: that's how a Coolant Cell is made, and how JEI finds this page for one.
 * emptyCell / filledCell are null if IC2's cells can't be made for the fluid.
 */
public final class CannerEnrichRecipe extends BlankRecipeWrapper {
    final FluidStack input, output;
    final List<ItemStack> additives;
    final ItemStack emptyCell, filledCell;

    public CannerEnrichRecipe(FluidStack input, List<ItemStack> additives, FluidStack output) {
        this(input, additives, output, null, null);
    }

    public CannerEnrichRecipe(FluidStack input, List<ItemStack> additives, FluidStack output, ItemStack emptyCell, ItemStack filledCell) {
        this.input = input;
        this.additives = additives;
        this.output = output;
        boolean cells = emptyCell != null && filledCell != null;
        this.emptyCell = cells ? emptyCell : null;
        this.filledCell = cells ? filledCell : null;
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        ingredients.setInput(FluidStack.class, input);
        if (emptyCell != null) {
            ingredients.setInputLists(ItemStack.class, Arrays.asList(additives, Collections.singletonList(emptyCell)));
            ingredients.setOutputs(ItemStack.class, Collections.singletonList(filledCell));
        } else {
            ingredients.setInputLists(ItemStack.class, Collections.singletonList(additives));
        }
        ingredients.setOutput(FluidStack.class, output);
    }
}
