package com.itstwoam.aoeqol.jei;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.BlankRecipeWrapper;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

/** Scaffold + construction foam -> what you mine once the foam has hardened. */
public final class FoamRecipe extends BlankRecipeWrapper {
    final ItemStack scaffold;
    final FluidStack foam;
    final List<ItemStack> outputs;

    public FoamRecipe(ItemStack scaffold, FluidStack foam, List<ItemStack> outputs) {
        this.scaffold = scaffold;
        this.foam = foam;
        this.outputs = outputs;
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        ingredients.setInputs(ItemStack.class, Collections.singletonList(scaffold));
        ingredients.setInputs(FluidStack.class, Collections.singletonList(foam));
        ingredients.setOutputs(ItemStack.class, outputs);
    }

    @Override
    public void drawInfo(net.minecraft.client.Minecraft minecraft, int width, int height, int mouseX, int mouseY) {
        // second output slot (iron fence) only for the reinforced iron scaffold
        if (outputs.size() > 1 && FoamCategory.slot != null) {
            FoamCategory.slot.draw(minecraft, FoamCategory.OUT_X + 20, FoamCategory.ROW_Y);
        }
    }

    @Override
    public List<String> getTooltipStrings(int mouseX, int mouseY) {
        if (FoamCategory.overArrow(mouseX, mouseY)) {
            return Arrays.asList(
                    "Spray the scaffold with a CF Sprayer.",
                    "§7The foam hardens by itself: about 10 min",
                    "§7in full light, much slower in the dark.",
                    "§7Right-click it with sand to harden it instantly.",
                    "§7Mined before it hardens, it drops the scaffold.");
        }
        return Collections.emptyList();
    }
}
