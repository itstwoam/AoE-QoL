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
 * JEI page: [scaffold] + [construction foam] -> [reinforced stone (+ iron fence)]. There is no machine for this,
 * so it uses plain slots and the vanilla furnace arrow, with the steps written underneath and the hardening
 * details in the arrow's tooltip.
 */
final class FoamCategory extends BlankRecipeCategory<FoamRecipe> {
    private static final ResourceLocation FURNACE = new ResourceLocation("minecraft", "textures/gui/container/furnace.png");
    static final int WIDTH = 160, HEIGHT = 46;
    static final int SCAFFOLD_X = 8, FOAM_X = 36, ARROW_X = 62, ARROW_Y = 1, OUT_X = 94, ROW_Y = 1;

    private final String title;
    private final IDrawable background;
    private final IDrawableStatic arrow;
    static IDrawableStatic slot;
    private final IDrawableAnimated arrowFilled;

    FoamCategory(IGuiHelper gui, String title) {
        this.title = title;
        background = gui.createBlankDrawable(WIDTH, HEIGHT);
        slot = gui.getSlotDrawable();
        arrow = gui.createDrawable(FURNACE, 79, 34, 24, 17);
        arrowFilled = gui.createAnimatedDrawable(gui.createDrawable(FURNACE, 176, 14, 24, 17), 120,
                IDrawableAnimated.StartDirection.LEFT, false);
    }

    static boolean overArrow(int x, int y) {
        return x >= ARROW_X && x < ARROW_X + 24 && y >= ARROW_Y && y < ARROW_Y + 17;
    }

    @Override public String getUid() { return FoamJei.UID; }
    @Override public String getTitle() { return title; }
    @Override public IDrawable getBackground() { return background; }

    @Override
    public void drawExtras(Minecraft minecraft) {
        slot.draw(minecraft, SCAFFOLD_X, ROW_Y);
        slot.draw(minecraft, FOAM_X, ROW_Y);
        slot.draw(minecraft, OUT_X, ROW_Y);
        minecraft.fontRendererObj.drawString("+", SCAFFOLD_X + 21, ROW_Y + 5, 0x404040);
        arrow.draw(minecraft, ARROW_X, ARROW_Y);
        minecraft.fontRendererObj.drawString("Spray, let it harden,", 2, 24, 0x404040);
        minecraft.fontRendererObj.drawString("then mine it (sand: instant)", 2, 35, 0x404040);
    }

    @Override
    public void drawAnimations(Minecraft minecraft) {
        arrowFilled.draw(minecraft, ARROW_X, ARROW_Y);
    }

    @Override
    public void setRecipe(IRecipeLayout layout, FoamRecipe recipe, IIngredients ingredients) {
        IGuiItemStackGroup items = layout.getItemStacks();
        items.init(0, true, SCAFFOLD_X, ROW_Y);
        items.init(1, false, OUT_X, ROW_Y);
        items.init(2, false, OUT_X + 20, ROW_Y);
        items.set(0, recipe.scaffold);
        items.set(1, recipe.outputs.get(0));
        if (recipe.outputs.size() > 1) items.set(2, recipe.outputs.get(1));
        IGuiFluidStackGroup fluids = layout.getFluidStacks();
        fluids.init(0, true, FOAM_X + 1, ROW_Y + 1, 16, 16, FoamJei.FOAM_PER_BLOCK, false, null);
        fluids.set(0, recipe.foam);
    }
}
