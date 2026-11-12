package com.tawkit.model.prayer;

import java.time.LocalTime;
import java.util.EnumMap;
import java.util.Map;

/**
 * Iqama timing configuration. For each prayer you can either set a fixed
 * iqama time or an offset (minutes after adhan). Shuruq has no iqama.
 */
public class IqamaSettings {

    /** Mode per prayer: minutes after adhan, or a fixed clock time. */
    public enum IqamaMode { OFFSET, FIXED }

    private final Map<PrayerType, IqamaMode> modes = new EnumMap<>(PrayerType.class);
    private final Map<PrayerType, Integer> offsetsMinutes = new EnumMap<>(PrayerType.class);
    private final Map<PrayerType, LocalTime> fixedTimes = new EnumMap<>(PrayerType.class);

    public IqamaSettings() {
        // Sensible defaults
        setOffset(PrayerType.FAJR, 20);
        setOffset(PrayerType.DHUHR, 15);
        setOffset(PrayerType.ASR, 15);
        setOffset(PrayerType.MAGHRIB, 5);
        setOffset(PrayerType.ISHA, 15);
    }

    public void setOffset(PrayerType prayer, int minutesAfterAdhan) {
        modes.put(prayer, IqamaMode.OFFSET);
        offsetsMinutes.put(prayer, minutesAfterAdhan);
        fixedTimes.remove(prayer);
    }

    public void setFixed(PrayerType prayer, LocalTime time) {
        modes.put(prayer, IqamaMode.FIXED);
        fixedTimes.put(prayer, time);
        offsetsMinutes.remove(prayer);
    }

    public IqamaMode getMode(PrayerType prayer) {
        return modes.getOrDefault(prayer, IqamaMode.OFFSET);
    }

    public int getOffsetMinutes(PrayerType prayer) {
        return offsetsMinutes.getOrDefault(prayer, 15);
    }

    public LocalTime getFixedTime(PrayerType prayer) {
        return fixedTimes.get(prayer);
    }

    public boolean hasIqama(PrayerType prayer) {
        return prayer != PrayerType.SHURUQ;
    }
}
