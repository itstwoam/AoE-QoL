package com.itstwoam.aoeqol.rftools;

import com.itstwoam.aoeqol.jec.NbtCompat;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.oredict.OreDictionary;

/**
 * Item comparison rules shared by the RFTools crafting fixes (crafting grid and Crafter).
 *
 * RFTools looks for each ingredient with a "strict" test (same item and same damage value, NBT never looked at)
 * and, if that leaves something missing, a "loose" one (same item, any damage value). Two kinds of items trip it:
 * <ul>
 * <li>items whose damage value is only wear or charge (IC2 batteries and crystals, worn tools): a different
 *     charge or wear fails the strict test. Fix: for items that can take damage (which therefore have no variants)
 *     the strict test ignores the damage value.</li>
 * <li>items whose variant is in NBT (IC2 cable insulation, Mekanism tiers, IC2 fluid cells): the strict test
 *     can't tell them apart and may take the wrong one. Fix: {@link #exact} is tried first, so an item whose NBT
 *     matches the recipe's is preferred; otherwise everything works as before.</li>
 * </ul>
 * The mod's own recipe check still runs afterwards, so a wrong pick can only fail a craft, never change it.
 */
public final class ItemMatch {
    private static boolean jecChecked, jec, warned;

    private ItemMatch() {}

    static boolean valid(ItemStack s) {
        return s != null && s.stackSize > 0;
    }

    /** RFTools' strict test, except that wear/charge (damage on items that can take damage) is ignored. */
    static boolean strict(ItemStack target, ItemStack input) {
        Item item = target.getItem();
        if (item != input.getItem()) return false;
        int meta = target.getMetadata();
        return meta == OreDictionary.WILDCARD_VALUE || meta == input.getMetadata() || (item != null && item.isDamageable());
    }

    /**
     * Same item, same variant and the same NBT as the recipe's item, ignoring the tags on the ignore list
     * (IC2's charge and heat by default). A recipe item without NBT doesn't prefer anything.
     */
    static boolean exact(ItemStack target, ItemStack input) {
        if (target.getItem() != input.getItem()) return false;
        int meta = target.getMetadata();
        if (meta != OreDictionary.WILDCARD_VALUE && meta != input.getMetadata()) return false;
        NBTTagCompound t = stripped(target);
        if (t.hasNoTags()) return true;
        return t.equals(stripped(input));
    }

    /** The recipe's item has NBT (ignoring condition tags) and the input's differs, e.g. cable insulation. */
    static boolean variantDiffers(ItemStack target, ItemStack input) {
        NBTTagCompound t = stripped(target);
        return !t.hasNoTags() && !t.equals(stripped(input));
    }

    private static NBTTagCompound stripped(ItemStack stack) {
        if (!jecChecked) {
            jec = Loader.isModLoaded("je_calculation");
            jecChecked = true;
        }
        if (jec) {
            try {
                return NbtCompat.strippedTag(stack);
            } catch (Throwable t) {
                if (!warned) {
                    warned = true;
                    System.out.println("[AoE-QoL] Using default ignored-tag list for RFTools: " + t);
                }
            }
        }
        NBTTagCompound copy = stack.getTagCompound() == null ? new NBTTagCompound() : stack.getTagCompound().copy();
        Item item = stack.getItem();
        ResourceLocation rl = item == null ? null : item.getRegistryName();
        if (rl != null && "ic2".equals(rl.getResourceDomain())) {
            copy.removeTag("charge");
            copy.removeTag("advDmg");
        }
        return copy;
    }
}
