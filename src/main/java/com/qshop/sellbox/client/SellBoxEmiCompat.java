package com.qshop.sellbox.client;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Optional EMI bridge that suppresses EMI while the sell box settings page is open. */
public final class SellBoxEmiCompat {
    private static final String CONFIG_CLASS = "dev.emi.emi.config.EmiConfig";
    private static final String SCREEN_MANAGER_CLASS = "dev.emi.emi.screen.EmiScreenManager";

    private static Field enabledField;
    private static Method forceRecalculate;
    private static boolean resolved;
    private static boolean suppressed;
    private static boolean previousEnabled;

    private SellBoxEmiCompat() {}

    public static void setSettingsSuppressed(boolean shouldSuppress) {
        if (!resolve()) return;
        try {
            if (shouldSuppress) {
                if (!suppressed) {
                    previousEnabled = enabledField.getBoolean(null);
                    suppressed = true;
                }
                enabledField.setBoolean(null, false);
            } else if (suppressed) {
                enabledField.setBoolean(null, previousEnabled);
                suppressed = false;
            } else {
                return;
            }
            forceRecalculate.invoke(null);
        } catch (ReflectiveOperationException ignored) {
            // EMI is optional and can be reloaded independently of the sell box.
        }
    }

    public static void restore() {
        setSettingsSuppressed(false);
    }

    private static boolean resolve() {
        if (resolved) return enabledField != null && forceRecalculate != null;
        resolved = true;
        try {
            Class<?> config = Class.forName(CONFIG_CLASS);
            enabledField = config.getField("enabled");
            Class<?> manager = Class.forName(SCREEN_MANAGER_CLASS);
            forceRecalculate = manager.getMethod("forceRecalculate");
            return true;
        } catch (ReflectiveOperationException ignored) {
            enabledField = null;
            forceRecalculate = null;
            return false;
        }
    }
}
