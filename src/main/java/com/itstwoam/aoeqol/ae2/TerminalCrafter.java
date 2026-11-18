package com.itstwoam.aoeqol.ae2;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

/**
 * Extra Utilities 2 recipes that cost XP (the Golden Lasso: 8 levels) find the crafting player by checking whether the
 * crafting grid belongs to the screen a player has open. The AE2 crafting terminal (and the Wireless Crafting
 * Terminal, which uses AE2's code) works recipes out in a throwaway grid owned by AE2's ContainerNull, so XU2 finds no
 * player and the result slot stays empty.
 *
 * Fix: while such a terminal works out or crafts a recipe, the player using it is remembered here; when XU2 gets
 * AE2's throwaway grid and that player still has the terminal open, XU2 checks (and later charges) that player, like
 * at a crafting table. Nothing else changes: AE2's security checks still apply to opening the terminal and to every
 * item taken from the network. Molecular Assemblers have no player and still can't make these recipes.
 */
public final class TerminalCrafter {
    private TerminalCrafter() {}

    private static final ThreadLocal<EntityPlayer> PLAYER = new ThreadLocal<EntityPlayer>();
    private static final Map<Class<?>, Method> PLAYER_INV = new ConcurrentHashMap<Class<?>, Method>();
    private static final String XU2_PSC = "com.rwtema.extrautils2.crafting.PlayerSpecificCrafting";
    private static volatile Method updatePlayer, isValid;
    private static volatile Field eventHandler;
    private static volatile boolean broken;

    // ---- AE2 / WCT side ----

    /** Start of ContainerCraftingTerm/ContainerWCT.onCraftMatrixChanged. */
    public static void beginContainer(Object container) {
        try {
            Class<?> c = container.getClass();
            Method m = PLAYER_INV.get(c);
            if (m == null) {
                m = c.getMethod("getPlayerInv");
                PLAYER_INV.put(c, m);
            }
            Object inv = m.invoke(container);
            PLAYER.set(inv instanceof InventoryPlayer ? ((InventoryPlayer) inv).player : null);
        } catch (Throwable t) {
            PLAYER.remove();
        }
    }

    /** Start of SlotCraftingTerm.craftItem(EntityPlayer, ...). */
    public static void beginCraft(Object player) {
        if (player instanceof EntityPlayer) PLAYER.set((EntityPlayer) player);
        else PLAYER.remove();
    }

    /** Before every return of the methods above. */
    public static void end() {
        PLAYER.remove();
    }

    /**
     * Replaces Platform.findMatchingRecipe in SlotCraftingTerm.craftItem: an XP recipe the player can't pay for any more
     * (levels spent since the result slot was filled) counts as no recipe, so AE2 takes nothing from the network.
     */
    public static IRecipe findMatchingRecipe(InventoryCrafting ic, World world) {
        IRecipe found = null;
        for (IRecipe r : (List<IRecipe>) CraftingManager.getInstance().getRecipeList()) {
            if (r.matches(ic, world)) {
                found = r;
                break;
            }
        }
        if (found != null && isXu2PlayerRecipe(found)) {
            Boolean ok = check(found, ic);
            if (ok != null && !ok) return null;
        }
        return found;
    }

    // ---- XU2 side ----

    /** End of XU2 PlayerSpecificCrafting.isGoodForCrafting(InventoryCrafting). */
    public static boolean isGoodForCrafting(boolean original, Object recipe, Object inv) {
        if (original || !(inv instanceof InventoryCrafting)) return original;
        Boolean ok = check(recipe, (InventoryCrafting) inv);
        return ok != null ? ok : original;
    }

    /** null = not an AE2 terminal craft (keep XU2's answer). */
    private static Boolean check(Object recipe, InventoryCrafting inv) {
        if (broken) return null;
        EntityPlayer player = PLAYER.get();
        if (player == null) return null;
        try {
            if (eventHandler == null) eventHandler = ReflectionHelper.findField(InventoryCrafting.class, "field_70465_c", "eventHandler");
            Object handler = eventHandler.get(inv);
            if (handler == null || !"appeng.container.ContainerNull".equals(handler.getClass().getName())) return null;
            if (!isTerminal(player.openContainer)) return null;
            if (isValid == null) {
                Class<?> psc = Class.forName(XU2_PSC, false, recipe.getClass().getClassLoader());
                Method u = psc.getDeclaredMethod("updatePlayer", EntityPlayerMP.class);
                Method v = psc.getDeclaredMethod("isValidForCrafting", EntityPlayer.class);
                u.setAccessible(true);
                v.setAccessible(true);
                updatePlayer = u;
                isValid = v;
            }
            if (player instanceof EntityPlayerMP) updatePlayer.invoke(recipe, player);
            return (Boolean) isValid.invoke(recipe, player);
        } catch (Throwable t) {
            broken = true;
            return null;
        }
    }

    private static boolean isTerminal(Object container) {
        if (container == null) return false;
        for (Class<?> c = container.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            String n = c.getName();
            if (n.equals("appeng.container.implementations.ContainerCraftingTerm")
                    || n.equals("p455w0rd.wct.container.ContainerWCT")) return true;
        }
        return false;
    }

    private static boolean isXu2PlayerRecipe(Object recipe) {
        for (Class<?> c = recipe.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            if (c.getName().equals(XU2_PSC)) return true;
        }
        return false;
    }
}
