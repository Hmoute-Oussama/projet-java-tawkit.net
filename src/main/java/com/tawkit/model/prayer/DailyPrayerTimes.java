package com.tawkit.model.prayer;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;

/**
 * All prayer entries for a single day, keyed by PrayerType.
 */
public class DailyPrayerTimes {

    private final LocalDate date;
    private final Map<PrayerType, PrayerEntry> entries = new EnumMap<>(PrayerType.class);

    public DailyPrayerTimes(LocalDate date) {
        this.date = date;
    }

    public LocalDate getDate() {
        return date;
    }

    public void put(PrayerType type, PrayerEntry entry) {
        entries.put(type, entry);
    }

    public PrayerEntry get(PrayerType type) {
        return entries.get(type);
    }

    public Map<PrayerType, PrayerEntry> getAll() {
        return entries;
    }
}
