package com.itstwoam.aoeqol.ae2;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.util.EnumFacing;

/**
 * AE2 facades drawn with their block's texture direction (client only).
 *
 * AE2's FacadeBuilder takes only the sprite of each face from the block's model and puts it on its own cube with
 * fixed texture directions, so a rotated model (a log lying east-west vs north-south, a quartz pillar on its side)
 * loses its rotation. Patched in:
 * - start of FacadeBuilder.getSprite: {@link #remember} keeps the model and state being looked up;
 * - FacadeBuilder.addFacade's calls to CubeBuilder.setTexture: {@link #setTexture} also sets the face's texture
 *   rotation (CubeBuilder.setUvRotation, 0-3) to the one that matches the model's own quad for that face, and
 *   {@link #setTextureAll} (the stilt) resets the rotations to 0.
 * The matching rotation is found by comparing which way u and v run across the face on the model's quad with
 * quads AE2's own CubeBuilder makes for each rotation, so it follows AE2's conventions rather than guessing them.
 * Any problem: rotation 0, as before.
 */
public final class FacadeRender {
    private FacadeRender() {}

    private static final ThreadLocal<Object[]> LOOKUP = new ThreadLocal<Object[]>();
    private static volatile boolean broken;
    private static Method setTexFace, setTexAll, setUvRotation, setDrawFaces, addCube, getOutput;
    private static Constructor<?> newBuilder;
    /** [face][rotation] -> signature of CubeBuilder's own quad; filled lazily. */
    private static final int[][][] TABLE = new int[6][][];

    /** Start of FacadeBuilder.getSprite(model, state, facing, rand). */
    public static void remember(Object model, Object state, Object facing, long rand) {
        LOOKUP.set(new Object[] {model, state, rand});
    }

    /** Replaces builder.setTexture(facing, sprite) in FacadeBuilder.addFacade. */
    public static void setTexture(Object builder, EnumFacing facing, TextureAtlasSprite sprite) throws Throwable {
        if (!ready(builder)) {
            builder.getClass().getMethod("setTexture", EnumFacing.class, TextureAtlasSprite.class).invoke(builder, facing, sprite);
            return;
        }
        setTexFace.invoke(builder, facing, sprite);
        int rot = 0;
        try {
            Object[] l = LOOKUP.get();
            if (l != null && facing != null && sprite != null) rot = rotationFor((IBakedModel) l[0], (IBlockState) l[1], (Long) l[2], facing, sprite);
        } catch (Throwable t) {
            rot = 0;
        }
        setUvRotation.invoke(builder, facing, rot);
    }

    /** Replaces builder.setTexture(sprite) in FacadeBuilder.addFacade (the stilt): all faces back to rotation 0. */
    public static void setTextureAll(Object builder, TextureAtlasSprite sprite) throws Throwable {
        if (!ready(builder)) {
            builder.getClass().getMethod("setTexture", TextureAtlasSprite.class).invoke(builder, sprite);
            return;
        }
        setTexAll.invoke(builder, sprite);
        for (EnumFacing f : EnumFacing.values()) setUvRotation.invoke(builder, f, 0);
    }

    private static boolean ready(Object builder) {
        if (broken || builder == null) return false;
        if (setUvRotation != null) return true;
        try {
            Class<?> c = builder.getClass();
            setTexFace = c.getMethod("setTexture", EnumFacing.class, TextureAtlasSprite.class);
            setTexAll = c.getMethod("setTexture", TextureAtlasSprite.class);
            setDrawFaces = c.getMethod("setDrawFaces", EnumSet.class);
            addCube = c.getMethod("addCube", float.class, float.class, float.class, float.class, float.class, float.class);
            getOutput = c.getMethod("getOutput");
            newBuilder = c.getConstructor(VertexFormat.class, List.class);
            setUvRotation = c.getMethod("setUvRotation", EnumFacing.class, int.class);
            return true;
        } catch (Throwable t) {
            broken = true;
            return false;
        }
    }

    private static int rotationFor(IBakedModel model, IBlockState state, long rand, EnumFacing facing, TextureAtlasSprite sprite) throws Throwable {
        BakedQuad q = null;
        for (BakedQuad b : model.getQuads(state, facing, rand)) if (b.getSprite() == sprite) { q = b; break; }
        if (q == null) for (BakedQuad b : model.getQuads(state, null, rand)) if (b.getFace() == facing && b.getSprite() == sprite) { q = b; break; }
        if (q == null) return 0;
        int[] want = signature(q, facing);
        if (want == null) return 0;
        int[][] table = table(facing, sprite);
        int best = 0, bestScore = -1;
        for (int r = 0; r < 4; r++) {
            if (table[r] == null) continue;
            int score = 0;
            for (int i = 0; i < 4; i++) if (table[r][i] == want[i]) score++;
            if (score > bestScore) { best = r; bestScore = score; }
        }
        return best;
    }

    private static int[][] table(EnumFacing facing, TextureAtlasSprite sprite) throws Throwable {
        int[][] t = TABLE[facing.ordinal()];
        if (t != null) return t;
        t = new int[4][];
        for (int r = 0; r < 4; r++) {
            List<BakedQuad> out = new ArrayList<BakedQuad>();
            Object b = newBuilder.newInstance(DefaultVertexFormats.ITEM, out);
            setTexAll.invoke(b, sprite);
            setUvRotation.invoke(b, facing, r);
            setDrawFaces.invoke(b, EnumSet.of(facing));
            addCube.invoke(b, 0F, 0F, 0F, 16F, 16F, 16F);
            for (BakedQuad q : out) if (q.getFace() == facing) { t[r] = signature(q, facing); break; }
        }
        TABLE[facing.ordinal()] = t;
        return t;
    }

    /**
     * Which way u and v run across the face: signs of du/da, du/db, dv/da, dv/db, with a and b the face's two axes
     * (UP/DOWN: x,z; NORTH/SOUTH: x,y; EAST/WEST: z,y). null if the quad can't be read.
     */
    static int[] signature(BakedQuad q, EnumFacing face) {
        VertexFormat f = q.getFormat();
        int[] d = q.getVertexData();
        int stride = f.getIntegerSize();
        if (d == null || stride <= 0 || d.length < stride * 4 || !f.hasUvOffset(0)) return null;
        int pos = -1;
        for (int i = 0; i < f.getElementCount(); i++) {
            if (f.getElement(i).getUsage() == VertexFormatElement.EnumUsage.POSITION) { pos = f.getOffset(i) / 4; break; }
        }
        if (pos < 0) return null;
        int uv = f.getUvOffsetById(0) / 4;
        float[] a = new float[4], b = new float[4], u = new float[4], v = new float[4];
        for (int k = 0; k < 4; k++) {
            int o = k * stride;
            float x = Float.intBitsToFloat(d[o + pos]), y = Float.intBitsToFloat(d[o + pos + 1]), z = Float.intBitsToFloat(d[o + pos + 2]);
            switch (face.getAxis()) {
                case Y: a[k] = x; b[k] = z; break;
                case Z: a[k] = x; b[k] = y; break;
                default: a[k] = z; b[k] = y; break;
            }
            u[k] = Float.intBitsToFloat(d[o + uv]);
            v[k] = Float.intBitsToFloat(d[o + uv + 1]);
        }
        return new int[] {grad(a, u), grad(b, u), grad(a, v), grad(b, v)};
    }

    /** Sign of how t changes from the low to the high side of axis s (0 if it doesn't). */
    private static int grad(float[] s, float[] t) {
        float lo = Float.MAX_VALUE, hi = -Float.MAX_VALUE;
        for (float x : s) { lo = Math.min(lo, x); hi = Math.max(hi, x); }
        if (hi - lo < 1e-4F) return 0;
        float mid = (lo + hi) / 2, sumHi = 0, sumLo = 0, tlo = Float.MAX_VALUE, thi = -Float.MAX_VALUE;
        int nHi = 0, nLo = 0;
        for (int k = 0; k < 4; k++) {
            if (s[k] > mid) { sumHi += t[k]; nHi++; } else { sumLo += t[k]; nLo++; }
            tlo = Math.min(tlo, t[k]); thi = Math.max(thi, t[k]);
        }
        if (nHi == 0 || nLo == 0) return 0;
        float diff = sumHi / nHi - sumLo / nLo;
        float eps = (thi - tlo) * 0.25F;
        return diff > eps ? 1 : diff < -eps ? -1 : 0;
    }
}
