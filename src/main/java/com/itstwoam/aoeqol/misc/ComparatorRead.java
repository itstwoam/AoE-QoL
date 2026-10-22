package com.itstwoam.aoeqol.misc;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Comparator values may only be asked of blocks that say they have one (hasComparatorInputOverride); vanilla
 * comparators always check first. Two mods in the pack get this wrong, and together they crash the server:
 *  - Integrated Dynamics' Redstone Reader asks the block it faces for a comparator value without checking.
 *  - Extra Utilities 2 answers with an exception for every block of its own that has no comparator value
 *    (e.g. the Scanner), instead of 0.
 * Fix: the reader checks first, like a comparator (no comparator value reads 0), and Extra Utilities 2 blocks
 * without a comparator value answer 0 to anyone who asks anyway.
 */
public final class ComparatorRead {
    private ComparatorRead() {}

    /** Replaces state.getComparatorInputOverride(world, pos) in Integrated Dynamics' redstone reader. */
    public static int read(IBlockState state, World world, BlockPos pos) {
        return state.hasComparatorInputOverride() ? state.getComparatorInputOverride(world, pos) : 0;
    }

    /** Replaces XUTile.getComparatorLevel() for Extra Utilities 2 tiles without comparator levels (it threw). */
    public static int none() {
        return 0;
    }
}
