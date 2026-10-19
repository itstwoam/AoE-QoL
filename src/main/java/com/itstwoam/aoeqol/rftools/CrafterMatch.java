package com.itstwoam.aoeqol.rftools;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * Replacements for RFTools' Crafter block (CrafterBaseTE), which has its own copy of the two-pass search over its
 * 26 buffer slots. See {@link ItemMatch} for the rules.
 */
public final class CrafterMatch {
    private static Method isKeepOne, getInventory, getCachedRecipe, getInventoryHelper, getStack, setStack;
    private static Field workInventory;
    private static boolean failed;

    private CrafterMatch() {}

    /** Replaces CrafterBaseTE.match(target, input, strictDamage). */
    public static boolean match(ItemStack target, ItemStack input, boolean strictDamage) {
        if (strictDamage) return ItemMatch.strict(target, input);
        return looseCrafter(target, input);
    }

    /** RFTools' own loose test for the Crafter: same item, any damage value. */
    static boolean looseCrafter(ItemStack target, ItemStack input) {
        boolean t = ItemMatch.valid(target), i = ItemMatch.valid(input);
        if (t != i) return false;
        return target.getItem() == input.getItem();
    }

    /**
     * Replaces the ghost-slot check in CrafterBaseTE.isItemValidForSlot (ghost.isItemEqual(stack)): a charged
     * battery or worn tool is accepted in a slot set to that item.
     */
    public static boolean ghostAccepts(ItemStack ghost, ItemStack stack) {
        if (ghost.isItemEqual(stack)) return true;
        Item item = ghost.getItem();
        return stack != null && item != null && item == stack.getItem() && item.isDamageable();
    }

    /**
     * Replaces CrafterBaseTE.testAndConsumeCraftingItems(recipe, undo, strictDamage): RFTools' code, with the
     * search over the buffer done in tiers (exact items first) like the crafting grid's.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static boolean testAndConsumeCraftingItems(Object crafter, Object craftingRecipe, Map undo, boolean strictDamage) {
        try {
            return test(crafter, craftingRecipe, undo, strictDamage);
        } catch (Throwable t) {
            if (!failed) {
                failed = true;
                System.out.println("[AoE-QoL] RFTools Crafter search failed, please report: " + t);
                t.printStackTrace();
            }
            return false;   // RFTools then puts back whatever was taken (undo) and crafts nothing
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean test(Object crafter, Object craftingRecipe, Map undo, boolean strictDamage) throws Exception {
        if (setStack == null) {
            Class<?> r = craftingRecipe.getClass();
            isKeepOne = r.getMethod("isKeepOne");
            getInventory = r.getMethod("getInventory");
            getCachedRecipe = r.getMethod("getCachedRecipe", World.class);
            getInventoryHelper = crafter.getClass().getMethod("getInventoryHelper");
            Field f = findField(crafter.getClass(), "workInventory");
            f.setAccessible(true);
            workInventory = f;
            Class<?> h = getInventoryHelper.getReturnType();
            getStack = h.getMethod("getStackInSlot", int.class);
            setStack = h.getMethod("setStackInSlot", int.class, ItemStack.class);
        }
        World world = ((TileEntity) crafter).getWorld();
        Object helper = getInventoryHelper.invoke(crafter);
        InventoryCrafting work = (InventoryCrafting) workInventory.get(crafter);
        int keep = (Boolean) isKeepOne.invoke(craftingRecipe) ? 1 : 0;
        InventoryCrafting inventory = (InventoryCrafting) getInventory.invoke(craftingRecipe);
        int tiers = strictDamage ? 2 : 3;
        for (int i = 0; i < inventory.getSizeInventory(); ++i) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!ItemMatch.valid(stack)) {
                work.setInventorySlotContents(i, null);
                continue;
            }
            int count = stack.stackSize;
            for (int tier = 0; tier < tiers && count > 0; tier++) {
                for (int j = 0; j < 26; ++j) {
                    int slotIdx = 10 + j;
                    ItemStack input = (ItemStack) getStack.invoke(helper, slotIdx);
                    if (ItemMatch.valid(input) && input.stackSize > keep && accepts(tier, stack, input)) {
                        work.setInventorySlotContents(i, input.copy());
                        int ss = count;
                        if (input.stackSize - ss < keep) ss = input.stackSize - keep;
                        count -= ss;
                        if (!undo.containsKey(slotIdx)) undo.put(slotIdx, input.copy());
                        input.splitStack(ss);
                        if (!ItemMatch.valid(input)) setStack.invoke(helper, slotIdx, null);
                    }
                    if (count == 0) break;
                }
            }
            if (count > 0) return false;
        }
        IRecipe recipe = (IRecipe) getCachedRecipe.invoke(craftingRecipe, world);
        return recipe.matches(work, world);
    }

    static boolean accepts(int tier, ItemStack target, ItemStack input) {
        boolean exact = ItemMatch.exact(target, input);
        if (tier == 0) return exact;
        boolean strict = ItemMatch.strict(target, input);
        if (tier == 1) return strict && !exact;
        return !strict && !exact && looseCrafter(target, input);
    }

    private static Field findField(Class<?> c, String name) throws NoSuchFieldException {
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            try {
                return k.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new NoSuchFieldException(name);
    }
}
