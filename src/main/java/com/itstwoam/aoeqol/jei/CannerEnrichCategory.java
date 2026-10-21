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
import net.minecraft.util.ResourceLocation;

/**
 * JEI page: fluid enriching (e.g. distilled water + lapis -> coolant). Drawn with the Fluid/Solid Canning
 * Machine's own GUI texture, with IC2's tank frames and gauge marks where the machine draws its two tanks and
 * the additive in the centre slot: [input tank] + [additive] -> [output tank], plus the machine's cell slots:
 * [empty cell] -> [filled cell] (e.g. a Coolant Cell).
 */
final class CannerEnrichCategory extends BlankRecipeCategory<CannerEnrichRecipe> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("ic2", "textures/gui/GUICanner.png");
    private static final ResourceLocation COMMON = new ResourceLocation("ic2", "textures/gui/common.png");
    // crop of GUICanner.png: everything above the player inventory, minus the title
    private static final int U = 36, V = 14;
    // tank positions from IC2's GuiCanner (TankGauge.createNormal: 20x55 frame, fluid area inset by 4)
    private static final int TANK_IN_X = 39, TANK_OUT_X = 117, TANK_Y = 42;

    private final String title;
    private final IDrawable background;
    private final IDrawableStatic tankFrame, gaugeMarks;
    private final IDrawableAnimated progress;

    CannerEnrichCategory(IGuiHelper gui, String title) {
        this.title = title;
        background = gui.createDrawable(TEXTURE, U, V, 104, 86);
        tankFrame = gui.createDrawable(COMMON, 6, 100, 20, 55);
        gaugeMarks = gui.createDrawable(COMMON, 42, 104, 12, 47);
        // the machine's progress indicator (233,0), drawn where the GUI draws it (74,22)
        progress = gui.createAnimatedDrawable(gui.createDrawable(TEXTURE, 233, 0, 23, 14), 100,
                IDrawableAnimated.StartDirection.LEFT, false);
    }

    @Override public String getUid() { return CannerJei.UID_ENRICH; }
    @Override public String getTitle() { return title; }
    @Override public IDrawable getBackground() { return background; }

    @Override
    public void drawExtras(Minecraft minecraft) {
        tankFrame.draw(minecraft, TANK_IN_X - U, TANK_Y - V);
        tankFrame.draw(minecraft, TANK_OUT_X - U, TANK_Y - V);
    }

    @Override
    public void drawAnimations(Minecraft minecraft) {
        progress.draw(minecraft, 74 - U, 22 - V);
    }

    @Override
    public void setRecipe(IRecipeLayout layout, CannerEnrichRecipe recipe, IIngredients ingredients) {
        int capacity = Math.max(1000, Math.max(recipe.input.amount, recipe.output.amount));
        IGuiFluidStackGroup fluids = layout.getFluidStacks();
        fluids.init(0, true, TANK_IN_X + 4 - U, TANK_Y + 4 - V, 12, 47, capacity, true, gaugeMarks);
        fluids.init(1, false, TANK_OUT_X + 4 - U, TANK_Y + 4 - V, 12, 47, capacity, true, gaugeMarks);
        fluids.set(ingredients);
        // slots from IC2's ContainerCanner: additive in the centre (80,44); in this mode the left slot (41,17)
        // takes an empty cell, which the machine fills from the output tank into the right slot (119,17)
        IGuiItemStackGroup items = layout.getItemStacks();
        items.init(0, true, 80 - 1 - U, 44 - 1 - V);
        items.set(0, recipe.additives);
        if (recipe.emptyCell != null) {
            items.init(1, true, 41 - 1 - U, 17 - 1 - V);
            items.init(2, false, 119 - 1 - U, 17 - 1 - V);
            items.set(1, recipe.emptyCell);
            items.set(2, recipe.filledCell);
        }
    }
}
