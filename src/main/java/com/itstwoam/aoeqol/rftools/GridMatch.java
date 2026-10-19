package com.itstwoam.aoeqol.rftools;

import com.itstwoam.aoeqol.jec.NbtCompat;
import gnu.trove.set.hash.TIntHashSet;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.oredict.OreDictionary;

/**
 * RFTools' crafting grid (Storage Scanner, Modular Storage) finds ingredients in two passes. The first, "strict"
 * pass needs the same item AND the same damage value as the recipe shows. IC2 batteries and energy crystals keep
 * their charge in the damage value, so a charged crystal never matches the one in the grid. The second pass
 * then accepts ANY item of the same kind, ignoring its variant, which for IC2 picks the wrong cable or machine
 * casing, the recipe check fails, and the grid reports the crystals as missing.
 *
 * Fix: in the strict pass, a damage difference is accepted for items whose damage value is only wear or charge:
 * the item can take damage (so it has no variants) and is on the ignored-damage list, the same rule and list
 * (ListIgnoredDamage, default ic2:*) the JEC fix uses. Without JEC installed the default list applies.
 * Everything else behaves exactly like RFTools' own StorageCraftingTools.match, which this replaces.
 */
public final class GridMatch {
    private static boolean jecChecked, jec, warned;

    private GridMatch() {}

    public static boolean match(ItemStack target, Object targetIDs, ItemStack input, boolean strictDamage) {
        if (strictDamage) {
            if (target.getItem() != input.getItem()) return false;
            int meta = target.getMetadata();
            return meta == Short.MAX_VALUE || meta == input.getMetadata() || isConditionDamage(target);
        }
        // RFTools' own loose pass, unchanged
        if (target.getItem() == input.getItem()) return true;
        TIntHashSet ids = (TIntHashSet) targetIDs;
        if (ids == null || ids.isEmpty()) return false;
        for (int id : OreDictionary.getOreIDs(input)) {
            if (ids.contains(id)) return true;
        }
        return false;
    }

    /** True if this item's damage value only stores wear or charge, per the ignored-damage rules. */
    static boolean isConditionDamage(ItemStack stack) {
        Item item = stack.getItem();
        if (item == null || !item.isDamageable()) return false;
        if (!jecChecked) {
            jec = Loader.isModLoaded("je_calculation");
            jecChecked = true;
        }
        if (jec) {
            try {
                return NbtCompat.isConditionDamage(stack);
            } catch (Throwable t) {
                if (!warned) {
                    warned = true;
                    System.out.println("[AoE-QoL] Using default ignored-damage list for RFTools: " + t);
                }
            }
        }
        ResourceLocation rl = item.getRegistryName();
        return rl != null && "ic2".equals(rl.getResourceDomain());
    }
}
