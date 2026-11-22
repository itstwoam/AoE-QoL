package com.itstwoam.aoeqol.ae2;

import com.itstwoam.aoeqol.AoEQoL;
import com.itstwoam.aoeqol.AoEQoLConfig;
import io.netty.buffer.ByteBuf;
import java.lang.reflect.Method;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
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
 * Left-click an AE2 facade with a Certus/Nether Quartz Wrench or Network Tool to turn the face it shows a quarter
 * turn; sneak + left-click to show the source block's next face (top, bottom, north, south, west, east), keeping the
 * quarters. Works for any facade. The turn is stored with the placed facade ({@link FacadeTurn}) and drawn by
 * {@link FacadeRender}; broken, wrenched or picked facades come back as the plain facade, so they stack.
 *
 * Client: {@link FacadeRotateClient} cancels the click (so nothing starts breaking) and sends {@link RotateMsg}.
 * Server: checks reach, held item and the facade, then swaps it. The client only does this when the server said it
 * has the feature on ({@link HelloMsg} at login), so on a server without AoE-QoL left-click stays normal.
 */
public final class FacadeRotate {
    private FacadeRotate() {}

    public static SimpleNetworkWrapper net;
    /** Client: the server we're on has this feature on. */
    public static volatile boolean serverOn;

    private static volatile boolean broken;
    private static Class<?> partHost, facadeItem;
    private static Method selectPart, getFacadeContainer, getFacade, removeFacade, addFacade, markForUpdate, markForSave,
            getTextureItem, getItemStack, createPartFromItemStack, fromOrdinal;
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
            NBTTagCompound tag = facadeStack.getTagCompound();
            if (tag == null || !tag.hasKey("damage")) return;
            Object fi = facadeStack.getItem();
            int damage = tag.getInteger("damage");
            int turn = FacadeTurn.step(FacadeTurn.turnOf(damage), side, player.isSneaking());
            ItemStack next = facadeStack.copy();
            next.getTagCompound().setInteger("damage", FacadeTurn.plain(damage) | (turn << FacadeTurn.SHIFT));
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
            if (b != null) world.playSound(null, pos, b.getSoundType().getPlaceSound(), SoundCategory.BLOCKS, 0.5F, 1.2F);
        } catch (Throwable t) {
            AoEQoL.LOG.warn("Couldn't turn the AE2 facade at {}: {}", pos, t.toString());
        }
    }

    /** Patched over Item.getMetadata(stack) in ItemFacade.getTextureBlockState: the block meta without the turn. */
    public static int plainMeta(Item item, ItemStack stack) {
        if (stack == null || FacadeTurn.plain(stack.getItemDamage()) == stack.getItemDamage()) return item.getMetadata(stack);
        ItemStack c = stack.copy();
        c.setItemDamage(FacadeTurn.plain(stack.getItemDamage()));
        return item.getMetadata(c);
    }

    /** Patched over IFacadePart.getItemStack() where AE2 hands a facade out (drops, wrench, pick block): no turn. */
    public static ItemStack dropStack(Object part) {
        try {
            return FacadeTurn.plainStack((ItemStack) getItemStack.invoke(part));
        } catch (Throwable t) {
            try { return (ItemStack) part.getClass().getMethod("getItemStack").invoke(part); } catch (Throwable u) { return null; }
        }
    }

    private static final ThreadLocal<String> BEFORE = new ThreadLocal<String>();
    private static volatile boolean streamBroken;
    private static Method fcGetFacade, fpGetItem, fpGetDamage, locFromOrdinal;

    /**
     * Start of AE2's FacadeContainer.readFromStream (client, cable bus update): remembers which facades it had.
     * AE2 only redraws the cable when a facade appears or disappears, so a facade swapped for another one (turned
     * with the wrench) kept its old look until something else redrew the block.
     */
    public static void beforeStreamRead(Object fc) {
        BEFORE.set(facadeKey(fc));
    }

    /** End of FacadeContainer.readFromStream: "changed" also when a facade became a different one. */
    public static boolean afterStreamRead(boolean changed, Object fc) {
        if (changed) { BEFORE.remove(); return true; }
        String before = BEFORE.get();
        BEFORE.remove();
        if (before == null) return false;
        String now = facadeKey(fc);
        return now != null && !now.equals(before);
    }

    private static String facadeKey(Object fc) {
        if (streamBroken || fc == null) return null;
        try {
            if (fcGetFacade == null) {
                ClassLoader cl = FacadeRotate.class.getClassLoader();
                Class<?> loc = Class.forName("appeng.api.util.AEPartLocation", false, cl);
                Class<?> fp = Class.forName("appeng.api.parts.IFacadePart", false, cl);
                fpGetItem = fp.getMethod("getItem");
                fpGetDamage = fp.getMethod("getItemDamage");
                locFromOrdinal = loc.getMethod("fromOrdinal", int.class);
                fcGetFacade = Class.forName("appeng.api.parts.IFacadeContainer", false, cl).getMethod("getFacade", loc);
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                Object part = fcGetFacade.invoke(fc, locFromOrdinal.invoke(null, i));
                if (part == null) { sb.append('-'); continue; }
                sb.append(System.identityHashCode(fpGetItem.invoke(part))).append(':').append(fpGetDamage.invoke(part)).append(';');
            }
            return sb.toString();
        } catch (Throwable t) {
            streamBroken = true;
            return null;
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
