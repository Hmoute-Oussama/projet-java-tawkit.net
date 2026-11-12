package com.tawkit.model.prayer;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * A single prayer slot: the adhan time and the iqama time.
 */
public class PrayerEntry {

    private final PrayerType type;
    private LocalTime adhanTime;
    private LocalTime iqamaTime;

    public PrayerEntry(PrayerType type, LocalTime adhanTime, LocalTime iqamaTime) {
        this.type = type;
        this.adhanTime = adhanTime;
        this.iqamaTime = iqamaTime;
    }

    public PrayerType getType() {
        return type;
    }

    public LocalTime getAdhanTime() {
        return adhanTime;
    }

    public void setAdhanTime(LocalTime adhanTime) {
        this.adhanTime = adhanTime;
    }

    public LocalTime getIqamaTime() {
        return iqamaTime;
    }

    public void setIqamaTime(LocalTime iqamaTime) {
        this.iqamaTime = iqamaTime;
    }

    public String getAdhanDisplay() {
        return format(adhanTime);
    }

    public String getIqamaDisplay() {
        return iqamaTime == null ? "--:--" : format(iqamaTime);
    }

    private static String format(LocalTime t) {
        return t.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    @Override
    public String toString() {
        return type.getDisplayName() + " " + getAdhanDisplay() + " / " + getIqamaDisplay();
    }
}
