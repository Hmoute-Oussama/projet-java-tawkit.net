package com.tawkit.model.common;

/**
 * Single source of truth for transliterated Hijri month names (English
 * spellings used by tawkit.net, e.g. "Dhul'Qidah"). Shared by the calendar
 * service and the dashboard date header.
 */
public final class HijriMonthNames {

    /** English (transliterated) month names, index 0 = Muharram. */
    public static final String[] ENGLISH = {
            "Muharram", "Safar", "Rabi' Al-Awwal", "Rabi' Al-Thani",
            "Jumada Al-Awwal", "Jumada Al-Thani", "Rajab", "Sha'ban",
            "Ramadan", "Shawwal", "Dhul'Qidah", "Dhul'Hijjah"
    };

    private HijriMonthNames() {
    }

    /** English name of month 1..12, or "" if out of range. */
    public static String english(int month) {
        return (month >= 1 && month <= 12) ? ENGLISH[month - 1] : "";
    }
}
