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

/**
 * JEI page: solid canning (e.g. empty fuel rod + uranium fuel -> uranium fuel rod). Drawn like the Fluid/Solid
 * Canning Machine in its "Bottle Solid" mode, whose class defines these recipes: its own GUI texture with the
 * pipe covers that mode draws, container top-left, filling in the centre, output top-right.
 */
final class CannerSolidCategory extends BlankRecipeCategory<CannerSolidRecipe> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("ic2", "textures/gui/GUICanner.png");
    // crop of GUICanner.png: slots, pipes and progress indicator, above the tanks and the tank-swap button
    private static final int U = 36, V = 14, WIDTH = 104, HEIGHT = 50;

    private final String title;
    private final IDrawable background;
    private final IDrawableStatic coverLeft, coverRight;
    private final IDrawableAnimated progress;

    CannerSolidCategory(IGuiHelper gui, String title) {
        this.title = title;
        background = gui.createDrawable(TEXTURE, U, V, WIDTH, HEIGHT);
        // GuiCanner (BottleSolid) covers the unused tank pipes with drawTexturedRect(59,53,9,18,3,4) and
        // (99,53,18,23,3,4); only their top part lies inside this crop
        int coverHeight = V + HEIGHT - 53;
        coverLeft = gui.createDrawable(TEXTURE, 3, 4, 9, coverHeight);
        coverRight = gui.createDrawable(TEXTURE, 3, 4, 18, coverHeight);
        // the machine's progress indicator (233,0), drawn where the GUI draws it (74,22)
        progress = gui.createAnimatedDrawable(gui.createDrawable(TEXTURE, 233, 0, 23, 14), 100,
                IDrawableAnimated.StartDirection.LEFT, false);
    }

    @Override public String getUid() { return CannerJei.UID_SOLID; }
    @Override public String getTitle() { return title; }
    @Override public IDrawable getBackground() { return background; }

    @Override
    public void drawExtras(Minecraft minecraft) {
        coverLeft.draw(minecraft, 59 - U, 53 - V);
        coverRight.draw(minecraft, 99 - U, 53 - V);
    }

    @Override
    public void drawAnimations(Minecraft minecraft) {
        progress.draw(minecraft, 74 - U, 22 - V);
    }

    @Override
    public void setRecipe(IRecipeLayout layout, CannerSolidRecipe recipe, IIngredients ingredients) {
        // slot positions from IC2's ContainerCanner (item at x,y; JEI's slot origin is 1px up-left)
        IGuiItemStackGroup items = layout.getItemStacks();
        items.init(0, true, 41 - 1 - U, 17 - 1 - V);   // container (empty fuel rod, tin can...)
        items.init(1, true, 80 - 1 - U, 44 - 1 - V);   // filling (uranium fuel, food...)
        items.init(2, false, 119 - 1 - U, 17 - 1 - V); // output
        items.set(ingredients);
    }
}
