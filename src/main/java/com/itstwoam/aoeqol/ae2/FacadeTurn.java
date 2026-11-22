package com.itstwoam.aoeqol.ae2;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3i;

/**
 * A placed facade's turn: which face of the source block shows on the facade's outward side, and how far that face
 * is turned (0-3 quarter turns). Stored above the block's own bits in the damage value AE2 keeps for the facade
 * ({@code damage = blockDamage | turn << 16}, turn = 1 + face * 4 + quarters, 0 = not turned), so AE2's own save
 * and network code carry it without changes. Everything that reads the block (ItemFacade.getTextureBlockState) or
 * hands the facade out as an item (drops, wrench, pick block) sees the plain damage, so facades always come back
 * as the normal facade and stack.
 */
public final class FacadeTurn {
    private FacadeTurn() {}

    public static final int SHIFT = 16, MASK = 0xFFFF;

    public static int turnOf(int damage) {
        int t = (damage >>> SHIFT) & 31;
        return t > 24 ? 0 : t;
    }

    public static int plain(int damage) {
        return damage & MASK;
    }

    /** One step: quarter turn, or (nextFace) the next source face with the same quarters. */
    public static int step(int turn, int side, boolean nextFace) {
        int face = turn == 0 ? side : (turn - 1) / 4;
        int quarters = turn == 0 ? 0 : (turn - 1) % 4;
        if (nextFace) face = (face + 1) % 6;
        else quarters = (quarters + 1) % 4;
        return face == side && quarters == 0 ? 0 : 1 + face * 4 + quarters;
    }

    /** The facade item as AE2 would hand it out: its damage without the turn. */
    public static ItemStack plainStack(ItemStack facade) {
        if (facade == null) return null;
        NBTTagCompound tag = facade.getTagCompound();
        if (tag == null || !tag.hasKey("damage")) return facade;
        int d = tag.getInteger("damage");
        if (plain(d) == d) return facade;
        ItemStack c = facade.copy();
        c.getTagCompound().setInteger("damage", plain(d));
        return c;
    }

    // --- the turn as a rotation of the block (3x3 integer matrices, about the block's centre) ---

    /** The rotation that takes the source block to how it looks on a facade on {@code side}. */
    public static int[][] rotation(EnumFacing side, int turn) {
        if (turn <= 0) return identity();
        EnumFacing face = EnumFacing.getFront((turn - 1) / 4);
        int quarters = (turn - 1) % 4;
        int[] s = vec(side), f = vec(face);
        int[][] base;
        if (face == side) base = identity();
        else if (face == side.getOpposite()) {
            int[] p = side.getAxis() == EnumFacing.Axis.Y ? new int[] {1, 0, 0} : new int[] {0, 1, 0};
            base = half(p);
        } else base = quarter(cross(f, s));
        int[][] r = base;
        int[][] q = quarter(s);
        for (int i = 0; i < quarters; i++) r = mul(q, r);
        return r;
    }

    /** Inverse of {@link #rotation} (rotations: the transpose). */
    public static int[][] inverse(EnumFacing side, int turn) {
        int[][] r = rotation(side, turn), t = new int[3][3];
        for (int i = 0; i < 3; i++) for (int j = 0; j < 3; j++) t[i][j] = r[j][i];
        return t;
    }

    public static EnumFacing apply(int[][] m, EnumFacing f) {
        int[] v = vec(f), w = new int[3];
        for (int i = 0; i < 3; i++) w[i] = m[i][0] * v[0] + m[i][1] * v[1] + m[i][2] * v[2];
        for (EnumFacing e : EnumFacing.values()) {
            Vec3i d = e.getDirectionVec();
            if (d.getX() == w[0] && d.getY() == w[1] && d.getZ() == w[2]) return e;
        }
        return f;
    }

    public static float[] mul(int[][] m, float[] p) {
        float[] o = new float[3];
        for (int i = 0; i < 3; i++) o[i] = m[i][0] * p[0] + m[i][1] * p[1] + m[i][2] * p[2];
        return o;
    }

    private static int[] vec(EnumFacing f) {
        Vec3i d = f.getDirectionVec();
        return new int[] {d.getX(), d.getY(), d.getZ()};
    }

    private static int[][] identity() {
        return new int[][] {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}};
    }

    /** +90 degrees about unit axis n: v -> n(n.v) + n x v. */
    private static int[][] quarter(int[] n) {
        return new int[][] {
            {n[0] * n[0], n[0] * n[1] - n[2], n[0] * n[2] + n[1]},
            {n[1] * n[0] + n[2], n[1] * n[1], n[1] * n[2] - n[0]},
            {n[2] * n[0] - n[1], n[2] * n[1] + n[0], n[2] * n[2]}};
    }

    /** 180 degrees about unit axis p: 2pp^T - I. */
    private static int[][] half(int[] p) {
        int[][] m = new int[3][3];
        for (int i = 0; i < 3; i++) for (int j = 0; j < 3; j++) m[i][j] = 2 * p[i] * p[j] - (i == j ? 1 : 0);
        return m;
    }

    private static int[] cross(int[] a, int[] b) {
        return new int[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    private static int[][] mul(int[][] a, int[][] b) {
        int[][] m = new int[3][3];
        for (int i = 0; i < 3; i++) for (int j = 0; j < 3; j++) m[i][j] = a[i][0] * b[0][j] + a[i][1] * b[1][j] + a[i][2] * b[2][j];
        return m;
    }
}
