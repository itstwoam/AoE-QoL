package com.itstwoam.aoeqol.jec;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.ItemStack;
import pers.towdium.just_enough_calculation.core.Recipe;
import pers.towdium.just_enough_calculation.util.helpers.ItemStackHelper;

/**
 * JEC's calculator step list ("procedure") labels every step with the one item it was run for. For a recipe with
 * several outputs (e.g. a Thermal Centrifuge making tiny U-235 and U-238) only that item is shown, so the other
 * outputs the same machine runs make never appear, even when a later step uses them.
 *
 * Fix: a step that uses a multi-output recipe lists EVERY output of the recipe, each with the amount it produces
 * over all runs (JEC merges repeated runs of the same recipe into one line per item).
 *
 * Injected at the end of JEC's Calculator.CostList(Recipe, ItemStack, long) constructor. CostList and its
 * "procedure" field are package-private in JEC, so they are reached through reflection.
 */
public final class ProcedureFix {
    private static Field procedureField;
    private static boolean broken;

    private ProcedureFix() {}

    @SuppressWarnings("unchecked")
    public static void fix(Object step, Recipe recipe, ItemStack dest, long runs) {
        if (broken || step == null || recipe == null || dest == null) return;
        try {
            if (procedureField == null) {
                procedureField = step.getClass().getDeclaredField("procedure");
                procedureField.setAccessible(true);
            }
            List<ItemStack> procedure = (List<ItemStack>) procedureField.get(step);
            if (procedure == null || procedure.size() != 1) return;
            List<ItemStack> outputs = new ArrayList<ItemStack>();
            for (ItemStack s : recipe.getOutput()) if (s != null) outputs.add(s);
            if (outputs.size() < 2) return;
            procedure.clear();
            // JEC shows the procedure list in reverse, so add the outputs back to front to keep the recipe's order
            for (int i = outputs.size() - 1; i >= 0; i--) {
                ItemStack s = outputs.get(i);
                procedure.add(ItemStackHelper.NBT.setData(s.copy(), ItemStackHelper.NBT.getType(s),
                        ItemStackHelper.NBT.getAmount(s) * runs));
            }
        } catch (Throwable t) {
            broken = true;
            System.out.println("[AoE-QoL] Calculator step-list fix disabled: " + t);
        }
    }
}
