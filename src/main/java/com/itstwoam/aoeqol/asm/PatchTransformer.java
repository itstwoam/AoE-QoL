package com.itstwoam.aoeqol.asm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.launchwrapper.IClassTransformer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Patches Just Enough Calculation 2.6.5, RFTools 5.92 and Ender IO 3.1.193 (MC 1.10.2) while their classes load.
 * Every patch only adds a call into this mod's own code ({@code com.itstwoam.aoeqol.jec}, {@code .rftools},
 * {@code .enderio}); the jars themselves are untouched.
 *
 * Each patch names the exact method (name + descriptor) it expects. If a different JEC version doesn't have it,
 * that patch is skipped and logged instead of crashing the game.
 */
public class PatchTransformer implements IClassTransformer {
    static final Logger LOG = LogManager.getLogger("AoE-QoL");

    private static final String JEC = "pers/towdium/just_enough_calculation/";
    private static final String H = "com/itstwoam/aoeqol/jec/";
    private static final String ITEMSTACK = "Lnet/minecraft/item/ItemStack;";
    private static final String NBT = "Lnet/minecraft/nbt/NBTTagCompound;";
    private static final String RECIPE = "L" + JEC + "core/Recipe;";
    private static final String STRING = "Ljava/lang/String;";

    enum Kind {
        /** Body becomes {@code return Hook.name(loads...);} (the hook takes the loaded values, returns the same type). */
        REPLACE,
        /** {@code Hook.name(loads...);} inserted at the start of the method. */
        PREPEND,
        /** {@code argN = Hook.name(loads...);} inserted at the start (replaces an argument). */
        PREPEND_ASSIGN,
        /** {@code Hook.name(loads...);} inserted before every RETURN. */
        BEFORE_RETURN,
        /** Calls to {@code redirect} (owner.name desc) inside the method become INVOKESTATIC Hook.name. */
        REDIRECT
    }

    static final class Patch {
        final String method, desc, hookOwner, hookName, hookDesc, what;
        final Kind kind;
        final String[] loads;   // e.g. "a0" (ALOAD 0), "j3" (LLOAD 3), "i3" (ILOAD 3)
        final int store;        // local for PREPEND_ASSIGN
        String rOwner, rName, rDesc; // REDIRECT target call
        int redirected;
        boolean applied;

        Patch(String method, String desc, Kind kind, String hookOwner, String hookName, String hookDesc,
              String loads, int store, String what) {
            this.method = method; this.desc = desc; this.kind = kind; this.hookOwner = hookOwner;
            this.hookName = hookName; this.hookDesc = hookDesc;
            this.loads = loads.isEmpty() ? new String[0] : loads.split(",");
            this.store = store; this.what = what;
        }
    }

    static Patch redirect(String method, String desc, String owner, String name, String callDesc,
                          String hookOwner, String hookName, String hookDesc, String what) {
        Patch p = new Patch(method, desc, Kind.REDIRECT, hookOwner, hookName, hookDesc, "", -1, what);
        p.rOwner = owner; p.rName = name; p.rDesc = callDesc;
        return p;
    }

    /** class (internal name) -> patches */
    static final Map<String, List<Patch>> PATCHES = new HashMap<String, List<Patch>>();

    static {
        add(JEC + "util/helpers/ItemStackHelper",
            new Patch("isItemEqual", "(" + ITEMSTACK + ITEMSTACK + ")Z", Kind.REPLACE,
                H + "NbtCompat", "isItemEqual", "(" + ITEMSTACK + ITEMSTACK + ")Z", "a0,a1", -1,
                "item matching with ignored tags/damage"));
        add(JEC + "util/helpers/ItemStackHelper$NBT",
            new Patch("equalsIgnoreJEC", "(" + NBT + NBT + ")Z", Kind.REPLACE,
                H + "NbtCompat", "equalsIgnoreJEC", "(" + NBT + NBT + ")Z", "a0,a1", -1,
                "tag comparison"));
        add(JEC + "JECConfig",
            new Patch("handleInit", "()V", Kind.PREPEND,
                H + "NbtCompat", "init", "()V", "", -1, "config options"));
        add(JEC + "event/TooltipEventHandler",
            new Patch("onTooltip", "(Lnet/minecraftforge/event/entity/player/ItemTooltipEvent;)V", Kind.PREPEND,
                H + "NbtCompat", "onTooltip", "(Lnet/minecraftforge/event/entity/player/ItemTooltipEvent;)V",
                "a1", -1, "tag tooltip (F3+H)"));
        add(JEC + "network/ProxyClient",
            new Patch("init", "()V", Kind.PREPEND,
                H + "ClientCommands", "register", "()V", "", -1, "/jectags and /jecunify"));
        add(JEC + "util/helpers/PlayerRecordHelper",
            new Patch("addOreDictPref", "(" + ITEMSTACK + ")V", Kind.PREPEND,
                H + "OreUnify", "onPreferenceSet", "(" + ITEMSTACK + ")V", "a0", -1,
                "ore dictionary unification on preference"),
            new Patch("addRecipe", "(" + RECIPE + STRING + ")V", Kind.PREPEND_ASSIGN,
                H + "OreUnify", "onRecipeSaved", "(" + RECIPE + STRING + ")" + RECIPE, "a0,a1", 0,
                "unification of new recipes"),
            new Patch("setRecipe", "(" + STRING + STRING + "I" + RECIPE + ")V", Kind.PREPEND_ASSIGN,
                H + "OreUnify", "onRecipeSaved", "(" + RECIPE + STRING + ")" + RECIPE, "a3,a0", 3,
                "unification of edited recipes"));
        String RFT = "com/itstwoam/aoeqol/rftools/";
        add("mcjty/rftools/craftinggrid/StorageCraftingTools",
            new Patch("match", "(" + ITEMSTACK + "Lgnu/trove/set/hash/TIntHashSet;" + ITEMSTACK + "Z)Z", Kind.REPLACE,
                RFT + "GridMatch", "match", "(" + ITEMSTACK + "Ljava/lang/Object;" + ITEMSTACK + "Z)Z",
                "a0,a1,a2,i3", -1, "crafting grid: wear/charge ignored"),
            new Patch("findMatchingItems", "(Lnet/minecraft/inventory/InventoryCrafting;Ljava/util/List;I" + ITEMSTACK
                    + "ILmcjty/rftools/craftinggrid/IItemSource;Z)I", Kind.REPLACE,
                RFT + "GridMatch", "findMatchingItems", "(Lnet/minecraft/inventory/InventoryCrafting;Ljava/util/List;I"
                    + ITEMSTACK + "ILjava/lang/Object;Z)I",
                "a0,a1,i2,a3,i4,a5,i6", -1, "crafting grid: exact variants first"));
        add("mcjty/rftools/blocks/crafter/CrafterBaseTE",
            new Patch("match", "(" + ITEMSTACK + ITEMSTACK + "Z)Z", Kind.REPLACE,
                RFT + "CrafterMatch", "match", "(" + ITEMSTACK + ITEMSTACK + "Z)Z", "a0,a1,i2", -1,
                "Crafter: wear/charge ignored"),
            new Patch("testAndConsumeCraftingItems", "(Lmcjty/rftools/blocks/crafter/CraftingRecipe;Ljava/util/Map;Z)Z",
                Kind.REPLACE, RFT + "CrafterMatch", "testAndConsumeCraftingItems",
                "(Ljava/lang/Object;Ljava/lang/Object;Ljava/util/Map;Z)Z", "a0,a1,a2,i3", -1,
                "Crafter: exact variants first"),
            redirect("func_94041_b", "(I" + ITEMSTACK + ")Z", "net/minecraft/item/ItemStack", "func_77969_a",
                "(" + ITEMSTACK + ")Z", RFT + "CrafterMatch", "ghostAccepts", "(" + ITEMSTACK + ITEMSTACK + ")Z",
                "Crafter: charged/worn items accepted in ghost slots"));
        add("crazypants/enderio/machine/capbank/BlockItemCapBank$InnerProv",
            new Patch("hasCapability", "(Lnet/minecraftforge/common/capabilities/Capability;Lnet/minecraft/util/EnumFacing;)Z",
                Kind.REPLACE, "com/itstwoam/aoeqol/enderio/CapBankFix", "hasCapability",
                "(Ljava/lang/Object;Lnet/minecraftforge/common/capabilities/Capability;Lnet/minecraft/util/EnumFacing;)Z",
                "a0,a1,a2", -1, "creative capacitor bank: no energy capability reported"),
            new Patch("getCapability", "(Lnet/minecraftforge/common/capabilities/Capability;Lnet/minecraft/util/EnumFacing;)Ljava/lang/Object;",
                Kind.REPLACE, "com/itstwoam/aoeqol/enderio/CapBankFix", "getCapability",
                "(Ljava/lang/Object;Lnet/minecraftforge/common/capabilities/Capability;Lnet/minecraft/util/EnumFacing;)Ljava/lang/Object;",
                "a0,a1,a2", -1, "creative capacitor bank: still no energy capability handed out"));
        add(JEC + "core/Calculator$CostList",
            new Patch("<init>", "(" + RECIPE + ITEMSTACK + "J)V", Kind.BEFORE_RETURN,
                H + "ProcedureFix", "fix", "(Ljava/lang/Object;" + RECIPE + ITEMSTACK + "J)V", "a0,a1,a2,j3", -1,
                "calculator step list"));
    }

    private static void add(String cls, Patch... patches) {
        List<Patch> list = new ArrayList<Patch>();
        Collections.addAll(list, patches);
        PATCHES.put(cls, list);
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null || transformedName == null
                || !(transformedName.startsWith("pers.towdium.just_enough_calculation.")
                     || transformedName.startsWith("mcjty.rftools.craftinggrid.")
                     || transformedName.startsWith("mcjty.rftools.blocks.crafter.")
                     || transformedName.startsWith("crazypants.enderio.machine.capbank."))) {
            return basicClass;
        }
        List<Patch> patches = PATCHES.get(transformedName.replace('.', '/'));
        if (patches == null) return basicClass;
        try {
            byte[] out = apply(basicClass, patches);
            for (Patch p : patches) {
                if (p.applied && p.kind == Kind.REDIRECT && p.redirected == 0) {
                    LOG.warn("{}.{}: call to redirect not found - '{}' not patched", transformedName, p.method, p.what);
                } else if (p.applied) LOG.info("Patched {}.{} ({})", transformedName, p.method, p.what);
                else LOG.warn("{}.{}{} not found - '{}' not patched (different mod version?)",
                        transformedName, p.method, p.desc, p.what);
            }
            return out;
        } catch (Throwable t) {
            LOG.error("Could not patch class " + transformedName + ", leaving it unchanged", t);
            return basicClass;
        }
    }

    /** Applies the patches to one class. Unchanged methods are copied byte-for-byte. Package-visible for tests. */
    static byte[] apply(byte[] bytes, final List<Patch> patches) {
        for (Patch p : patches) { p.applied = false; p.redirected = 0; }
        ClassReader reader = new ClassReader(bytes);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        reader.accept(new ClassVisitor(Opcodes.ASM5, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                for (Patch p : patches) {
                    if (p.applied || !p.method.equals(name) || !p.desc.equals(desc)) continue;
                    p.applied = true;
                    return patchMethod(mv, p);
                }
                return mv;
            }
        }, 0);
        return writer.toByteArray();
    }

    private static void loadArgs(MethodVisitor mv, Patch p) {
        for (String l : p.loads) {
            char k = l.charAt(0);
            mv.visitVarInsn(k == 'j' ? Opcodes.LLOAD : k == 'i' ? Opcodes.ILOAD : Opcodes.ALOAD, Integer.parseInt(l.substring(1)));
        }
    }

    private static MethodVisitor patchMethod(final MethodVisitor mv, final Patch p) {
        switch (p.kind) {
            case REPLACE:
                // write the new body and drop the original one
                mv.visitCode();
                loadArgs(mv, p);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, p.hookOwner, p.hookName, p.hookDesc, false);
                mv.visitInsn(Type.getReturnType(p.desc).getOpcode(Opcodes.IRETURN));
                mv.visitMaxs(0, 0);
                mv.visitEnd();
                return null;
            case PREPEND:
            case PREPEND_ASSIGN:
                return new MethodVisitor(Opcodes.ASM5, mv) {
                    @Override
                    public void visitCode() {
                        super.visitCode();
                        loadArgs(mv, p);
                        super.visitMethodInsn(Opcodes.INVOKESTATIC, p.hookOwner, p.hookName, p.hookDesc, false);
                        if (p.kind == Kind.PREPEND_ASSIGN) super.visitVarInsn(Opcodes.ASTORE, p.store);
                    }
                };
            case BEFORE_RETURN:
                return new MethodVisitor(Opcodes.ASM5, mv) {
                    @Override
                    public void visitInsn(int opcode) {
                        if (opcode == Opcodes.RETURN) {
                            loadArgs(mv, p);
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, p.hookOwner, p.hookName, p.hookDesc, false);
                        }
                        super.visitInsn(opcode);
                    }
                };
            case REDIRECT:
                return new MethodVisitor(Opcodes.ASM5, mv) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                        if (opcode == Opcodes.INVOKEVIRTUAL && owner.equals(p.rOwner) && name.equals(p.rName) && desc.equals(p.rDesc)) {
                            p.redirected++;
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, p.hookOwner, p.hookName, p.hookDesc, false);
                            return;
                        }
                        super.visitMethodInsn(opcode, owner, name, desc, itf);
                    }
                };
            default:
                return mv;
        }
    }
}
