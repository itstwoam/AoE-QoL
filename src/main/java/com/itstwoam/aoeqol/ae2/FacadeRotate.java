package com.itstwoam.aoeqol.ae2;

import com.itstwoam.aoeqol.AoEQoL;
import com.itstwoam.aoeqol.AoEQoLConfig;
import io.netty.buffer.ByteBuf;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Left-click an AE2 facade with a Certus/Nether Quartz Wrench or Network Tool to turn it: logs step through standing,
 * lying east-west, lying north-south and bark on all sides; other blocks with an axis or facing step through theirs
 * (hay bale, bone block, purpur pillar, quartz pillar, pumpkin...). Blocks without a direction don't change.
 *
 * A facade only stores the source item and its damage, and AE2 draws it from block.getStateFromMeta(damage). This
 * steps the block's own direction property, keeps only states that survive the state -> meta -> state round trip,
 * and lets AE2's own createFacadeForItem accept the result before swapping the facade, so it can't make a facade AE2
 * would refuse. The rotated facade keeps its direction when broken, picked or placed again.
 *
 * Client: {@link FacadeRotateClient} cancels the click (so nothing starts breaking) and sends {@link RotateMsg}.
 * Server: checks reach, held item and the facade, then swaps it. The client only does this when the server said it
 * has the feature on ({@link HelloMsg} at login), so on a server without AoE-QoL left-click stays normal.
 *
 * Blocks whose item doesn't keep its damage (hay bale, bone block, purpur pillar: plain ItemBlock) would always draw
 * standing, so {@link #textureState} (end of ItemFacade.getTextureBlockState) uses the stored damage for those.
 */
public final class FacadeRotate {
    private FacadeRotate() {}

    public static SimpleNetworkWrapper net;
    /** Client: the server we're on has this feature on. */
    public static volatile boolean serverOn;

    private static volatile boolean broken;
    private static Class<?> partHost, facadeItem;
    private static Method selectPart, getFacadeContainer, getFacade, removeFacade, addFacade, markForUpdate, markForSave,
            getTextureItem, getItemStack, createFacadeForItem, createPartFromItemStack, fromOrdinal;
    private static java.lang.reflect.Field spFacade, spPart, spSide;

    private static final String[] TOOLS = {"appliedenergistics2:certus_quartz_wrench", "appliedenergistics2:nether_quartz_wrench",
            "appliedenergistics2:network_tool"};

    public static void init() {
        net = NetworkRegistry.INSTANCE.newSimpleChannel(AoEQoL.MODID);
        net.registerMessage(RotateHandler.class, RotateMsg.class, 0, Side.SERVER);
        net.registerMessage(HelloHandler.class, HelloMsg.class, 1, Side.CLIENT);
        MinecraftForge.EVENT_BUS.register(new FacadeRotate.Events());
    }

    private static boolean ready() {
        if (broken) return false;
        if (partHost != null) return true;
        try {
            ClassLoader cl = FacadeRotate.class.getClassLoader();
            partHost = Class.forName("appeng.api.parts.IPartHost", false, cl);
            Class<?> fc = Class.forName("appeng.api.parts.IFacadeContainer", false, cl);
            Class<?> fp = Class.forName("appeng.api.parts.IFacadePart", false, cl);
            Class<?> sp = Class.forName("appeng.api.parts.SelectedPart", false, cl);
            Class<?> loc = Class.forName("appeng.api.util.AEPartLocation", false, cl);
            facadeItem = Class.forName("appeng.items.parts.ItemFacade", false, cl);
            selectPart = partHost.getMethod("selectPart", Vec3d.class);
            getFacadeContainer = partHost.getMethod("getFacadeContainer");
            markForUpdate = partHost.getMethod("markForUpdate");
            markForSave = partHost.getMethod("markForSave");
            getFacade = fc.getMethod("getFacade", loc);
            removeFacade = fc.getMethod("removeFacade", partHost, loc);
            addFacade = fc.getMethod("addFacade", fp);
            getTextureItem = fp.getMethod("getTextureItem");
            getItemStack = fp.getMethod("getItemStack");
            createFacadeForItem = facadeItem.getMethod("createFacadeForItem", ItemStack.class, boolean.class);
            createPartFromItemStack = facadeItem.getMethod("createPartFromItemStack", ItemStack.class, loc);
            fromOrdinal = loc.getMethod("fromOrdinal", int.class);
            spFacade = sp.getField("facade");
            spPart = sp.getField("part");
            spSide = sp.getField("side");
            return true;
        } catch (Throwable t) {
            broken = true;
            partHost = null;
            AoEQoL.LOG.warn("AE2 facade turning is off: AE2's classes aren't as expected ({})", t.toString());
            return false;
        }
    }

    public static boolean isTool(ItemStack held) {
        if (held == null || held.getItem() == null) return false;
        ResourceLocation id = held.getItem().getRegistryName();
        if (id == null) return false;
        String s = id.toString();
        for (String t : TOOLS) if (t.equals(s)) return true;
        return false;
    }

    /** The side (AEPartLocation ordinal) of the facade under the hit point, or -1. Works on both sides. */
    public static int facadeSideAt(World world, BlockPos pos, Vec3d hit) {
        if (!ready() || world == null || pos == null || hit == null) return -1;
        try {
            TileEntity te = world.getTileEntity(pos);
            if (!partHost.isInstance(te)) return -1;
            Object sel = selectPart.invoke(te, hit.subtract(pos.getX(), pos.getY(), pos.getZ()));
            if (sel == null || spPart.get(sel) != null || spFacade.get(sel) == null) return -1;
            Object side = spSide.get(sel);
            return side == null ? -1 : ((Enum<?>) side).ordinal();
        } catch (Throwable t) {
            return -1;
        }
    }

    /** Server: turn the facade on that side one step. */
    static void rotate(EntityPlayerMP player, BlockPos pos, int side, Vec3d hit) {
        if (!AoEQoLConfig.ON_AE2_ROTATE_FACADES || !ready() || side < 0 || side > 5) return;
        World world = player.worldObj;
        if (!world.isBlockLoaded(pos) || player.getDistanceSq(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64) return;
        if (!isTool(player.getHeldItemMainhand())) return;
        if (facadeSideAt(world, pos, hit) != side) return;
        try {
            TileEntity te = world.getTileEntity(pos);
            Object loc = fromOrdinal.invoke(null, side);
            Object fc = getFacadeContainer.invoke(te);
            Object part = getFacade.invoke(fc, loc);
            if (part == null) return;
            ItemStack facadeStack = (ItemStack) getItemStack.invoke(part);
            ItemStack base = (ItemStack) getTextureItem.invoke(part);
            if (facadeStack == null || base == null || !facadeItem.isInstance(facadeStack.getItem())) return;
            Object fi = facadeStack.getItem();
            ItemStack next = nextFacade(fi, base);
            if (next == null) return;
            Object newPart = createPartFromItemStack.invoke(fi, next, loc);
            if (newPart == null) return;
            removeFacade.invoke(fc, te, loc);
            if (!(Boolean) addFacade.invoke(fc, newPart)) {
                addFacade.invoke(fc, part); // put the old one back
                return;
            }
            markForSave.invoke(te);
            markForUpdate.invoke(te);
            Block b = Block.getBlockFromItem(base.getItem());
            if (b != null) world.playSound(null, pos, b.getSoundType().getPlaceSound(), SoundCategory.BLOCKS, 0.6F, 1.1F);
        } catch (Throwable t) {
            AoEQoL.LOG.warn("Couldn't turn the AE2 facade at {}: {}", pos, t.toString());
        }
    }

    /** The facade item for the next direction of the base block, or null if it has none. */
    private static ItemStack nextFacade(Object facadeItemObj, ItemStack base) throws Exception {
        Item item = base.getItem();
        Block block = Block.getBlockFromItem(item);
        if (block == null) return null;
        int meta = stateMeta(item, base.getItemDamage());
        IBlockState state = block.getStateFromMeta(meta);
        for (IProperty<?> prop : state.getPropertyNames()) {
            List<Object> values = directionValues(prop, state.getValue(prop));
            if (values == null || values.size() < 2) continue;
            int at = values.indexOf(state.getValue(prop));
            for (int step = 1; step < values.size(); step++) {
                IBlockState ns = with(state, prop, values.get((at + step) % values.size()));
                int nm = block.getMetaFromState(ns);
                if (nm < 0 || nm > 15 || block.getStateFromMeta(nm) != ns) continue;
                ItemStack nb = new ItemStack(item, 1, nm);
                if (stateMeta(item, nm) != nm) continue;
                Object f = createFacadeForItem.invoke(facadeItemObj, nb, false);
                if (f instanceof ItemStack) return (ItemStack) f;
            }
        }
        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static IBlockState with(IBlockState s, IProperty p, Object v) {
        return s.withProperty(p, (Comparable) v);
    }

    /**
     * The values of a direction property, in turning order, or null if it isn't one. Axis properties go standing,
     * east-west, north-south, then all-bark/none; facing goes in its own order. A "variant" holding pillar values
     * (quartz pillar: lines_y/x/z) counts with just those values.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static List<Object> directionValues(IProperty prop, Object current) {
        Collection<?> all = prop.getAllowedValues();
        Class<?> vc = prop.getValueClass();
        String name = prop.getName();
        boolean axis = vc == EnumFacing.Axis.class || "axis".equals(name)
                || (vc.isEnum() && vc.getName().equals("net.minecraft.block.BlockLog$EnumAxis"));
        boolean facing = vc == EnumFacing.class || "facing".equals(name);
        List<Object> out = new ArrayList<Object>();
        if (axis) {
            for (String n : new String[] {"y", "x", "z", "none"})
                for (Object v : all) if (n.equals(prop.getName((Comparable) v))) out.add(v);
            for (Object v : all) if (!out.contains(v)) out.add(v);
            return out;
        }
        if (facing) {   // turning round: north, east, south, west, then up and down
            for (String n : new String[] {"north", "east", "south", "west", "up", "down"})
                for (Object v : all) if (n.equals(prop.getName((Comparable) v))) out.add(v);
            for (Object v : all) if (!out.contains(v)) out.add(v);
            return out;
        }
        String cur = prop.getName((Comparable) current);
        if (cur != null && cur.startsWith("lines_")) {
            for (String n : new String[] {"lines_y", "lines_x", "lines_z"})
                for (Object v : all) if (n.equals(prop.getName((Comparable) v))) out.add(v);
            return out;
        }
        return null;
    }

    /**
     * The block meta a facade of this item and damage stands for. AE2 uses item.getMetadata(damage); for an item that
     * doesn't keep its damage (plain ItemBlock) the damage itself, so turned hay/bone/purpur facades keep their axis.
     */
    private static int stateMeta(Item item, int damage) {
        int m = item.getMetadata(damage);
        return (m != damage && damage >= 0 && damage <= 15 && !item.getHasSubtypes()) ? damage : m;
    }

    /**
     * Patched in at the end of AE2's ItemFacade.getTextureBlockState: for a facade of an item that drops its damage
     * (hay bale, bone block, purpur pillar), the state for the stored damage instead of the standing one. Only
     * facades turned with the wrench have such a damage; every other facade comes back unchanged.
     */
    public static IBlockState textureState(IBlockState ret, Object stack) {
        try {
            if (ret == null || !(stack instanceof ItemStack)) return ret;
            NBTTagCompound tag = ((ItemStack) stack).getTagCompound();
            if (tag == null || !tag.hasKey("damage")) return ret;
            int damage = tag.getInteger("damage");
            if (damage <= 0 || damage > 15) return ret;
            Item item = Item.getByNameOrId(tag.getString("item"));
            if (item == null || item.getHasSubtypes() || item.getMetadata(damage) == damage) return ret;
            Block block = Block.getBlockFromItem(item);
            if (block == null || ret.getBlock() != block) return ret;
            IBlockState s = block.getStateFromMeta(damage);
            return block.getMetaFromState(s) == damage ? s : ret;
        } catch (Throwable t) {
            return ret;
        }
    }

    /** Client -> server: turn the facade on {@code side} of the cable bus at {@code pos}. */
    public static final class RotateMsg implements IMessage {
        BlockPos pos;
        int side;
        float hx, hy, hz;

        public RotateMsg() {}

        public RotateMsg(BlockPos pos, int side, Vec3d hit) {
            this.pos = pos;
            this.side = side;
            this.hx = (float) hit.xCoord;
            this.hy = (float) hit.yCoord;
            this.hz = (float) hit.zCoord;
        }

        @Override public void fromBytes(ByteBuf buf) {
            pos = BlockPos.fromLong(buf.readLong());
            side = buf.readByte();
            hx = buf.readFloat(); hy = buf.readFloat(); hz = buf.readFloat();
        }

        @Override public void toBytes(ByteBuf buf) {
            buf.writeLong(pos.toLong());
            buf.writeByte(side);
            buf.writeFloat(hx); buf.writeFloat(hy); buf.writeFloat(hz);
        }
    }

    public static final class RotateHandler implements IMessageHandler<RotateMsg, IMessage> {
        @Override public IMessage onMessage(final RotateMsg m, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            if (player == null || m.pos == null) return null;
            ((WorldServer) player.worldObj).addScheduledTask(new Runnable() {
                @Override public void run() {
                    rotate(player, m.pos, m.side, new Vec3d(m.hx, m.hy, m.hz));
                }
            });
            return null;
        }
    }

    /** Server -> client at login: whether this server turns facades. */
    public static final class HelloMsg implements IMessage {
        boolean on;

        public HelloMsg() {}

        HelloMsg(boolean on) { this.on = on; }

        @Override public void fromBytes(ByteBuf buf) { on = buf.readBoolean(); }

        @Override public void toBytes(ByteBuf buf) { buf.writeBoolean(on); }
    }

    public static final class HelloHandler implements IMessageHandler<HelloMsg, IMessage> {
        @Override public IMessage onMessage(HelloMsg m, MessageContext ctx) {
            serverOn = m.on;
            return null;
        }
    }

    public static final class Events {
        @SubscribeEvent
        public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
            EntityPlayer p = event.player;
            if (p instanceof EntityPlayerMP && net != null) net.sendTo(new HelloMsg(AoEQoLConfig.ON_AE2_ROTATE_FACADES), (EntityPlayerMP) p);
        }
    }
}
