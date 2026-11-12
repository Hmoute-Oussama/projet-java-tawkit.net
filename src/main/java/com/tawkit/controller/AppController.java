package com.tawkit.controller;

import com.tawkit.model.content.ContentItem;
import com.tawkit.model.content.Slide;
import com.tawkit.model.prayer.DailyPrayerTimes;
import com.tawkit.model.prayer.PrayerEntry;
import com.tawkit.model.prayer.PrayerType;
import com.tawkit.model.settings.AppSettings;
import com.tawkit.model.weather.WeatherData;
import com.tawkit.service.audio.AudioService;
import com.tawkit.service.content.ContentService;
import com.tawkit.service.prayer.HijriCalendarService;
import com.tawkit.service.prayer.PrayerScheduler;
import com.tawkit.service.prayer.PrayerTimeCalculator;
import com.tawkit.service.scheduler.ReminderService;
import com.tawkit.service.settings.SettingsService;
import com.tawkit.service.weather.WeatherService;
import com.tawkit.view.MainFrame;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Central controller: loads settings, wires the scheduler, audio, weather,
 * content and reminders services to the MainFrame.
 */
public class AppController implements PrayerScheduler.Listener, WeatherService.Listener {

    private final MainFrame frame;
    private final SettingsService settingsService = new SettingsService();
    private final AppSettings settings;
    private final AudioService audio = new AudioService();
    private final ContentService content = new ContentService();
    private final HijriCalendarService hijri = new HijriCalendarService();
    private final PrayerTimeCalculator calculator = new PrayerTimeCalculator();
    private final ReminderService reminders = new ReminderService();
    private final WeatherService weatherService = new WeatherService(this);

    private PrayerScheduler scheduler;
    private boolean showingAzkar = false;
    private boolean preFajrAlertFired = false;

    public AppController(MainFrame frame) {
        this.frame = frame;
        this.settings = settingsService.load();
        settingsService.save(settings); // normalize defaults to disk
    }

    public AppSettings getSettings() {
        return settings;
    }

    /** Starts the whole application. */
    public void start() {
        applySettingsToFrame();
        frame.setPrayerTimes(scheduler.getToday());

        scheduler = new PrayerScheduler(settings, calculator);
        scheduler.setListener(this);

        if (settings.isWeatherEnabled() && settings.getLocation() != null) {
            weatherService.refreshAsync(settings.getLocation().getLatitude(),
                    settings.getLocation().getLongitude());
        }
    }

    /** Called after the settings dialog was saved. */
    public void settingsChanged() {
        settingsService.save(settings);
        applySettingsToFrame();
        if (scheduler != null) {
            scheduler.recompute();
            frame.setPrayerTimes(scheduler.getToday());
        }
        if (settings.isWeatherEnabled() && settings.getLocation() != null) {
            weatherService.refreshAsync(settings.getLocation().getLatitude(),
                    settings.getLocation().getLongitude());
        }
    }

    private void applySettingsToFrame() {
        com.tawkit.service.localization.Messages.setLanguage(settings.getLanguage());
        frame.setTitle(com.tawkit.service.localization.Messages.get("app.title"));
        frame.getPrayerTimesPanel().refreshNames();
        frame.setMosqueName(settings.getMosqueName());
        frame.setUse24Hour(settings.isUse24HourClock());
        frame.setShowSeconds(settings.isShowSeconds());
        frame.setLanguage(settings.getLanguage());

        hijri.setOffsetDays(settings.getHijriOffsetDays());
        updateDates();

        if (settings.getBackgroundTheme().equals("custom")
                && !settings.getCustomBackgroundPath().isBlank()) {
            frame.applyCustomBackground(settings.getCustomBackgroundPath());
        } else {
            frame.applyTheme(settings.getBackgroundTheme());
        }

        frame.setTickerMessages(buildTickerMessages());
        frame.setContentItems(buildDefaultContent());
        frame.setRotationSeconds(settings.getContentRotationSeconds());
        frame.setSlides(settings.isSlidesEnabled() ? content.getSlides() : List.of());

        audio.setEnabled(settings.isAdhanEnabled());
        audio.setVolume(settings.getAudioVolume());
        reminders.setRules(settings.getReminders());
    }

    private void updateDates() {
        LocalDate today = LocalDate.now();
        frame.setGregorianDate(today);
        frame.setHijriDate(hijri.toHijri(today));
    }

    private List<String> buildTickerMessages() {
        List<String> msgs = new ArrayList<>(content.getAnnouncements());
        if (!settings.getAnnouncements().isEmpty()) {
            msgs.addAll(settings.getAnnouncements());
        }
        long daysToRamadan = hijri.daysUntilRamadan(LocalDate.now());
        if (daysToRamadan > 0 && daysToRamadan < 60) {
            msgs.add("Ramadan starts in " + daysToRamadan + " day(s)");
        }
        if (HijriCalendarService.isFastingDay(LocalDate.now())) {
            msgs.add("Fasting day: Monday / Thursday");
        }
        return msgs;
    }

    private List<ContentItem> buildDefaultContent() {
        List<ContentItem> items = new ArrayList<>();
        items.addAll(content.getVerses());
        items.addAll(content.getHadiths());
        items.addAll(content.getAdhkar());
        return items;
    }

    // ------------------------------------------------- scheduler callbacks

    @Override
    public void onTick(PrayerScheduler.TickSnapshot snapshot) {
        // Prayer grid: highlight next, soften past
        frame.setPrayerTimes(scheduler.getToday());
        frame.setHighlightedPrayer(snapshot.nextPrayer);

        if (snapshot.state == PrayerScheduler.State.IQAMA_COUNTDOWN && snapshot.secondsToIqama >= 0) {
            frame.setIqamaCountdown("Iqama in " + formatMMSS(snapshot.secondsToIqama));
        } else {
            frame.setIqamaCountdown("");
        }

        // Next-prayer ring
        String name = snapshot.nextPrayer != null ? snapshot.nextPrayer.getDisplayName() : "";
        String countdown = snapshot.secondsToNextAdhan >= 0
                ? formatHHMMSS(snapshot.secondsToNextAdhan) : "--:--:--";
        String iqamaInfo = snapshot.state == PrayerScheduler.State.IQAMA_COUNTDOWN
                ? "Iqama " + formatMMSS(snapshot.secondsToIqama) : "";
        frame.setNextPrayer(name, countdown, ringProgress(snapshot), iqamaInfo);

        // Dimming during prayer
        boolean dim = settings.isScreenDimDuringPrayer()
                && snapshot.state != PrayerScheduler.State.TO_ADHAN;
        frame.setDimmed(dim);

        // Reminders
        for (String message : reminders.check(LocalTime.now(), snapshot.date, scheduler.getToday())) {
            frame.showAlertMessage(message);
            audio.playAlert();
        }

        // Built-in pre-Fajr alert
        if (settings.isPreFajrAlertEnabled() && snapshot.nextPrayer == PrayerType.FAJR
                && snapshot.secondsToNextAdhan >= 0) {
            long target = settings.getPreFajrAlertMinutes() * 60L;
            if (snapshot.secondsToNextAdhan <= target && snapshot.secondsToNextAdhan > target - 60) {
                if (!preFajrAlertFired) {
                    preFajrAlertFired = true;
                    frame.showAlertMessage("Fajr in "
                            + snapshot.secondsToNextAdhan / 60 + " minutes");
                    audio.playAlert();
                }
            } else if (snapshot.secondsToNextAdhan > target + 300) {
                preFajrAlertFired = false;
            }
        }

        // Restore default content once the praying window is over
        if (snapshot.state == PrayerScheduler.State.TO_ADHAN && showingAzkar) {
            showingAzkar = false;
            frame.setContentItems(buildDefaultContent());
        }
    }

    @Override
    public void onAdhan(PrayerType prayer) {
        if (!settings.isAdhanEnabled()) return;
        audio.play(prayer == PrayerType.FAJR
                ? AudioService.Sound.ADHAN_FAJR : AudioService.Sound.ADHAN, prayer);
        // Show azkar after prayer, like Tawkit
        if (!content.getAdhkar().isEmpty()) {
            showingAzkar = true;
            frame.setContentItems(content.getAdhkar());
        }
    }

    @Override
    public void onIqama(PrayerType prayer) {
        if (!settings.isAdhanEnabled()) return;
        audio.play(AudioService.Sound.IQAMA, prayer);
    }

    @Override
    public void onDayChanged(LocalDate newDay) {
        updateDates();
        frame.setTickerMessages(buildTickerMessages());
        if (settings.isWeatherEnabled() && settings.getLocation() != null) {
            weatherService.refreshAsync(settings.getLocation().getLatitude(),
                    settings.getLocation().getLongitude());
        }
    }

    // --------------------------------------------------- weather callbacks

    @Override
    public void onWeatherUpdated(WeatherData data) {
        frame.setWeather(data);
    }

    @Override
    public void onWeatherError(String message) {
        System.err.println("[Weather] " + message);
    }

    // -------------------------------------------------------------- helpers

    /**
     * Fraction of the interval between the previous prayer's adhan and the
     * next prayer's adhan that has already elapsed — drives the ring.
     */
    private double ringProgress(PrayerScheduler.TickSnapshot snapshot) {
        if (snapshot.nextPrayer == null || snapshot.secondsToNextAdhan < 0) return 0;
        PrayerType[] order = PrayerType.values();
        int idx = snapshot.nextPrayer.ordinal();

        DailyPrayerTimes sourceDay = scheduler.getToday();
        LocalTime prevAdhan;
        if (idx == 0) {
            // Fajr: previous is today's Isha
            PrayerEntry isha = sourceDay.get(PrayerType.ISHA);
            if (isha == null || isha.getAdhanTime() == null) return 0;
            prevAdhan = isha.getAdhanTime();
        } else {
            PrayerEntry prev = sourceDay.get(order[idx - 1]);
            if (prev == null || prev.getAdhanTime() == null) return 0;
            prevAdhan = prev.getAdhanTime();
        }

        long interval;
        if (idx == 0) {
            interval = Duration.between(prevAdhan, LocalTime.MAX).getSeconds() + 1
                    + Duration.between(LocalTime.MIDNIGHT, snapshot.nextAdhanTime).getSeconds();
        } else {
            interval = Duration.between(prevAdhan, snapshot.nextAdhanTime).getSeconds();
        }
        if (interval <= 0) return 0;
        double elapsed = interval - snapshot.secondsToNextAdhan;
        return Math.max(0.0, Math.min(1.0, elapsed / interval));
    }

    private static String formatHHMMSS(long totalSeconds) {
        long h = totalSeconds / 3600;
        long m = (totalSeconds % 3600) / 60;
        long s = totalSeconds % 60;
        return String.format("%d:%02d:%02d", h, m, s);
    }

    private static String formatMMSS(long totalSeconds) {
        long m = totalSeconds / 60;
        long s = totalSeconds % 60;
        return String.format("%d:%02d", m, s);
    }

    public ContentService getContentService() {
        return content;
    }
}
