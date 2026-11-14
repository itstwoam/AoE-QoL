package com.itstwoam.aoeqol.calculator;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;

import com.itstwoam.aoeqol.AoEQoLConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.text.TextComponentTranslation;
import org.lwjgl.opengl.GL11;

/**
 * Calculator shows wrong odds for getting things out of circuits.
 *
 * Extraction Chamber: dirt gives a Dirty Circuit and cobblestone a Damaged Circuit when SonarCore.rand.nextInt(9)
 * hits 8, so 1 in 9 = 11.1%. Its JEI page draws a hard-coded "12.5%", and the Age of Engineering guide (Age 4)
 * repeats it.
 *
 * Analysing Chamber: a new circuit gets one number per output row, 1..50, 1..100, 1..1000, 1..2000, 1..10000,
 * 1..20000 (CircuitBoard.setData), and each item of a row is one of those numbers. The JEI page's "Extract Chance"
 * is wrong for rows 1, 2 and 4 (6.0, 0.2 and 0.02 % instead of 2, 1 and 0.05 %).
 *
 * Display only; nothing about the machines changes.
 */
public final class ChanceText {
    private ChanceText() {}

    static final String GUIDE_WRONG = "circuits with a chance of 12.5% from Dirt or Cobblestone";
    static final String GUIDE_RIGHT = "circuits with a chance of 11.1% from Dirt or Cobblestone";

    /** Number range per Analysing Chamber output row (CircuitBoard.setData: 1 + random * N). */
    static final int[] ROW_RANGE = {50, 100, 1000, 2000, 10000, 20000};

    /** End of FTB Utilities' Guides.replaceSubstitutes: the guide page text as read from the pack. */
    public static String guide(String text) {
        if (text != null && text.indexOf("12.5%") >= 0) return text.replace(GUIDE_WRONG, GUIDE_RIGHT);
        return text;
    }

    /** Redirected FontRenderer.drawString in Calculator's JEI Extraction page. */
    public static int extractionText(FontRenderer font, String text, int x, int y, int colour) {
        if ("12.5%".equals(text)) text = "11.1%";
        return font.drawString(text, x, y, colour);
    }

    /** "2", "1", "0.1", "0.05", "0.01", "0.005" for rows 1..6; null for anything else. */
    static String rowPercent(int row) {
        if (row < 1 || row > ROW_RANGE.length) return null;
        return BigDecimal.valueOf(100).divide(BigDecimal.valueOf(ROW_RANGE[row - 1])).stripTrailingZeros().toPlainString();
    }

    private static Field recipeField;
    private static Method inputs, stackSize;

    /**
     * Replaces Calculator's JEI Analysing page drawInfo (only while AoEQoLConfig.OK_CALC_ANALYSING_JEI is true,
     * which is cleared if SonarCore's recipe classes can't be read, so Calculator's own text comes back).
     */
    public static void analysingInfo(Object wrapper, Minecraft mc, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
        int row;
        try {
            if (recipeField == null) {
                Class<?> v2 = Class.forName("sonar.core.integration.jei.JEIRecipeV2");
                recipeField = v2.getField("recipe");
                inputs = Class.forName("sonar.core.recipes.ISonarRecipe").getMethod("inputs");
                stackSize = Class.forName("sonar.core.recipes.ISonarRecipeObject").getMethod("getStackSize");
            }
            Object recipe = recipeField.get(wrapper);
            Object first = ((List<?>) inputs.invoke(recipe)).get(0);
            row = (Integer) stackSize.invoke(first);
        } catch (Throwable t) {
            AoEQoLConfig.OK_CALC_ANALYSING_JEI = false;
            System.out.println("[AoE-QoL] Calculator Analysing Chamber chance fix disabled: " + t);
            return;
        }
        String pct = rowPercent(row);
        if (pct == null) pct = "0";
        String text = new TextComponentTranslation("info.extractChance").getFormattedText() + " = " + pct + " %";
        FontRenderer font = mc.fontRendererObj;
        GL11.glPushMatrix();
        GL11.glScaled(0.7, 0.7, 0.7);
        int xSize = (int) (recipeWidth * 1.4285714285714286);   // same placement as Calculator's own text
        font.drawString(text, xSize / 2 - font.getStringWidth(text) / 2, 40, 0x404040);
        GL11.glPopMatrix();
    }
}
