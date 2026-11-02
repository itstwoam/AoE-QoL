package com.itstwoam.aoeqol.jec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import pers.towdium.just_enough_calculation.core.Recipe;
import pers.towdium.just_enough_calculation.util.helpers.PlayerRecordHelper;

/**
 * JEC's calculator step list can ask for an item before the items it's made from. JEC plans from the top down
 * and shows that plan reversed, which only works if every ingredient is planned after the item that uses it. A
 * recipe that makes a batch (Iron Tubing: 16 per craft) breaks that: the batch planned for the first item that needed
 * it also covers items planned later (a Pressure Valve needing 3), so nothing moves the tubing behind the valve and,
 * reversed, the valve comes first.
 *
 * Fix: the finished step list is re-sorted so that every step comes after the steps that make its ingredients.
 * Otherwise JEC's order is kept: each time, the first remaining step whose ingredients are all done is taken. Each
 * step's ingredients come from the recipe JEC uses for it (the first saved recipe for that item); the outputs of one
 * multi-output recipe stay together. Steps that depend on each other in a loop keep JEC's order. Amounts are never
 * changed.
 *
 * Injected at the end of JEC's Calculator.getProcedure(). The list is worked out once per calculation and cached,
 * since the calculator screen asks for it every frame.
 */
public final class StepOrder {
    private static final Map<Object, List<ItemStack>> CACHE = new WeakHashMap<Object, List<ItemStack>>();
    private static boolean broken;

    private StepOrder() {}

    public static List<ItemStack> sort(List<ItemStack> steps, Object calculator) {
        if (broken || steps == null || steps.size() < 2 || calculator == null) return steps;
        try {
            List<ItemStack> sorted;
            synchronized (CACHE) {
                sorted = CACHE.get(calculator);
            }
            if (sorted == null || sorted.size() != steps.size()) {
                sorted = order(steps);
                synchronized (CACHE) {
                    CACHE.put(calculator, sorted);
                }
            }
            List<ItemStack> copy = new ArrayList<ItemStack>(sorted.size());
            for (ItemStack s : sorted) copy.add(s.copy());
            return copy;
        } catch (Throwable t) {
            broken = true;
            System.out.println("[AoE-QoL] Calculator step order fix disabled: " + t);
            return steps;
        }
    }

    /** One step, or the outputs of one multi-output recipe, kept together. */
    private static final class Unit {
        final List<ItemStack> stacks = new ArrayList<ItemStack>();
        Recipe recipe;
    }

    static List<ItemStack> order(List<ItemStack> steps) {
        // Group the steps by the recipe JEC uses for them.
        List<Unit> units = new ArrayList<Unit>();
        for (ItemStack step : steps) {
            Recipe recipe = recipeFor(step);
            Unit unit = null;
            if (recipe != null) {
                for (Unit u : units) {
                    if (u.recipe == recipe) {
                        unit = u;
                        break;
                    }
                }
            }
            if (unit == null) {
                unit = new Unit();
                unit.recipe = recipe;
                units.add(unit);
            }
            unit.stacks.add(step);
        }
        // depends[i][j]: unit i uses something unit j makes.
        int n = units.size();
        boolean[][] depends = new boolean[n][n];
        for (int i = 0; i < n; i++) {
            Recipe r = units.get(i).recipe;
            if (r == null) continue;
            for (ItemStack input : r.getInput()) {
                if (input == null) continue;
                for (int j = 0; j < n; j++) {
                    if (j == i || depends[i][j]) continue;
                    for (ItemStack made : units.get(j).stacks) {
                        if (same(input, made)) {
                            depends[i][j] = true;
                            break;
                        }
                    }
                }
            }
        }
        List<ItemStack> out = new ArrayList<ItemStack>(steps.size());
        boolean[] placed = new boolean[n];
        for (int done = 0; done < n; done++) {
            int pick = -1;
            for (int i = 0; i < n && pick < 0; i++) {
                if (placed[i]) continue;
                boolean ready = true;
                for (int j = 0; j < n && ready; j++) if (depends[i][j] && !placed[j]) ready = false;
                if (ready) pick = i;
            }
            if (pick < 0) {   // a loop: keep JEC's order
                for (int i = 0; i < n; i++) {
                    if (!placed[i]) {
                        pick = i;
                        break;
                    }
                }
            }
            placed[pick] = true;
            out.addAll(units.get(pick).stacks);
        }
        return Collections.unmodifiableList(out);
    }

    private static Recipe recipeFor(ItemStack step) {
        List<Recipe> recipes = PlayerRecordHelper.getAllRecipeOutput(step);
        return recipes == null || recipes.isEmpty() ? null : recipes.get(0);
    }

    /** Same item and variant, ignoring amounts and the condition tags JEC is told to ignore. Records nothing. */
    static boolean same(ItemStack a, ItemStack b) {
        if (a.getItem() != b.getItem()) return false;
        if (a.getMetadata() != b.getMetadata() && !(a.getItem() != null && a.getItem().isDamageable())) return false;
        NBTTagCompound ta = NbtCompat.strippedTag(a), tb = NbtCompat.strippedTag(b);
        ta.removeTag("je_calculation");
        tb.removeTag("je_calculation");
        return ta.equals(tb);
    }
}
