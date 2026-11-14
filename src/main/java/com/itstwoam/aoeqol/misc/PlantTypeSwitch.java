package com.itstwoam.aoeqol.misc;

import net.minecraftforge.common.EnumPlantType;

/**
 * Forge's Block.canSustainPlant looks the plant type up in a table (a Java enum switch) sized to the plant types that
 * existed when the table was first used. Mods add their own types later (Extra Utilities 2's "ender" for the Ender
 * Lily), and a type added after that crashes the lookup (ArrayIndexOutOfBoundsException), e.g. when farmland under
 * an Ender Lily dries out.
 *
 * Fix: the table is made large enough for types added later. Their entries are empty, so they take Forge's own
 * answer for a type it doesn't know: the block can't sustain the plant.
 */
public final class PlantTypeSwitch {
    private PlantTypeSwitch() {}

    static final int SIZE = 256;

    /** Redirected EnumPlantType.values() in Block$3's static init, where only its length is used. */
    public static EnumPlantType[] values() {
        EnumPlantType[] v = EnumPlantType.values();
        return v.length >= SIZE ? v : new EnumPlantType[SIZE];
    }
}
