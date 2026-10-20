package com.itstwoam.aoeqol.enderio;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;

/**
 * Ender IO's capacitor bank item (BlockItemCapBank$InnerProv) reports the Forge energy capability for the
 * creative bank (hasCapability = true) but then hands out none (getCapability = null), on purpose, so nothing
 * drains an infinite bank. Mods that trust hasCapability and use the result straight away crash with a
 * NullPointerException, e.g. a Draconic Evolution capacitor in "charge held item" or "charge inventory" mode
 * (BrandonsCore EnergyHelper.canReceiveEnergy).
 *
 * Fix: hasCapability answers the same as getCapability, i.e. false for the creative bank. getCapability is
 * replaced too (its own check called hasCapability), so it keeps returning nothing for the creative bank.
 * Other banks are unchanged.
 */
public final class CapBankFix {
    private static Field container, backend;
    private static Method backendHas, backendGet, typeFromMeta, isCreative;
    private static boolean failed;

    private CapBankFix() {}

    private static void init(Object prov) throws Exception {
        if (isCreative != null) return;
        Class<?> c = prov.getClass();
        Field f = c.getDeclaredField("container");
        f.setAccessible(true);
        Field g = c.getDeclaredField("backend");
        g.setAccessible(true);
        backendHas = g.getType().getMethod("hasCapability", Capability.class, EnumFacing.class);
        backendGet = g.getType().getMethod("getCapability", Capability.class, EnumFacing.class);
        Class<?> type = Class.forName("crazypants.enderio.machine.capbank.CapBankType", false, c.getClassLoader());
        typeFromMeta = type.getMethod("getTypeFromMeta", int.class);
        container = f;
        backend = g;
        isCreative = type.getMethod("isCreative");
    }

    private static boolean creative(Object prov) throws Exception {
        ItemStack stack = (ItemStack) container.get(prov);
        return stack != null && (Boolean) isCreative.invoke(typeFromMeta.invoke(null, stack.getMetadata()));
    }

    /** Replaces BlockItemCapBank$InnerProv.hasCapability(capability, facing). */
    public static boolean hasCapability(Object prov, Capability<?> capability, EnumFacing facing) {
        try {
            init(prov);
            return (Boolean) backendHas.invoke(backend.get(prov), capability, facing) && !creative(prov);
        } catch (Throwable t) {
            fail(t);
            return false;   // never claim a capability we can't hand out
        }
    }

    /** Replaces BlockItemCapBank$InnerProv.getCapability(capability, facing). */
    @SuppressWarnings("unchecked")
    public static <T> T getCapability(Object prov, Capability<T> capability, EnumFacing facing) {
        try {
            init(prov);
            if (creative(prov)) return null;
            return (T) backendGet.invoke(backend.get(prov), capability, facing);
        } catch (Throwable t) {
            fail(t);
            return null;
        }
    }

    private static void fail(Throwable t) {
        if (!failed) {
            failed = true;
            System.out.println("[AoE-QoL] Ender IO capacitor bank fix failed, please report: " + t);
        }
    }
}
