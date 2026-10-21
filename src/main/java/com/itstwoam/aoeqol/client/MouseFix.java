package com.itstwoam.aoeqol.client;

import com.itstwoam.aoeqol.AoEQoLConfig;
import net.minecraftforge.common.config.Property;
import org.apache.logging.log4j.LogManager;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

/**
 * When a screen closes, Minecraft captures the mouse again and the hidden cursor is moved back to the middle of
 * the window. Now and then (about 1 close in 50 in testing, any screen, vanilla included) Windows still delivers one
 * last "mouse is here" message from before that move, and the mouse library reads the gap as one huge movement: the
 * view spins by the cursor's distance from the middle (70-150 degrees on a 4K window).
 *
 * Fix (setting "Ignore leftover mouse jump", on by default): when the mouse is captured, the cursor's distance from
 * the middle is remembered. For the next few frames, a mouse reading that matches that distance (within a few
 * pixels) is dropped, once. Every other reading passes through unchanged, so real movement isn't affected.
 *
 * Hooks: start of MouseHelper.grabMouseCursor(), and the Mouse.getDX()/getDY() calls in MouseHelper.mouseXYChange().
 */
public final class MouseFix {
    static final int FRAMES = 6, TOLERANCE = 12, MIN_OFFSET = 40;

    private static int offsetX, offsetY, framesLeft, pendingDY;

    private MouseFix() {}

    private static boolean enabled() {
        Property p = AoEQoLConfig.ignoreMouseJump;
        return p == null || p.getBoolean();
    }

    /** Before Minecraft captures the mouse (a screen is closing): remember where the cursor is. */
    public static void beforeGrab() {
        framesLeft = 0;
        if (!enabled() || Mouse.isGrabbed()) return;
        try {
            offsetX = Mouse.getX() - Display.getWidth() / 2;
            offsetY = Mouse.getY() - Display.getHeight() / 2;
            if (Math.abs(offsetX) + Math.abs(offsetY) >= MIN_OFFSET) framesLeft = FRAMES;
        } catch (Throwable t) {
            framesLeft = 0;
        }
    }

    /** Replaces Mouse.getDX() in mouseXYChange (called once per frame, followed by {@link #dy()}). */
    public static int dx() {
        int dx = Mouse.getDX();
        pendingDY = Mouse.getDY();
        if (framesLeft > 0) {
            framesLeft--;
            if (Math.abs(dx - offsetX) <= TOLERANCE && Math.abs(pendingDY - offsetY) <= TOLERANCE) {
                framesLeft = 0;
                LogManager.getLogger("AoE-QoL").info("Ignored a leftover mouse jump of {},{} after a screen closed", dx, pendingDY);
                pendingDY = 0;
                return 0;
            }
        }
        return dx;
    }

    /** Replaces Mouse.getDY() in mouseXYChange. */
    public static int dy() {
        int dy = pendingDY;
        pendingDY = 0;
        return dy;
    }
}
