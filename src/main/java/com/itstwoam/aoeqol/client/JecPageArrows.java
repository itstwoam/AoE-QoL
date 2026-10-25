package com.itstwoam.aoeqol.client;

import com.itstwoam.aoeqol.AoEQoLConfig;
import java.lang.reflect.Field;
import java.util.List;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

/**
 * On three Just Enough Calculation screens (the calculator, the item picker and the ore dictionary screen) the page
 * arrows are swapped: "<" goes to the next page and ">" to the previous one. (The recipe list has them the right way
 * round.) Setting "Page arrows the right way round": after such a screen builds its buttons, the two arrows' click
 * actions are swapped, so "<" goes back and ">" goes forward. JEC's own page code is unchanged.
 *
 * Called at the end of init() of GuiCalculator, GuiPicker and GuiOreDict.
 */
public final class JecPageArrows {
    private static Field buttonList, listenerLeft, listenerRight;
    private static boolean broken;

    private JecPageArrows() {}

    public static void afterInit(Object gui) {
        if (broken || !AoEQoLConfig.ON_JEC_PAGE_ARROWS || !(gui instanceof GuiScreen)) return;
        try {
            if (buttonList == null) buttonList = ReflectionHelper.findField(GuiScreen.class, "buttonList", "field_146292_n");
            GuiButton back = null, forward = null;
            for (Object o : (List<?>) buttonList.get(gui)) {
                if (!(o instanceof GuiButton)) continue;
                GuiButton b = (GuiButton) o;
                if (b.id == 0 && "<".equals(b.displayString)) back = b;
                else if (b.id == 1 && ">".equals(b.displayString)) forward = b;
            }
            if (back == null || forward == null || back.getClass() != forward.getClass()) return;
            if (listenerLeft == null) {
                listenerLeft = field(back.getClass(), "listenerLeft");
                listenerRight = field(back.getClass(), "listenerRight");
            }
            swap(listenerLeft, back, forward);
            swap(listenerRight, back, forward);
        } catch (Throwable t) {
            broken = true;
            System.out.println("[AoE-QoL] JEC page arrow fix disabled, please report: " + t);
        }
    }

    private static Field field(Class<?> c, String name) throws NoSuchFieldException {
        Field f = c.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }

    private static void swap(Field f, Object a, Object b) throws IllegalAccessException {
        Object x = f.get(a);
        f.set(a, f.get(b));
        f.set(b, x);
    }
}
