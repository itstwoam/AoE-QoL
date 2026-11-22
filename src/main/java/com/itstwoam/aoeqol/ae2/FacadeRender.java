package com.itstwoam.aoeqol.ae2;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;

/**
 * Draws AE2 facades as a real (turned) block would look (client only).
 *
 * AE2's FacadeBuilder.addFacade takes one sprite per face from the block's model and lays it on its own cube with
 * fixed texture directions: rotated models lose their rotation, and three faces come out mirrored. Patched in at
 * the start ({@link #begin}) and every return ({@link #end}) of addFacade: every quad AE2 added for the facade is
 * given the sprite, texture coordinates and tint of the block's own model face it should show. Which face that is
 * comes from the facade's turn ({@link FacadeTurn}): no turn = the block as placed normally; a turn shows the chosen
 * source face on the facade's outward side, turned in 90 degree steps, with the other faces following as if the
 * whole block were turned. Texture coordinates are read off the model's quad at the same spot of the face, so the
 * facade's thin edges and any cut-outs line up too. Model faces that aren't a full square, AE2's own facade texture
 * and any error: AE2's quad is left as it was.
 */
public final class FacadeRender {
    private FacadeRender() {}

    private static final ThreadLocal<Object[]> CTX = new ThreadLocal<Object[]>();
    private static volatile boolean broken;
    private static Method getOutput, getSourceBlock, resolveTint;
    private static Field textureItem, facadeTexture;

    /** Start of FacadeBuilder.addFacade(layer, facades, side, busBounds, thin, stilt, rand, builder). */
    public static void begin(Object facadeBuilder, Object facades, Object side, Object builder) {
        CTX.remove();
        if (broken || !(facades instanceof Map) || !(side instanceof EnumFacing) || builder == null) return;
        try {
            if (getOutput == null) {
                Field ft = facadeBuilder.getClass().getDeclaredField("facadeTexture");
                ft.setAccessible(true);
                facadeTexture = ft;
                getOutput = builder.getClass().getMethod("getOutput");
            }
            Object frs = ((Map<?, ?>) facades).get(side);
            if (frs == null) return;
            if (getSourceBlock == null) {
                Field tf = frs.getClass().getDeclaredField("textureItem");
                tf.setAccessible(true);
                textureItem = tf;
                resolveTint = frs.getClass().getMethod("resolveTintColor", int.class);
                getSourceBlock = frs.getClass().getMethod("getSourceBlock");
            }
            List<?> out = (List<?>) getOutput.invoke(builder);
            IBlockState state = (IBlockState) getSourceBlock.invoke(frs);
            if (out == null || state == null) return;
            int turn = 0;
            Object ti = textureItem.get(frs);
            if (ti instanceof ItemStack) turn = FacadeTurn.turnOf(((ItemStack) ti).getItemDamage());
            CTX.set(new Object[] {out, out.size(), side, turn, state, frs, facadeTexture.get(facadeBuilder)});
        } catch (Throwable t) {
            broken = true;
        }
    }

    /** Every return of FacadeBuilder.addFacade. */
    @SuppressWarnings("unchecked")
    public static void end() {
        Object[] c = CTX.get();
        CTX.remove();
        if (c == null) return;
        try {
            List<Object> out = (List<Object>) c[0];
            int start = (Integer) c[1];
            int[][] inv = FacadeTurn.inverse((EnumFacing) c[2], (Integer) c[3]);
            IBlockState state = (IBlockState) c[4];
            IBakedModel model = Minecraft.getMinecraft().getBlockRendererDispatcher().getModelForState(state);
            for (int i = start; i < out.size(); i++) {
                Object o = out.get(i);
                if (!(o instanceof BakedQuad)) continue;
                BakedQuad q = (BakedQuad) o;
                if (q.getSprite() == c[6]) continue;   // stilt and AE2's fallback texture
                BakedQuad n = redo(q, model, state, inv, c[5]);
                if (n != null) out.set(i, n);
            }
        } catch (Throwable t) {
            // leave AE2's quads as they are
        }
    }

    /** AE2's quad with the sprite, texture coordinates and tint of the model face it should show; null = keep. */
    static BakedQuad redo(BakedQuad q, IBakedModel model, IBlockState state, int[][] inv, Object frs) throws Throwable {
        EnumFacing world = q.getFace();
        if (world == null) return null;
        EnumFacing src = FacadeTurn.apply(inv, world);
        BakedQuad m = null;
        for (BakedQuad b : model.getQuads(state, src, 0L)) { m = b; break; }
        if (m == null) for (BakedQuad b : model.getQuads(state, null, 0L)) if (b.getFace() == src) { m = b; break; }
        if (m == null || m.getSprite() == null) return null;
        float[][] corner = corners(m, src);
        if (corner == null) return null;

        VertexFormat f = q.getFormat();
        int[] d = q.getVertexData().clone();
        int s = f.getIntegerSize(), pos = posOffset(f);
        if (pos < 0 || !f.hasUvOffset(0) || d.length < s * 4) return null;
        int uv = f.getUvOffsetById(0) / 4;
        int col = f.hasColor() ? f.getColorOffset() / 4 : -1;
        int rgb = 0xFFFFFF;
        if (m.hasTintIndex() && frs != null) rgb = (Integer) resolveTint.invoke(frs, m.getTintIndex()) & 0xFFFFFF;
        for (int k = 0; k < 4; k++) {
            int o = k * s;
            float[] p = {Float.intBitsToFloat(d[o + pos]) - 0.5F, Float.intBitsToFloat(d[o + pos + 1]) - 0.5F, Float.intBitsToFloat(d[o + pos + 2]) - 0.5F};
            float[] sp = FacadeTurn.mul(inv, p);
            float[] ab = inFace(src, new float[] {sp[0] + 0.5F, sp[1] + 0.5F, sp[2] + 0.5F});
            float a = clamp(ab[0]), b = clamp(ab[1]);
            float u = (1 - a) * (1 - b) * corner[0][0] + a * (1 - b) * corner[1][0] + (1 - a) * b * corner[2][0] + a * b * corner[3][0];
            float v = (1 - a) * (1 - b) * corner[0][1] + a * (1 - b) * corner[1][1] + (1 - a) * b * corner[2][1] + a * b * corner[3][1];
            d[o + uv] = Float.floatToRawIntBits(u);
            d[o + uv + 1] = Float.floatToRawIntBits(v);
            if (col >= 0) {
                int alpha = d[o + col] & 0xFF000000;
                d[o + col] = alpha | ((rgb & 0xFF) << 16) | (rgb & 0xFF00) | ((rgb >> 16) & 0xFF);
            }
        }
        return new BakedQuad(d, -1, world, m.getSprite(), q.shouldApplyDiffuseLighting(), f);
    }

    /** Texture coordinates at the model face's corners, index (a high ? 1 : 0) + (b high ? 2 : 0); null if not a full square. */
    static float[][] corners(BakedQuad m, EnumFacing src) {
        VertexFormat mf = m.getFormat();
        int[] md = m.getVertexData();
        int ms = mf.getIntegerSize(), mp = posOffset(mf);
        if (mp < 0 || !mf.hasUvOffset(0) || md == null || md.length < ms * 4) return null;
        int muv = mf.getUvOffsetById(0) / 4;
        float[][] corner = new float[4][];
        for (int k = 0; k < 4; k++) {
            int o = k * ms;
            float[] ab = inFace(src, new float[] {Float.intBitsToFloat(md[o + mp]), Float.intBitsToFloat(md[o + mp + 1]), Float.intBitsToFloat(md[o + mp + 2])});
            if (!edge(ab[0]) || !edge(ab[1])) return null;
            int idx = (ab[0] > 0.5F ? 1 : 0) + (ab[1] > 0.5F ? 2 : 0);
            if (corner[idx] != null) return null;
            corner[idx] = new float[] {Float.intBitsToFloat(md[o + muv]), Float.intBitsToFloat(md[o + muv + 1])};
        }
        return corner;
    }

    private static int posOffset(VertexFormat f) {
        for (int i = 0; i < f.getElementCount(); i++)
            if (f.getElement(i).getUsage() == VertexFormatElement.EnumUsage.POSITION) return f.getOffset(i) / 4;
        return -1;
    }

    /** The two coordinates across a face (UP/DOWN: x,z; NORTH/SOUTH: x,y; EAST/WEST: z,y). */
    static float[] inFace(EnumFacing face, float[] p) {
        switch (face.getAxis()) {
            case Y: return new float[] {p[0], p[2]};
            case Z: return new float[] {p[0], p[1]};
            default: return new float[] {p[2], p[1]};
        }
    }

    private static boolean edge(float x) {
        return Math.abs(x) < 1e-3F || Math.abs(x - 1) < 1e-3F;
    }

    private static float clamp(float x) {
        return x < 0 ? 0 : x > 1 ? 1 : x;
    }
}
