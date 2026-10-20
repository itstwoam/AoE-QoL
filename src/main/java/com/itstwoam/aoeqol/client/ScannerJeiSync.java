package com.itstwoam.aoeqol.client;

import com.itstwoam.aoeqol.AoEQoL;
import com.itstwoam.aoeqol.AoEQoLConfig;
import com.itstwoam.aoeqol.jei.AoEQoLJeiPlugin;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import mezz.jei.api.IItemListOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Keeps the RFTools Storage Scanner's search box and JEI's search box in sync while the scanner is open, like
 * AE2's "JEI synchronized" search: typing in the scanner sets JEI's filter, typing in JEI sets the scanner's
 * search, and opening the scanner takes over what JEI's box already holds. The scanner's text is set the way
 * typing would (its own text event fires), so its item filter and container highlighting work as usual.
 * Setting: syncStorageScannerWithJei (Mods -> AoE-QoL -> Config), read every tick.
 */
public final class ScannerJeiSync {
    static final String SCANNER = "mcjty.rftools.blocks.storagemonitor.GuiStorageScanner";

    private Field searchField;
    private Method getText, setText, fireTextEvents;
    private boolean broken;
    private GuiScreen lastScreen;
    private String lastScanner = "", lastJei = "";

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (broken || event.phase != TickEvent.Phase.END) return;
        GuiScreen screen = Minecraft.getMinecraft().currentScreen;
        if (screen == null || !SCANNER.equals(screen.getClass().getName())) {
            lastScreen = null;
            return;
        }
        if (!AoEQoLConfig.scannerJeiSync.getBoolean() || AoEQoLJeiPlugin.runtime == null) return;
        try {
            IItemListOverlay jei = AoEQoLJeiPlugin.runtime.getItemListOverlay();
            if (jei == null) return;
            Object field = field(screen);
            if (field == null) return;   // screen not set up yet
            String scanner = (String) getText.invoke(field);
            String jeiText = jei.getFilterText();
            if (scanner == null) scanner = "";
            if (jeiText == null) jeiText = "";
            if (screen != lastScreen) {
                // just opened: take over JEI's search, if any
                lastScreen = screen;
                if (!jeiText.isEmpty() && !jeiText.equals(scanner)) setScanner(field, jeiText);
                lastScanner = lastJei = jeiText.isEmpty() ? scanner : jeiText;
                return;
            }
            if (!scanner.equals(lastScanner)) {
                if (!scanner.equals(jeiText)) jei.setFilterText(scanner);
                lastScanner = lastJei = scanner;
            } else if (!jeiText.equals(lastJei)) {
                if (!jeiText.equals(scanner)) setScanner(field, jeiText);
                lastScanner = lastJei = jeiText;
            }
        } catch (Throwable t) {
            broken = true;
            AoEQoL.LOG.warn("Storage Scanner / JEI search sync disabled, please report", t);
        }
    }

    private Object field(GuiScreen screen) throws Exception {
        if (searchField == null) {
            Field f = screen.getClass().getDeclaredField("searchField");
            f.setAccessible(true);
            Class<?> type = f.getType();
            getText = type.getMethod("getText");
            setText = type.getMethod("setText", String.class);
            fireTextEvents = type.getDeclaredMethod("fireTextEvents", String.class);
            fireTextEvents.setAccessible(true);
            searchField = f;
        }
        return searchField.get(screen);
    }

    private void setScanner(Object field, String text) throws Exception {
        setText.invoke(field, text);
        fireTextEvents.invoke(field, text);
    }
}
