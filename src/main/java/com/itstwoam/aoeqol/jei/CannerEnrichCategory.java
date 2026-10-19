package com.itstwoam.aoeqol.jei;

import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IDrawableAnimated;
import mezz.jei.api.gui.IDrawableStatic;
import mezz.jei.api.gui.IGuiFluidStackGroup;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.BlankRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;

/** JEI page: Canning Machine, fluid enriching. Layout: [fluid] + [additive] -> [fluid]. */
final class CannerEnrichCategory extends BlankRecipeCategory<CannerEnrichRecipe> {
    private final IDrawable background, slot;
    private final IDrawableAnimated arrow;

    CannerEnrichCategory(IGuiHelper gui) {
        background = gui.createBlankDrawable(110, 34);
        slot = gui.getSlotDrawable();
        IDrawableStatic arrowFull = gui.createDrawable(CannerSolidCategory.FURNACE, 176, 14, 24, 17);
        arrow = gui.createAnimatedDrawable(arrowFull, 100, IDrawableAnimated.StartDirection.LEFT, false);
    }

    @Override public String getUid() { return CannerJei.UID_ENRICH; }
    @Override public String getTitle() { return "Canning Machine (Fluid Enriching)"; }
    @Override public IDrawable getBackground() { return background; }

    @Override
    public void drawExtras(Minecraft minecraft) {
        tankFrame(0, 0);
        slot.draw(minecraft, 26, 8);
        tankFrame(86, 0);
        minecraft.fontRendererObj.drawString("+", 20, 13, 0x404040);
    }

    private static void tankFrame(int x, int y) {
        Gui.drawRect(x, y, x + 18, y + 34, 0xFF373737);
        Gui.drawRect(x + 1, y + 1, x + 17, y + 33, 0xFF8B8B8B);
    }

    @Override
    public void drawAnimations(Minecraft minecraft) {
        arrow.draw(minecraft, 54, 9);
    }

    @Override
    public void setRecipe(IRecipeLayout layout, CannerEnrichRecipe recipe, IIngredients ingredients) {
        int capacity = Math.max(1000, Math.max(recipe.input.amount, recipe.output.amount));
        IGuiFluidStackGroup fluids = layout.getFluidStacks();
        fluids.init(0, true, 1, 1, 16, 32, capacity, true, null);
        fluids.init(1, false, 87, 1, 16, 32, capacity, true, null);
        fluids.set(ingredients);
        IGuiItemStackGroup items = layout.getItemStacks();
        items.init(0, true, 26, 8);
        items.set(ingredients);
    }
}
