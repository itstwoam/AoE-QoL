package com.itstwoam.aoeqol.misc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Extra Utilities 2's Bag of Holding keeps its contents in the item's NBT, but sends clients an empty tag instead
 * (getNBTShareTag). In Creative mode the client is trusted with the inventory and sends its copy of a slot back to
 * the server, which stores it: on a server the bag is overwritten with an empty one and everything in it is lost
 * (single player passes items to the client in memory, so it isn't affected).
 *
 * Fix: the bag sends its real tag, so the client's copy is the same as the server's, as long as a client would
 * accept it: Minecraft reads an item's tag with a 2 MB budget (PacketBuffer, NBTSizeTracker 0x200000) and drops the
 * connection past that. A bag over that budget is sent the way Extra Utilities 2 does it.
 */
public final class BagShareTag {
    private BagShareTag() {}

    /** Minecraft's budget for reading one item's tag from a packet. */
    static final long LIMIT = 0x200000L;

    /** Replaces ItemBagOfHolding.getNBTShareTag(ItemStack). */
    public static NBTTagCompound shareTag(ItemStack stack) {
        NBTTagCompound tag = stack == null ? null : stack.getTagCompound();
        if (tag == null) return null;
        try {
            return clientAccepts(tag) ? tag : new NBTTagCompound();
        } catch (Throwable t) {
            return new NBTTagCompound();   // Extra Utilities 2's own answer
        }
    }

    /** True if a client would read this tag: written as in a packet, then read back with the client's budget. */
    static boolean clientAccepts(NBTTagCompound tag) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream(8192) {
            @Override public synchronized void write(byte[] b, int off, int len) { super.write(b, off, len); if (count > LIMIT) throw new TooBig(); }
            @Override public synchronized void write(int b) { super.write(b); if (count > LIMIT) throw new TooBig(); }
        };
        try {
            CompressedStreamTools.write(tag, new DataOutputStream(out));
        } catch (TooBig e) {
            return false;   // more bytes than the budget: the budget counts at least one per byte
        }
        try {
            CompressedStreamTools.read(new DataInputStream(new ByteArrayInputStream(out.toByteArray())), new NBTSizeTracker(LIMIT));
            return true;
        } catch (RuntimeException e) {
            return false;   // "Tried to read NBT tag that was too big"
        }
    }

    private static final class TooBig extends RuntimeException {
        TooBig() { super(null, null, false, false); }
    }
}
