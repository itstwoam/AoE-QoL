package com.itstwoam.aoeqol.rftools;

import gnu.trove.set.hash.TIntHashSet;
import java.lang.reflect.Method;
import java.util.List;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;
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
    /** Grid slots filled with a different NBT variant than the recipe shows, during the current test craft. */
    private static final boolean[] variantSlots = new boolean[9];

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
     * then ore dictionary equivalents and finally other variants of the same item. Returns the count still missing.
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
        int tiers = strictDamage ? 2 : 4;
        for (int tier = 0; tier < tiers && count > 0; tier++) {
            for (Object o : (Iterable<?>) getItems.invoke(itemSource)) {
                Pair pair = (Pair) o;
                ItemStack input = (ItemStack) pair.getValue();
                if (ItemMatch.valid(input) && accepts(tier, stack, ids, input)) {
                    if (tier > 0 && slot >= 0 && slot < 9 && ItemMatch.variantDiffers(stack, input)) variantSlots[slot] = true;
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

    /** Start of StorageCraftingTools.tryRecipe (a test craft). */
    public static void beginTest() {
        java.util.Arrays.fill(variantSlots, false);
        if (!com.itstwoam.aoeqol.AoEQoLConfig.ON_RFTOOLS_MATCHING) return;
    }

    /**
     * End of StorageCraftingTools.tryRecipe. RFTools reports per slot how many items are missing, plus slot 9 =
     * "the recipe doesn't accept what was found", and marks those red. Slots filled with a different NBT variant
     * than the recipe shows (e.g. another kind of IC2 cable, which counts as the same item and damage value) look
     * found, so only the result (or only the other missing items) turned red.
     *
     * Nothing missing but the recipe refuses: the variant slots are marked, which is almost always the reason.
     * Something missing as well: the recipe is asked with the missing slots filled from the recipe's own items.
     * If it still refuses, but accepts once the variant slots get the recipe's items too, the variants are wrong
     * and are marked as well; if it accepts the variants, they stay unmarked.
     */
    public static void afterTest(int[] missing, InventoryCrafting work, InventoryCrafting shown, IRecipe recipe,
                                 EntityPlayerMP player) {
        if (!com.itstwoam.aoeqol.AoEQoLConfig.ON_RFTOOLS_MATCHING) return;
        if (missing == null || missing.length < 10 || missing[9] <= 0) return;
        boolean anyVariant = false, anyMissing = false;
        for (int i = 0; i < 9; i++) {
            anyVariant |= variantSlots[i];
            anyMissing |= missing[i] > 0;
        }
        if (!anyVariant) return;
        if (anyMissing) {
            try {
                if (work == null || shown == null || recipe == null || player == null) return;
                World world = player.getEntityWorld();
                InventoryCrafting test = copy(work);
                for (int i = 0; i < 9; i++) if (missing[i] > 0) test.setInventorySlotContents(i, shownCopy(shown, i));
                if (recipe.matches(test, world)) return;            // the variants would do
                for (int i = 0; i < 9; i++) if (variantSlots[i]) test.setInventorySlotContents(i, shownCopy(shown, i));
                if (!recipe.matches(test, world)) return;           // something else is wrong; can't tell what
            } catch (Throwable t) {
                return;
            }
        }
        for (int i = 0; i < 9; i++) if (variantSlots[i] && missing[i] <= 0) missing[i] = 1;
    }

    private static InventoryCrafting copy(InventoryCrafting from) {
        InventoryCrafting to = new InventoryCrafting(new NoContainer(), 3, 3);
        for (int i = 0; i < 9 && i < from.getSizeInventory(); i++) {
            ItemStack s = from.getStackInSlot(i);
            to.setInventorySlotContents(i, s == null ? null : s.copy());
        }
        return to;
    }

    private static ItemStack shownCopy(InventoryCrafting shown, int slot) {
        if (slot >= shown.getSizeInventory()) return null;
        ItemStack s = shown.getStackInSlot(slot);
        return s == null ? null : s.copy();
    }

    private static final class NoContainer extends Container {
        @Override
        public boolean canInteractWith(EntityPlayer player) {
            return false;
        }
    }

    /**
     * Tier 0: exact (same variant and NBT). 1: the rest of the strict test. Loose pass only: 2: a different item
     * sharing an ore dictionary name (IE iron plate for IC2's), 3: RFTools' last resort, the same item in another
     * variant (IC2 copper plate for IC2 iron plate), which the recipe usually rejects.
     */
    static boolean accepts(int tier, ItemStack target, TIntHashSet ids, ItemStack input) {
        boolean exact = ItemMatch.exact(target, input);
        if (tier == 0) return exact;
        boolean strict = ItemMatch.strict(target, input);
        if (tier == 1) return strict && !exact;
        if (exact || strict) return false;
        boolean ore = sharesOre(ids, input);
        if (tier == 2) return ore;
        return !ore && target.getItem() == input.getItem();
    }

    static boolean sharesOre(TIntHashSet ids, ItemStack input) {
        if (ids == null || ids.isEmpty()) return false;
        for (int id : OreDictionary.getOreIDs(input)) {
            if (ids.contains(id)) return true;
        }
        return false;
    }
}
