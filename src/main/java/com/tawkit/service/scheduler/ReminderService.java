package com.tawkit.service.scheduler;

import com.tawkit.model.prayer.PrayerEntry;
import com.tawkit.model.prayer.PrayerType;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * User-defined sound alerts with Tawkit's keyword syntax:
 *
 * <pre>
 *   15.BEFORE.FAJR.(Salam Alaykoum)     15 min before Fajr
 *   10.AFTER.MAGHRIB.(Time to go)       10 min after Maghrib
 *   NISF.(Midnight)                     midnight
 *   TULT.(Last third of night)          last third of night
 * </pre>
 *
 * Prayers: FAJR, SHRQ, DOHR, ASSR, MGRB, ISHA. Each rule fires once per day.
 */
public class ReminderService {

    public record Reminder(int minutes, boolean before, PrayerType prayer,
                           boolean midnight, boolean lastThird, String message, String raw) {}

    private final List<Reminder> reminders = new ArrayList<>();
    private final Set<String> firedToday = new HashSet<>();
    private LocalDate lastDay;

    public void setRules(List<String> rules) {
        reminders.clear();
        if (rules == null) return;
        for (String rule : rules) {
            Reminder r = parse(rule);
            if (r != null) reminders.add(r);
        }
    }

    public List<Reminder> getReminders() {
        return List.copyOf(reminders);
    }

    /**
     * Checks which reminders are due at the given instant.
     *
     * @param now         current time
     * @param today       today's prayer times
     * @return list of messages that fired at this instant
     */
    public List<String> check(LocalTime now, LocalDate date,
                              com.tawkit.model.prayer.DailyPrayerTimes today) {
        if (reminders.isEmpty() || today == null) return List.of();
        if (!date.equals(lastDay)) {
            lastDay = date;
            firedToday.clear();
        }

        List<String> fired = new ArrayList<>();
        for (Reminder r : reminders) {
            if (r.midnight()) {
                if (isWindow(now, LocalTime.MIDNIGHT, 60) && firedToday.add(r.raw())) {
                    fired.add(r.message());
                }
                continue;
            }
            if (r.lastThird()) {
                LocalTime start = lastThirdStart(today);
                if (start != null && !now.isBefore(start) && now.isBefore(start.plusMinutes(1))
                        && firedToday.add(r.raw())) {
                    fired.add(r.message());
                }
                continue;
            }
            PrayerEntry entry = today.get(r.prayer());
            if (entry == null || entry.getAdhanTime() == null) continue;
            LocalTime due = r.before()
                    ? entry.getAdhanTime().minusMinutes(r.minutes())
                    : entry.getAdhanTime().plusMinutes(r.minutes());
            if (isWindow(now, due, 60) && firedToday.add(r.raw())) {
                fired.add(r.message());
            }
        }
        return fired;
    }

    /** Start time of the last third of the night (Isha -> Fajr interval). */
    private LocalTime lastThirdStart(com.tawkit.model.prayer.DailyPrayerTimes today) {
        PrayerEntry isha = today.get(PrayerType.ISHA);
        PrayerEntry fajrTomorrow = null;
        // approximate with today's fajr shifted by 24h for interval computation
        PrayerEntry fajr = today.get(PrayerType.FAJR);
        if (isha == null || isha.getAdhanTime() == null || fajr == null || fajr.getAdhanTime() == null) {
            return null;
        }
        long ishaSec = isha.getAdhanTime().toSecondOfDay();
        long fajrSec = fajr.getAdhanTime().toSecondOfDay() + 86400; // next day
        long nightSeconds = fajrSec - ishaSec;
        if (nightSeconds <= 0) return null;
        long start = ishaSec + (long) (nightSeconds * (2.0 / 3.0));
        return LocalTime.ofSecondOfDay((int) (start % 86400));
    }

    private boolean isWindow(LocalTime now, LocalTime due, int toleranceSeconds) {
        long diff = Duration.between(due, now).getSeconds();
        return diff >= 0 && diff < toleranceSeconds;
    }

    /** Parses one rule; returns null when invalid. */
    public static Reminder parse(String rule) {
        String r = rule.trim();
        if (r.isBlank()) return null;

        int open = r.indexOf('(');
        int close = r.lastIndexOf(')');
        if (open < 0 || close <= open) return null;
        String message = r.substring(open + 1, close).trim();
        if (message.isEmpty()) return null;

        String head = r.substring(0, open).trim();     // e.g. "15.BEFORE.FAJR" or "NISF"
        String[] parts = head.split("\\.");
        if (parts.length == 0) return null;

        try {
            if (parts.length == 1 && parts[0].equalsIgnoreCase("NISF")) {
                return new Reminder(0, true, null, true, false, message, r);
            }
            if (parts.length == 1 && parts[0].equalsIgnoreCase("TULT")) {
                return new Reminder(0, true, null, false, true, message, r);
            }
            if (parts.length == 3) {
                int minutes = Integer.parseInt(parts[0].trim());
                boolean before = parts[1].trim().equalsIgnoreCase("BEFORE");
                PrayerType prayer = mapPrayer(parts[2].trim());
                if (prayer == null) return null;
                return new Reminder(minutes, before, prayer, false, false, message, r);
            }
        } catch (NumberFormatException ignored) {
        }
        return null;
    }

    private static PrayerType mapPrayer(String token) {
        return switch (token.toUpperCase()) {
            case "FAJR" -> PrayerType.FAJR;
            case "SHRQ", "SHURUQ" -> PrayerType.SHURUQ;
            case "DOHR", "DHUHR" -> PrayerType.DHUHR;
            case "ASSR", "ASR" -> PrayerType.ASR;
            case "MGRB", "MAGHRIB" -> PrayerType.MAGHRIB;
            case "ISHA" -> PrayerType.ISHA;
            default -> null;
        };
    }
}
