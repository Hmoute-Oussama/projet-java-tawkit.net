package com.tawkit.model.prayer;

/**
 * Represents the daily prayer slots plus sunrise.
 * The order matches the chronological sequence of the day.
 */
public enum PrayerType {
    FAJR("Fajr"),
    SHURUQ("Shuruq"),
    DHUHR("Dhuhr"),
    ASR("Asr"),
    MAGHRIB("Maghrib"),
    ISHA("Isha");

    private final String displayName;

    PrayerType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
