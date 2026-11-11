package com.itstwoam.aoeqol.rftools;

import java.util.List;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;

/** DEBUG BUILD ONLY: logs what the RFTools crafting grid does on a craft button press. */
public final class GridDebug {
    static volatile boolean crafting;
    private GridDebug() {}

    static void log(String s) {
        System.out.println("[AoE-QoL grid debug] " + s);
    }

    static String d(ItemStack s) {
        if (s == null) return "-";
        return s.stackSize + "x " + s.getItem().getRegistryName() + "@" + s.getMetadata() + (s.getTagCompound() == null ? "" : s.getTagCompound());
    }

    public static void craftStart(int n, Object recipe) {
        crafting = true;
        String result = "?";
        try {
            ItemStack r = (ItemStack) recipe.getClass().getMethod("getResult").invoke(recipe);
            result = d(r) + " max " + (r == null ? 0 : r.getMaxStackSize());
        } catch (Throwable t) {
            result = t.toString();
        }
        log("craft button n=" + n + " result " + result + " smarterMatching=" + com.itstwoam.aoeqol.AoEQoLConfig.ON_RFTOOLS_MATCHING);
    }

    public static void craftEnd() {
        log("craft button done");
        crafting = false;
    }

    @SuppressWarnings("rawtypes")
    public static List consumed(List result, boolean strict) {
        if (crafting) log("  attempt (" + (strict ? "strict" : "loose") + ") -> " + (result == null || result.isEmpty() ? "FAILED" : "crafted " + result.size() + " stack(s): " + d((ItemStack) result.get(0))));
        return result;
    }

    public static boolean matches(IRecipe recipe, InventoryCrafting inv, World world) {
        boolean ok = recipe.matches(inv, world);
        if (crafting) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < inv.getSizeInventory(); i++) if (inv.getStackInSlot(i) != null) sb.append(" [").append(i).append("] ").append(d(inv.getStackInSlot(i)));
            log("    recipe " + recipe.getClass().getSimpleName() + (ok ? " accepts" : " REFUSES") + sb);
        }
        return ok;
    }

    static void pick(boolean strict, int slot, ItemStack want, int tier, Object key, ItemStack got, int took, int left) {
        if (!crafting) return;
        String k = String.valueOf(key);
        try {
            java.lang.reflect.Method gi = key.getClass().getMethod("getInventory"), gs = key.getClass().getMethod("getSlot");
            gi.setAccessible(true); gs.setAccessible(true);
            Object inv = gi.invoke(key);
            k = inv.getClass().getSimpleName() + "#" + System.identityHashCode(inv) + " slot " + gs.invoke(key);
        } catch (Throwable t) { }
        log("    slot " + slot + " wants " + d(want) + ": tier " + tier + " took " + took + " of " + d(got) + " from " + k + ", missing " + left);
    }

    static void missing(boolean strict, int slot, ItemStack want, int left) {
        if (crafting && left > 0) log("    slot " + slot + " wants " + d(want) + ": NOT FOUND, missing " + left);
    }
}
