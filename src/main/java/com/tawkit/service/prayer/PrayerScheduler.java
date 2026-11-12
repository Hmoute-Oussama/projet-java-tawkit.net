package com.tawkit.service.prayer;

import com.tawkit.model.prayer.DailyPrayerTimes;
import com.tawkit.model.prayer.IqamaSettings;
import com.tawkit.model.prayer.Location;
import com.tawkit.model.prayer.PrayerEntry;
import com.tawkit.model.prayer.PrayerType;
import com.tawkit.model.settings.AppSettings;

import javax.swing.Timer;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Heartbeat of the application. Every second it determines:
 * <ul>
 *   <li>the next upcoming prayer (today's, or tomorrow's Fajr at night)</li>
 *   <li>the countdown to the next adhan</li>
 *   <li>the iqama countdown after an adhan has fired</li>
 *   <li>when to fire adhan / iqama events (consumed by the audio service)</li>
 *   <li>day rollover (times recomputed, listeners notified)</li>
 * </ul>
 */
public class PrayerScheduler {

    public enum State { TO_ADHAN, ADHAN_PLAYING, IQAMA_COUNTDOWN, PRAYING }

    /** Immutable snapshot of the scheduler state for UI updates. */
    public static final class TickSnapshot {
        public final LocalDate date;
        public final PrayerType nextPrayer;       // may be null at edge cases
        public final LocalTime nextAdhanTime;
        public final LocalTime nextIqamaTime;
        public final long secondsToNextAdhan;     // -1 when unknown
        public final long secondsToIqama;         // -1 when not in iqama countdown
        public final State state;
        public final PrayerType currentPrayer;    // the prayer whose adhan just fired

        TickSnapshot(LocalDate date, PrayerType nextPrayer, LocalTime nextAdhanTime, LocalTime nextIqamaTime,
                     long secondsToNextAdhan, long secondsToIqama, State state, PrayerType currentPrayer) {
            this.date = date;
            this.nextPrayer = nextPrayer;
            this.nextAdhanTime = nextAdhanTime;
            this.nextIqamaTime = nextIqamaTime;
            this.secondsToNextAdhan = secondsToNextAdhan;
            this.secondsToIqama = secondsToIqama;
            this.state = state;
            this.currentPrayer = currentPrayer;
        }
    }

    public interface Listener {
        default void onTick(TickSnapshot snapshot) {}
        default void onAdhan(PrayerType prayer) {}
        default void onIqama(PrayerType prayer) {}
        default void onDayChanged(LocalDate newDay) {}
    }

    private final AppSettings settings;
    private final PrayerTimeCalculator calculator;

    private DailyPrayerTimes today;
    private DailyPrayerTimes tomorrow;

    private State state = State.TO_ADHAN;

    private PrayerType pendingPrayer;         // adhan fired, awaiting iqama
    private LocalTime pendingIqamaTime;
    private boolean adhanFired = false;
    private boolean iqamaFired = false;

    private final Timer swingTimer;
    private Listener listener;

    public PrayerScheduler(AppSettings settings, PrayerTimeCalculator calculator) {
        this.settings = settings;
        this.calculator = calculator;
        recompute();
        this.swingTimer = new Timer(1000, e -> tick());
        this.swingTimer.start();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public DailyPrayerTimes getToday() {
        return today;
    }

    public DailyPrayerTimes getTomorrow() {
        return tomorrow;
    }

    public State getState() {
        return state;
    }

    public void stop() {
        swingTimer.stop();
    }

    /** Recomputes times from settings (called after settings change). */
    public void recompute() {
        LocalDate now = LocalDate.now();
        this.today = computeDay(now);
        this.tomorrow = computeDay(now.plusDays(1));
        this.pendingPrayer = null;
        this.pendingIqamaTime = null;
        this.adhanFired = false;
        this.iqamaFired = false;
        this.state = State.TO_ADHAN;
    }

    private DailyPrayerTimes computeDay(LocalDate date) {
        Location loc = settings.getLocation();
        double lat = loc != null ? loc.getLatitude() : 33.5731;
        double lon = loc != null ? loc.getLongitude() : -7.5898;
        double tz = resolveTimezone(loc);

        var raw = calculator.compute(date, lat, lon, tz,
                settings.getCalculationMethod(), settings.getAsrMethod());

        IqamaSettings iqama = settings.getIqamaSettings();
        DailyPrayerTimes day = new DailyPrayerTimes(date);
        for (PrayerType type : PrayerType.values()) {
            LocalTime adhan = raw.get(type);
            if (adhan == null) continue;

            int adjust = settings.getAdjust(type);
            if (adjust != 0) adhan = adhan.plusMinutes(adjust);

            LocalTime iqamaTime = null;
            if (iqama.hasIqama(type)) {
                if (iqama.getMode(type) == IqamaSettings.IqamaMode.FIXED && iqama.getFixedTime(type) != null) {
                    iqamaTime = iqama.getFixedTime(type);
                } else {
                    iqamaTime = adhan.plusMinutes(iqama.getOffsetMinutes(type));
                }
            }
            day.put(type, new PrayerEntry(type, adhan, iqamaTime));
        }
        return day;
    }

    /** Location timezone if set, otherwise the system default offset. */
    static double resolveTimezone(Location loc) {
        if (loc != null && loc.getTimezone() != 0.0) return loc.getTimezone();
        ZoneId zone = ZoneId.systemDefault();
        return ZoneOffset.from(zone.getRules().getOffset(Instant.now())).getTotalSeconds() / 3600.0;
    }

    // ------------------------------------------------------------- ticking

    private void tick() {
        LocalDate nowDate = LocalDate.now();
        if (!nowDate.equals(today.getDate())) {
            recompute();
            if (listener != null) listener.onDayChanged(nowDate);
        }

        LocalTime now = LocalTime.now();
        evaluate(now);
        if (listener != null) listener.onTick(snapshot());
    }

    private void evaluate(LocalTime now) {
        // 1) Adhan firing
        if (!adhanFired) {
            PrayerType atAdhan = prayerAtAdhanTime(today, now);
            if (atAdhan != null) {
                adhanFired = true;
                iqamaFired = false;
                pendingPrayer = atAdhan;
                PrayerEntry entry = today.get(atAdhan);
                pendingIqamaTime = entry != null ? entry.getIqamaTime() : null;
                state = State.ADHAN_PLAYING;
                if (listener != null) listener.onAdhan(atAdhan);
            }
        }

        // 2) Iqama countdown / firing
        if (adhanFired && !iqamaFired && pendingIqamaTime != null) {
            long secs = secondsBetween(now, pendingIqamaTime);
            if (secs <= 0) {
                iqamaFired = true;
                state = State.PRAYING;
                if (listener != null) listener.onIqama(pendingPrayer);
            } else {
                state = State.IQAMA_COUNTDOWN;
            }
        }

        // 3) Release the "praying" state after the dim window
        if (adhanFired && (iqamaFired || pendingIqamaTime == null)) {
            LocalTime releaseBase = pendingIqamaTime != null
                    ? pendingIqamaTime
                    : today.get(pendingPrayer) != null ? today.get(pendingPrayer).getAdhanTime() : null;
            boolean done = releaseBase == null
                    || now.isAfter(releaseBase.plusMinutes(dimMinutes()));
            if (done && !nextPrayerIsImminent(now)) {
                adhanFired = false;
                iqamaFired = false;
                pendingPrayer = null;
                pendingIqamaTime = null;
                state = State.TO_ADHAN;
            }
        }

        // 4) Next upcoming prayer: today's first remaining, else tomorrow's Fajr
        //    (recomputed each tick so it is always consistent)
    }

    private boolean nextPrayerIsImminent(LocalTime now) {
        var next = findNext(now);
        if (next == null) return false;
        PrayerEntry e = next.entry();
        return e != null && e.getAdhanTime() != null
                && secondsBetween(now, e.getAdhanTime()) <= 120;
    }

    private record NextPrayer(DailyPrayerTimes day, PrayerType type, PrayerEntry entry) {}

    /** First prayer today whose adhan is still ahead, else tomorrow's Fajr. */
    private NextPrayer findNext(LocalTime now) {
        for (PrayerType type : PrayerType.values()) {
            PrayerEntry e = today.get(type);
            if (e != null && e.getAdhanTime() != null && e.getAdhanTime().isAfter(now)) {
                return new NextPrayer(today, type, e);
            }
        }
        PrayerEntry fajr = tomorrow.get(PrayerType.FAJR);
        if (fajr != null) return new NextPrayer(tomorrow, PrayerType.FAJR, fajr);
        return null;
    }

    private int dimMinutes() {
        return Math.max(0, settings.getPrayerDimMinutes());
    }

    /** The prayer whose adhan time is within [now, now+1s). */
    private PrayerType prayerAtAdhanTime(DailyPrayerTimes day, LocalTime now) {
        for (PrayerType type : PrayerType.values()) {
            PrayerEntry e = day.get(type);
            if (e == null || e.getAdhanTime() == null) continue;
            LocalTime adhan = e.getAdhanTime();
            if (!now.isBefore(adhan) && now.isBefore(adhan.plusSeconds(1))) {
                return type;
            }
        }
        return null;
    }

    private static long secondsBetween(LocalTime from, LocalTime to) {
        return Duration.between(from, to).getSeconds();
    }

    private TickSnapshot snapshot() {
        LocalTime now = LocalTime.now();
        LocalTime nextAdhan = null;
        LocalTime nextIqama = null;
        long toAdhan = -1;

        NextPrayer next = findNext(now);
        if (next != null && next.entry() != null) {
            nextAdhan = next.entry().getAdhanTime();
            nextIqama = next.entry().getIqamaTime();
            if (nextAdhan != null) {
                if (next.day() == today) {
                    toAdhan = Math.max(0, secondsBetween(now, nextAdhan));
                } else {
                    // Tomorrow's Fajr: remaining today + Fajr's own seconds
                    toAdhan = secondsBetween(now, LocalTime.MAX)
                            + 1 + secondsBetween(LocalTime.MIDNIGHT, nextAdhan);
                }
            }
        }

        long toIqama = -1;
        if (state == State.IQAMA_COUNTDOWN && pendingIqamaTime != null) {
            toIqama = Math.max(0, secondsBetween(now, pendingIqamaTime));
        }

        return new TickSnapshot(LocalDate.now(),
                next != null ? next.type() : null,
                nextAdhan, nextIqama, toAdhan, toIqama, state, pendingPrayer);
    }
}
