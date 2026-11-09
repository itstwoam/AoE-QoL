package com.itstwoam.aoeqol.rftools;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;

/**
 * The Storage Scanner counted everything in a vanilla double chest twice. Each half of a double chest is its own
 * inventory in the scanner's list, but asking either half for its items (Forge's item capability) gives the WHOLE
 * double chest. So the "all inventories" view added the double chest's contents once per half: 64 clay dust showed
 * as 128, while taking it out of course only found the real 64. The same happened in the scanner's item counts
 * (RFTools screens' storage modules).
 *
 * Fix: while the scanner adds up several inventories, a chest whose other half is also in that list contributes
 * only its own half. Viewing one chest on its own still shows the whole double chest, as before; taking items out
 * is unchanged. Not a setting: it only corrects a count.
 *
 * Hooks in StorageScannerTileEntity: getInventoryForBlock and countItems mark the list being added up;
 * addItemsFromInventory's RFToolsTools.getItemCapabilitySafe and countItems' InventoryHelper.getItems are redirected.
 */
public final class ChestDedupe {
    private static final ThreadLocal<Collection<BlockPos>> LIST = new ThreadLocal<Collection<BlockPos>>();
    private static Field routable;
    private static Method capabilitySafe, getItems;

    private ChestDedupe() {}

    /** Start of getInventoryForBlock: a y of -1 means "all inventories". */
    public static void beginView(Object scanner, BlockPos pos) {
        LIST.set(pos != null && pos.getY() == -1 ? routable(scanner) : null);
    }

    /** Start of countItems: it adds up the routable inventories when starred, otherwise all of them. */
    @SuppressWarnings("unchecked")
    public static void beginCount(Object scanner, boolean starred) {
        try {
            if (starred) {
                LIST.set(routable(scanner));
            } else {
                List<BlockPos> all = (List<BlockPos>) scanner.getClass().getMethod("getInventories").invoke(scanner);
                LIST.set(all == null ? null : new HashSet<BlockPos>(all));
            }
        } catch (Throwable t) {
            LIST.set(null);
        }
    }

    public static void end() {
        LIST.remove();
    }

    @SuppressWarnings("unchecked")
    private static Collection<BlockPos> routable(Object scanner) {
        try {
            if (routable == null) {
                Field f = scanner.getClass().getDeclaredField("routable");
                f.setAccessible(true);
                routable = f;
            }
            return (Collection<BlockPos>) routable.get(scanner);
        } catch (Throwable t) {
            return null;
        }
    }

    /** The chest's own half if its other half is in the list being added up, otherwise null. */
    private static IItemHandler ownHalf(TileEntity te) {
        Collection<BlockPos> list = LIST.get();
        if (list == null || !(te instanceof TileEntityChest)) return null;
        World world = te.getWorld();
        BlockPos pos = te.getPos();
        if (world == null || pos == null) return null;
        for (EnumFacing side : EnumFacing.HORIZONTALS) {
            BlockPos other = pos.offset(side);
            if (!list.contains(other) || !world.isBlockLoaded(other)) continue;
            if (world.getBlockState(other).getBlock() != te.getBlockType()) continue;
            if (world.getTileEntity(other) instanceof TileEntityChest) return ((TileEntityChest) te).getSingleChestHandler();
        }
        return null;
    }

    /** Redirected RFToolsTools.getItemCapabilitySafe in addItemsFromInventory. */
    public static IItemHandler handler(TileEntity te) {
        try {
            IItemHandler half = ownHalf(te);
            if (half != null) return half;
        } catch (Throwable t) {
            // fall back to RFTools' own behaviour
        }
        try {
            if (capabilitySafe == null)
                capabilitySafe = Class.forName("mcjty.rftools.varia.RFToolsTools").getMethod("getItemCapabilitySafe", TileEntity.class);
            return (IItemHandler) capabilitySafe.invoke(null, te);
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw rethrow(e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Redirected InventoryHelper.getItems in countItems. */
    public static Stream<ItemStack> getItems(TileEntity te, Predicate<ItemStack> predicate) {
        try {
            IItemHandler half = ownHalf(te);
            if (half != null) {
                Stream.Builder<ItemStack> out = Stream.builder();
                for (int i = 0; i < half.getSlots(); i++) {
                    ItemStack s = half.getStackInSlot(i);
                    if (s != null && s.stackSize > 0 && predicate.test(s)) out.add(s);
                }
                return out.build();
            }
        } catch (Throwable t) {
            // fall back to RFTools' own behaviour
        }
        try {
            if (getItems == null)
                getItems = Class.forName("mcjty.lib.container.InventoryHelper").getMethod("getItems", TileEntity.class, Predicate.class);
            @SuppressWarnings("unchecked")
            Stream<ItemStack> items = (Stream<ItemStack>) getItems.invoke(null, te, predicate);
            return items;
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw rethrow(e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static RuntimeException rethrow(Throwable t) {
        if (t instanceof RuntimeException) return (RuntimeException) t;
        if (t instanceof Error) throw (Error) t;
        return new RuntimeException(t);
    }
}
