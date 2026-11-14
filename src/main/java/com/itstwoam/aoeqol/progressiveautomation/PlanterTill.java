package com.itstwoam.aoeqol.progressiveautomation;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.EnumPlantType;
import net.minecraftforge.common.IPlantable;

/**
 * Progressive Automation's Planter turns dirt or grass into farmland before planting any seed that is an IPlantable,
 * whatever ground the plant needs. Extra Utilities 2's Ender Lily (plant type "ender": end stone, dirt or grass)
 * ended up on dry farmland, and when that farmland dried out Forge asked whether the lily could grow there, which
 * crashed the server (see misc.PlantTypeSwitch).
 *
 * Fix: the Planter only tills for plants of the Crop type, the only type that needs farmland. Everything else is
 * planted on the ground as it is, like a player would. If a seed can't say its type, the Planter tills as before.
 */
public final class PlanterTill {
    private PlanterTill() {}

    /** End of PA's Vanilla.shouldHoe(ItemStack) (also used by its RightClickPlants handler). */
    public static boolean shouldHoe(boolean original, Object stack) {
        if (!original || !(stack instanceof ItemStack)) return original;
        try {
            Item item = ((ItemStack) stack).getItem();
            if (!(item instanceof IPlantable)) return original;
            EnumPlantType type = ((IPlantable) item).getPlantType(null, null);
            return type == null || type == EnumPlantType.Crop;
        } catch (Throwable t) {
            return original;   // the seed needs a world to answer: till as Progressive Automation always did
        }
    }
}
