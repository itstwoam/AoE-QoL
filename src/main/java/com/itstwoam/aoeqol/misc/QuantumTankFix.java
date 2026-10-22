package com.itstwoam.aoeqol.misc;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.CapabilityItemHandler;

/**
 * QuantumStorage's Quantum Tank says it has no item handler, yet hands one out when asked (its three bucket slots),
 * and the tank itself claims to be an item handler but its "what's in slot N" method is missing from the shipped
 * mod (it was renamed to Minecraft's inventory method when the mod was built). Extra Utilities 2's transfer and
 * retrieval nodes, told there's no item handler, fall back to using the tank itself and call the missing method:
 * the server crashes. (The tank's own item handler methods also accept anything and store nothing, so a node
 * pushing into it would have deleted items.)
 *
 * Fix: the tank reports the item handler it hands out, so other mods use its bucket slots like any inventory,
 * and the missing method is added for any mod that still uses the tank directly.
 */
public final class QuantumTankFix {
    private QuantumTankFix() {}

    /** End of TileQuantumTank.hasCapability: also true for the item handler, which getCapability already returns. */
    public static boolean hasCapability(boolean original, Object capability) {
        return original || (capability != null && capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY);
    }

    /** Added as TileQuantumTank.getStackInSlot(int) (IItemHandler): same as its inventory's slot. */
    public static ItemStack getStackInSlot(Object tank, int slot) {
        return ((IInventory) tank).getStackInSlot(slot);
    }
}
