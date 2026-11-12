package com.tawkit.service.localization;

import java.util.ListResourceBundle;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Small i18n layer. Bundles live in <code>/i18n/messages_XX.properties</code>.
 * Arabic switches the UI to RTL.
 */
public final class Messages {

    private static final String BASE = "i18n.messages";
    private static volatile Locale current = Locale.ENGLISH;

    private Messages() {
    }

    public static void setLanguage(String code) {
        current = switch (code == null ? "en" : code.toLowerCase()) {
            case "ar" -> new Locale("ar");
            case "fr" -> new Locale("fr");
            default -> Locale.ENGLISH;
        };
    }

    public static Locale getLocale() {
        return current;
    }

    public static boolean isRTL() {
        return "ar".equals(current.getLanguage());
    }

    public static String get(String key) {
        try {
            return ResourceBundle.getBundle(BASE, current).getString(key);
        } catch (MissingResourceException e) {
            try {
                return ResourceBundle.getBundle(BASE, Locale.ENGLISH).getString(key);
            } catch (MissingResourceException e2) {
                return key;
            }
        }
    }

    /** Localized prayer name. */
    public static String prayerName(com.tawkit.model.prayer.PrayerType type) {
        return get("prayer." + type.name());
    }

    /** Fallback bundle used when no properties files are present. */
    public static class Defaults extends ListResourceBundle {
        @Override
        protected Object[][] getContents() {
            return new Object[][]{};
        }
    }
}
