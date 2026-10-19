package com.itstwoam.aoeqol.jei;

import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IDrawableAnimated;
import mezz.jei.api.gui.IDrawableStatic;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.BlankRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

/** JEI page: Canning Machine, solid canning. Layout: [container] + [filling] -> [output]. */
final class CannerSolidCategory extends BlankRecipeCategory<CannerSolidRecipe> {
    static final ResourceLocation FURNACE = new ResourceLocation("minecraft", "textures/gui/container/furnace.png");
    private final IDrawable background, slot;
    private final IDrawableAnimated arrow;

    CannerSolidCategory(IGuiHelper gui) {
        background = gui.createBlankDrawable(110, 26);
        slot = gui.getSlotDrawable();
        IDrawableStatic arrowFull = gui.createDrawable(FURNACE, 176, 14, 24, 17);
        arrow = gui.createAnimatedDrawable(arrowFull, 100, IDrawableAnimated.StartDirection.LEFT, false);
    }

    @Override public String getUid() { return CannerJei.UID_SOLID; }
    @Override public String getTitle() { return "Canning Machine"; }
    @Override public IDrawable getBackground() { return background; }

    @Override
    public void drawExtras(Minecraft minecraft) {
        slot.draw(minecraft, 0, 4);
        slot.draw(minecraft, 26, 4);
        slot.draw(minecraft, 86, 4);
        minecraft.fontRendererObj.drawString("+", 20, 9, 0x404040);
    }

    @Override
    public void drawAnimations(Minecraft minecraft) {
        arrow.draw(minecraft, 54, 5);
    }

    @Override
    public void setRecipe(IRecipeLayout layout, CannerSolidRecipe recipe, IIngredients ingredients) {
        IGuiItemStackGroup items = layout.getItemStacks();
        items.init(0, true, 0, 4);
        items.init(1, true, 26, 4);
        items.init(2, false, 86, 4);
        items.set(ingredients);
    }
}
