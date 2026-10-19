package com.itstwoam.aoeqol.jec;

import java.lang.reflect.Field;
import java.util.List;
import net.minecraft.item.ItemStack;
import pers.towdium.just_enough_calculation.core.Recipe;
import pers.towdium.just_enough_calculation.util.helpers.ItemStackHelper;

/**
 * JEC's calculator step list ("procedure") labels every step with the item it was run for. For a recipe with
 * several outputs (e.g. a Thermal Centrifuge making U-238 and tiny U-235) that splits one machine's runs over
 * several lines, and the second line looks like extra crafting ("2 tiny U-235").
 *
 * Fix: a step that uses a multi-output recipe is listed under the recipe's FIRST output slot, with the amount
 * that slot produces over all runs, so later runs of the same recipe merge into one line. The order of the
 * outputs in a saved recipe decides the label.
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
            ItemStack first = null;
            int outputs = 0;
            for (ItemStack s : recipe.getOutput()) {
                if (s == null) continue;
                if (first == null) first = s;
                outputs++;
            }
            if (outputs < 2 || first == null || ItemStackHelper.isItemEqual(first, dest)) return;
            ItemStack label = ItemStackHelper.NBT.setData(first.copy(), ItemStackHelper.NBT.getType(first),
                    ItemStackHelper.NBT.getAmount(first) * runs);
            procedure.set(0, label);
        } catch (Throwable t) {
            broken = true;
            System.out.println("[AoE-QoL] Calculator step-list fix disabled: " + t);
        }
    }
}
