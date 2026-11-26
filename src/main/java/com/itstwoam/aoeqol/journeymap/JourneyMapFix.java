package com.itstwoam.aoeqol.journeymap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Map;
import org.apache.logging.log4j.LogManager;

/**
 * JourneyMap 5.5.2 fixes.
 *
 * Web map images: at startup JourneyMap copies its theme and entity icons out of its jar into journeymap/icon/,
 * and the web map serves its images from there. It checks for its own jar with the URL's raw path, so a game folder
 * with a space in it ("Age%20of%20Engineering") makes the jar "not exist", nothing is copied and every web map
 * image is a 404. {@link #isInJar} and {@link #path} use the decoded file path instead.
 *
 * Web map Auto-Map: the map state reads the full-screen map's settings, which JourneyMap only hands it once the
 * full-screen map has been opened; before that, Auto-Map from the web map fails with a NullPointerException.
 * {@link #ensureProperties} gives the state the full-screen map's settings when it has none yet.
 *
 * Web map west of X=0 blank: the web map draws each 512-block tile as one degree of longitude, and today's Google
 * Maps script wraps tile numbers around the globe (360 tiles at zoom 0, 360 * 2^zoom at higher zooms) before asking
 * for them, so tile -1 is requested as 359. The script JourneyMap was written for passed negative numbers through.
 * {@link #tileParameter} turns a wrapped x back into the negative one (a tile past 180 degrees can't exist).
 *
 * Web map waypoints, mobs and other players never drawn: the page's refresh sets a "drawing" flag, draws mobs and
 * players, then waypoints, then clears the flag. The waypoint step asks Google Maps for the visible area, which
 * today's script leaves undefined until the map has settled, so the first refresh throws there and the flag stays
 * set: every later refresh is skipped as "already drawing". Once the area is known, it is upside down (JourneyMap's
 * projection runs north-south the other way to what Google Maps expects), so "is this waypoint on screen" is false for
 * every waypoint. {@link #webFile} patches journeymap.js as it is served: every waypoint is drawn (Google Maps skips
 * off-screen markers itself), and an error in the waypoint step no longer leaves the flag set.
 */
public final class JourneyMapFix {
    private JourneyMapFix() {}

    /** Replaces FileHandler.isInJar(URL). Same rule, with the decoded path. */
    public static boolean isInJar(URL location) {
        if ("jar".equals(location.getProtocol())) return true;
        if (!"file".equals(location.getProtocol())) return false;
        File file = toFile(location);
        return file.exists() && file.getName().endsWith(".jar");
    }

    /** Replaces URL.getPath()/getFile() in FileHandler.copyResources(File, URL, ...): decoded for file: URLs. */
    public static String path(URL url) {
        if ("file".equals(url.getProtocol())) return toFile(url).getPath();
        return url.getPath();
    }

    public static String file(URL url) {
        if ("file".equals(url.getProtocol())) return toFile(url).getPath();
        return url.getFile();
    }

    static File toFile(URL url) {
        try {
            return new File(url.toURI());
        } catch (Throwable t) {
            try {
                return new File(URLDecoder.decode(url.getFile(), "UTF-8"));
            } catch (Throwable t2) {
                return new File(url.getFile());
            }
        }
    }

    private static Field propsField;
    private static Method getClient, getFullMapProperties;
    private static boolean broken;

    /** Prepended to MapState.getMapType() and setMapType(MapType). */
    public static void ensureProperties(Object state) {
        if (broken || state == null) return;
        try {
            if (propsField == null) {
                Field f = state.getClass().getDeclaredField("lastMapProperties");
                f.setAccessible(true);
                propsField = f;
            }
            if (propsField.get(state) != null) return;
            if (getClient == null) {
                getClient = Class.forName("journeymap.common.Journeymap").getMethod("getClient");
                getFullMapProperties = getClient.getReturnType().getMethod("getFullMapProperties");
            }
            Object client = getClient.invoke(null);
            if (client == null) return;
            Object props = getFullMapProperties.invoke(client);
            if (props != null) propsField.set(state, props);
        } catch (Throwable t) {
            broken = true;
            LogManager.getLogger("AoE-QoL").warn("JourneyMap map state fix disabled: " + t);
        }
    }

    private static Method getParameter;

    /** Replaces TileService's getParameter(Map, String, Integer) calls in filter(): unwraps the tile "x". */
    public static Integer tileParameter(Object service, Map map, String key, Integer def) throws Exception {
        if (getParameter == null) {
            Method m = Class.forName("journeymap.client.service.BaseService")
                .getDeclaredMethod("getParameter", Map.class, String.class, Integer.class);
            m.setAccessible(true);
            getParameter = m;
        }
        Integer value = (Integer) getParameter.invoke(service, map, key, def);
        if (value == null || !"x".equals(key)) return value;
        try {
            Integer zoom = (Integer) getParameter.invoke(service, map, "zoom", 0);
            int z = zoom == null ? 0 : zoom;
            if (z < 0 || z > 20) return value;
            int wrap = 360 << z;
            int x = value;
            if (x >= wrap / 2 && x < wrap) return x - wrap;
        } catch (Throwable ignored) {
        }
        return value;
    }

    private static final Charset LATIN1 = Charset.forName("ISO-8859-1");
    private static final String[][] JS_FIXES = {
        {"if(e.getBounds().contains(A))", "if(!0)"},
        {"U(),Q(),J=!1", "function(){try{U()}catch(x){console.error(x)}}(),Q(),J=!1"},
    };

    /** FILTER_RETURN on FileService.getStream(String, Event): patches the web map's journeymap.js. */
    public static InputStream webFile(InputStream in, String path) {
        if (in == null || path == null || !path.endsWith("/js/journeymap.js")) return in;
        byte[] bytes;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            in.close();
            bytes = out.toByteArray();
        } catch (Throwable t) {
            LogManager.getLogger("AoE-QoL").warn("JourneyMap web map script not patched: " + t);
            return null;
        }
        String js = new String(bytes, LATIN1);
        for (String[] fix : JS_FIXES) {
            if (js.contains(fix[0])) js = js.replace(fix[0], fix[1]);
        }
        return new ByteArrayInputStream(js.getBytes(LATIN1));
    }
}
