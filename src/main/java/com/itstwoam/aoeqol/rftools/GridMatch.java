package com.itstwoam.aoeqol.rftools;

import gnu.trove.set.hash.TIntHashSet;
import java.lang.reflect.Method;
import java.util.List;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Replacements for RFTools' StorageCraftingTools (the crafting grid of the Storage Scanner and Modular Storage).
 * See {@link ItemMatch} for the rules.
 */
public final class GridMatch {
    private static Method getItems, decrStackSize;
    private static boolean failed;

    private GridMatch() {}

    /** Replaces StorageCraftingTools.match(target, targetIDs, input, strictDamage). */
    public static boolean match(ItemStack target, Object targetIDs, ItemStack input, boolean strictDamage) {
        if (strictDamage) return ItemMatch.strict(target, input);
        return loose(target, (TIntHashSet) targetIDs, input);
    }

    /** RFTools' own loose test: same item, any damage value, or a shared ore dictionary name. */
    static boolean loose(ItemStack target, TIntHashSet ids, ItemStack input) {
        if (target.getItem() == input.getItem()) return true;
        if (ids == null || ids.isEmpty()) return false;
        for (int id : OreDictionary.getOreIDs(input)) {
            if (ids.contains(id)) return true;
        }
        return false;
    }

    /**
     * Replaces StorageCraftingTools.findMatchingItems. Same as RFTools' version (takes items from the sources in
     * order until the slot's count is met, recording what it took for undo) except that it looks in tiers: first
     * items matching the recipe's item exactly, then the rest of what the strict test accepts, and in the loose pass
     * finally the rest of what the loose test accepts. Returns the count still missing.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static int findMatchingItems(InventoryCrafting work, List undo, int slot, ItemStack stack, int count,
                                        Object itemSource, boolean strictDamage) {
        try {
            return find(work, undo, slot, stack, count, itemSource, strictDamage);
        } catch (Throwable t) {
            if (!failed) {
                failed = true;
                System.out.println("[AoE-QoL] RFTools crafting grid search failed, please report: " + t);
                t.printStackTrace();
            }
            return count;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static int find(InventoryCrafting work, List undo, int slot, ItemStack stack, int count,
                            Object itemSource, boolean strictDamage) throws Exception {
        if (getItems == null) {
            Class<?> source = Class.forName("mcjty.rftools.craftinggrid.IItemSource", false, itemSource.getClass().getClassLoader());
            Class<?> key = Class.forName("mcjty.rftools.craftinggrid.IItemKey", false, itemSource.getClass().getClassLoader());
            decrStackSize = source.getMethod("decrStackSize", key, int.class);
            getItems = source.getMethod("getItems");
        }
        TIntHashSet ids = new TIntHashSet(OreDictionary.getOreIDs(stack));
        int tiers = strictDamage ? 2 : 3;
        for (int tier = 0; tier < tiers && count > 0; tier++) {
            for (Object o : (Iterable<?>) getItems.invoke(itemSource)) {
                Pair pair = (Pair) o;
                ItemStack input = (ItemStack) pair.getValue();
                if (ItemMatch.valid(input) && accepts(tier, stack, ids, input)) {
                    work.setInventorySlotContents(slot, input.copy());
                    int ss = count;
                    if (input.stackSize - ss < 0) ss = input.stackSize;
                    count -= ss;
                    Object key = pair.getKey();
                    ItemStack extracted = (ItemStack) decrStackSize.invoke(itemSource, key, ss);
                    undo.add(Pair.of(key, extracted));
                }
                if (count == 0) break;
            }
        }
        return count;
    }

    static boolean accepts(int tier, ItemStack target, TIntHashSet ids, ItemStack input) {
        boolean exact = ItemMatch.exact(target, input);
        if (tier == 0) return exact;
        boolean strict = ItemMatch.strict(target, input);
        if (tier == 1) return strict && !exact;
        return !strict && !exact && loose(target, ids, input);
    }
}
