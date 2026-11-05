package com.itstwoam.aoeqol.rftools;

import com.itstwoam.aoeqol.AoEQoLConfig;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * Taking an item out of the RFTools Storage Scanner only hands over an exact match of what you clicked, data tag
 * included. A brand-new IC2 electric item (an empty Re-Battery, crystal, drill...) has no data tag at all, but as
 * soon as your game shows its charge bar or tooltip, IC2 attaches an empty tag to the copy you see. So the item you
 * click "has" a tag and the stored one doesn't, nothing matches, and nothing comes out.
 *
 * Fix (setting "Take out brand-new items", on by default): an empty tag counts the same as no tag. Items whose tags
 * really differ (charge, insulation...) still don't match.
 *
 * Replaces the ItemHandlerHelper.canItemStacksStack calls in StorageScannerTileEntity.requestStack and
 * requestStackFromInv.
 */
public final class ScannerTake {
    private ScannerTake() {}

    public static boolean canItemStacksStack(ItemStack a, ItemStack b) {
        if (ItemHandlerHelper.canItemStacksStack(a, b)) return true;
        if (!AoEQoLConfig.ON_RFTOOLS_EMPTY_TAG || a == null || b == null || !a.isItemEqual(b)) return false;
        return empty(a.getTagCompound()) && empty(b.getTagCompound());
    }

    private static boolean empty(NBTTagCompound tag) {
        return tag == null || tag.hasNoTags();
    }
}
