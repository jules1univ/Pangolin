package fr.univrennes.istic.l2gen.application.core.config;

import java.lang.management.ManagementFactory;
import java.util.prefs.Preferences;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import fr.univrennes.istic.l2gen.application.Pangol1;

public final class Config {

    private static Preferences prefs = Preferences.userNodeForPackage(Pangol1.class);

    public static boolean DEBUG_MODE = ManagementFactory.getRuntimeMXBean().getInputArguments().toString()
            .indexOf("-agentlib:jdwp") > 0
            || Config.getBoolean("settings.advanced.dev_mode", false);
    public static boolean DARK_MODE = false;

    public static FlatSVGIcon getIcon(String path) {
        String newPath = path.replace(".svg", Config.DARK_MODE ? "_light.svg" : "_dark.svg");
        return new FlatSVGIcon(newPath, 14, 14);
    }

    public static FlatSVGIcon getIcon(String path, float scale) {
        String newPath = path.replace(".svg", Config.DARK_MODE ? "_light.svg" : "_dark.svg");
        return new FlatSVGIcon(newPath, (int) (14 * scale), (int) (14 * scale));
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        return prefs.getBoolean(key, defaultValue);
    }

    public static int getInt(String key, int defaultValue) {
        return prefs.getInt(key, defaultValue);
    }

    public static float getFloat(String key, float defaultValue) {
        return prefs.getFloat(key, defaultValue);
    }

    public static byte[] getByteArray(String key, byte[] defaultValue) {
        return prefs.getByteArray(key, defaultValue);
    }

    public static String getString(String key, String defaultValue) {
        return prefs.get(key, defaultValue);
    }

    public static void put(String key, boolean value) {
        prefs.putBoolean(key, value);
    }

    public static void put(String key, int value) {
        prefs.putInt(key, value);
    }

    public static void put(String key, float value) {
        prefs.putFloat(key, value);
    }

    public static void put(String key, String value) {
        prefs.put(key, value);
    }

    public static void put(String key, byte[] value) {
        prefs.putByteArray(key, value);
    }

    public static void putIfAbsent(String key, String value) {
        if (prefs.get(key, null) == null) {
            prefs.put(key, value);
        }
    }

    public static void putIfAbsent(String key, boolean value) {
        if (prefs.get(key, null) == null) {
            prefs.putBoolean(key, value);
        }
    }

    public static void putIfAbsent(String key, int value) {
        if (prefs.get(key, null) == null) {
            prefs.putInt(key, value);
        }
    }

    public static void putIfAbsent(String key, float value) {
        if (prefs.get(key, null) == null) {
            prefs.putFloat(key, value);
        }
    }

    public static void clear(String key) {
        prefs.remove(key);
    }

    public static void clear() {
        try {
            prefs.clear();
        } catch (Exception e) {
            Log.debug("Erreur lors de la réinitialisation des préférences", e);
        }
    }
}
