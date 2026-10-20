package com.itstwoam.aoeqol.ic2;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * IC2's energy storage blocks (BatBox, CESU, MFE, MFSU and their chargepads) report their fill level to
 * comparators (0-15), but never tell the world when that level changes. Comparators and anything that reads
 * them (e.g. a Super Circuit Maker inventory scanner) only re-read on a block update next to them, so they keep
 * showing a stale value, e.g. "full" long after the MFE has drained, until something is placed or broken.
 *
 * Fix: after each server tick of such a block, if its comparator level differs from last tick (or on its first
 * tick after loading), notify the neighbours the way vanilla containers do (World.updateComparatorOutputLevel).
 * The level only has 16 steps, so this fires rarely.
 *
 * Called at the end of IC2's TileEntityElectricBlock.updateEntityServer().
 */
public final class ComparatorFix {
    private static final Map<TileEntity, Integer> LAST = new WeakHashMap<TileEntity, Integer>();
    private static boolean broken;

    private ComparatorFix() {}

    public static void afterTick(Object tile) {
        if (broken || !(tile instanceof TileEntity)) return;
        try {
            TileEntity te = (TileEntity) tile;
            World world = te.getWorld();
            if (world == null || world.isRemote) return;
            BlockPos pos = te.getPos();
            IBlockState state = world.getBlockState(pos);
            if (!state.hasComparatorInputOverride()) return;
            int level = state.getComparatorInputOverride(world, pos);
            Integer last = LAST.put(te, level);
            // also on the first tick after loading, so values saved stale before this fix get corrected
            if (last == null || last != level) world.updateComparatorOutputLevel(pos, state.getBlock());
        } catch (Throwable t) {
            broken = true;
            System.out.println("[AoE-QoL] IC2 comparator update fix disabled, please report: " + t);
        }
    }
}
