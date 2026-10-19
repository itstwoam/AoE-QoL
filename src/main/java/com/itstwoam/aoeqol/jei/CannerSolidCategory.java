package com.itstwoam.aoeqol.jei;

import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IDrawableAnimated;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.BlankRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

/**
 * JEI page: solid canning (e.g. uranium fuel + empty fuel rod -> fuel rod). Drawn with the Solid Canning
 * Machine's own GUI texture, cropped to its slot row, with the slots at the same positions as in the machine:
 * [filling] > [container] -> [output].
 */
final class CannerSolidCategory extends BlankRecipeCategory<CannerSolidRecipe> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("ic2", "textures/gui/GUISolidCanner.png");
    // crop of GUISolidCanner.png: the slot row, with a 4px margin
    private static final int U = 32, V = 31;

    private final String title;
    private final IDrawable background;
    private final IDrawableAnimated arrow;

    CannerSolidCategory(IGuiHelper gui, String title) {
        this.title = title;
        background = gui.createDrawable(TEXTURE, U, V, 106, 26);
        // the machine's progress arrow (filled version at 176,14), drawn where the GUI draws it (88,35)
        arrow = gui.createAnimatedDrawable(gui.createDrawable(TEXTURE, 176, 14, 25, 16), 100,
                IDrawableAnimated.StartDirection.LEFT, false);
    }

    @Override public String getUid() { return CannerJei.UID_SOLID; }
    @Override public String getTitle() { return title; }
    @Override public IDrawable getBackground() { return background; }

    @Override
    public void drawAnimations(Minecraft minecraft) {
        arrow.draw(minecraft, 88 - U, 35 - V);
    }

    @Override
    public void setRecipe(IRecipeLayout layout, CannerSolidRecipe recipe, IIngredients ingredients) {
        // slot positions from IC2's ContainerSolidCanner (item at x,y; JEI's slot origin is 1px up-left)
        IGuiItemStackGroup items = layout.getItemStacks();
        items.init(0, true, 67 - 1 - U, 36 - 1 - V);   // container (empty fuel rod, tin can...)
        items.init(1, true, 37 - 1 - U, 36 - 1 - V);   // filling (uranium fuel, food...)
        items.init(2, false, 116 - 1 - U, 36 - 1 - V); // output
        items.set(ingredients);
    }
}
