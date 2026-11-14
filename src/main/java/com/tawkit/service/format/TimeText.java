package com.tawkit.service.format;

import java.time.LocalTime;

/**
 * Single source of truth for all user-facing time strings (clock digits,
 * prayer times, countdowns) so 12h/24h handling exists in exactly one place.
 */
public final class TimeText {

    private TimeText() {
    }

    /** Main clock digits, e.g. "1:07" (12h) or "13:07" (24h). */
    public static String clockDigits(LocalTime t, boolean use24) {
        if (use24) return t.getHour() + ":" + String.format("%02d", t.getMinute());
        int hr = t.getHour() % 12;
        if (hr == 0) hr = 12;
        return hr + ":" + String.format("%02d", t.getMinute());
    }

    /** "AM"/"PM" for 12h mode, "" for 24h mode. */
    public static String amPm(LocalTime t, boolean use24) {
        return use24 ? "" : (t.getHour() < 12 ? "AM" : "PM");
    }

    /** Two-digit seconds, e.g. "30". */
    public static String seconds(LocalTime t) {
        return String.format("%02d", t.getSecond());
    }

    /** Prayer-time cell, e.g. "4:26" (12h) or "04:26" (24h). */
    public static String prayerTime(LocalTime t, boolean use24) {
        if (use24) return String.format("%02d:%02d", t.getHour(), t.getMinute());
        int hr = t.getHour() % 12;
        if (hr == 0) hr = 12;
        return hr + ":" + String.format("%02d", t.getMinute());
    }

    /** Countdown "H:MM:SS" from total seconds. */
    public static String hhmmss(long totalSeconds) {
        long h = totalSeconds / 3600;
        long m = (totalSeconds % 3600) / 60;
        long s = totalSeconds % 60;
        return String.format("%d:%02d:%02d", h, m, s);
    }

    /** Countdown "M:SS" from total seconds. */
    public static String mmss(long totalSeconds) {
        long m = totalSeconds / 60;
        long s = totalSeconds % 60;
        return String.format("%d:%02d", m, s);
    }

    /** Trims "H:MM:SS" down to "H:MM" (the countdown ring shows h:mm). */
    public static String withoutSeconds(String hms) {
        if (hms == null) return "--:--";
        int firstColon = hms.indexOf(':');
        if (firstColon >= 0) {
            int secondColon = hms.indexOf(':', firstColon + 1);
            if (secondColon > 0) return hms.substring(0, secondColon);
        }
        return hms;
    }
}
