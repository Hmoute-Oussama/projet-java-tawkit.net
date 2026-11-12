package com.tawkit.model.settings;

import com.tawkit.model.prayer.AsrMethod;
import com.tawkit.model.prayer.CalculationMethod;
import com.tawkit.model.prayer.IqamaSettings;
import com.tawkit.model.prayer.Location;

import java.util.ArrayList;
import java.util.List;

/**
 * Central configuration for the Tawkit desktop app. Persisted to disk.
 */
public class AppSettings {

    private Location location;
    private CalculationMethod calculationMethod = CalculationMethod.MUSLIM_WORLD_LEAGUE;
    private AsrMethod asrMethod = AsrMethod.STANDARD;
    private IqamaSettings iqamaSettings = new IqamaSettings();

    private String mosqueName = "";
    private String language = "en";
    private boolean adhanEnabled = true;
    private boolean screenDimDuringPrayer = true;

    /** How many minutes after iqama the "praying" (dimmed) state lasts. */
    private int prayerDimMinutes = 10;
    private int hijriOffsetDays = 0;

    // ---- display ----
    private boolean use24HourClock = true;
    private boolean showSeconds = true;
    private boolean weatherEnabled = true;
    private int contentRotationSeconds = 18;
    private boolean slidesEnabled = true;

    // ---- audio ----
    private double audioVolume = 1.0;
    private boolean preFajrAlertEnabled = true;
    private int preFajrAlertMinutes = 10;

    // ---- background: "night" | "green" | "gradient-blue" | "custom" ----
    private String backgroundTheme = "night";
    private String customBackgroundPath = "";

    // ---- user content ----
    private List<String> announcements = new ArrayList<>();
    private List<String> reminders = new ArrayList<>();

    // Manual adjustments in minutes per prayer (+/-)
    private int fajrAdjust = 0;
    private int shuruqAdjust = 0;
    private int dhuhrAdjust = 0;
    private int asrAdjust = 0;
    private int maghribAdjust = 0;
    private int ishaAdjust = 0;

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public CalculationMethod getCalculationMethod() {
        return calculationMethod;
    }

    public void setCalculationMethod(CalculationMethod calculationMethod) {
        this.calculationMethod = calculationMethod;
    }

    public AsrMethod getAsrMethod() {
        return asrMethod;
    }

    public void setAsrMethod(AsrMethod asrMethod) {
        this.asrMethod = asrMethod;
    }

    public IqamaSettings getIqamaSettings() {
        return iqamaSettings;
    }

    public void setIqamaSettings(IqamaSettings iqamaSettings) {
        this.iqamaSettings = iqamaSettings;
    }

    public String getMosqueName() {
        return mosqueName;
    }

    public void setMosqueName(String mosqueName) {
        this.mosqueName = mosqueName;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public boolean isAdhanEnabled() {
        return adhanEnabled;
    }

    public void setAdhanEnabled(boolean adhanEnabled) {
        this.adhanEnabled = adhanEnabled;
    }

    public boolean isScreenDimDuringPrayer() {
        return screenDimDuringPrayer;
    }

    public void setScreenDimDuringPrayer(boolean screenDimDuringPrayer) {
        this.screenDimDuringPrayer = screenDimDuringPrayer;
    }

    public int getPrayerDimMinutes() {
        return prayerDimMinutes;
    }

    public void setPrayerDimMinutes(int prayerDimMinutes) {
        this.prayerDimMinutes = prayerDimMinutes;
    }

    public int getHijriOffsetDays() {
        return hijriOffsetDays;
    }

    public void setHijriOffsetDays(int hijriOffsetDays) {
        this.hijriOffsetDays = hijriOffsetDays;
    }

    public int getAdjust(com.tawkit.model.prayer.PrayerType type) {
        return switch (type) {
            case FAJR -> fajrAdjust;
            case SHURUQ -> shuruqAdjust;
            case DHUHR -> dhuhrAdjust;
            case ASR -> asrAdjust;
            case MAGHRIB -> maghribAdjust;
            case ISHA -> ishaAdjust;
        };
    }

    public void setAdjust(com.tawkit.model.prayer.PrayerType type, int minutes) {
        switch (type) {
            case FAJR -> fajrAdjust = minutes;
            case SHURUQ -> shuruqAdjust = minutes;
            case DHUHR -> dhuhrAdjust = minutes;
            case ASR -> asrAdjust = minutes;
            case MAGHRIB -> maghribAdjust = minutes;
            case ISHA -> ishaAdjust = minutes;
        }
    }

    public boolean isUse24HourClock() {
        return use24HourClock;
    }

    public void setUse24HourClock(boolean use24HourClock) {
        this.use24HourClock = use24HourClock;
    }

    public boolean isShowSeconds() {
        return showSeconds;
    }

    public void setShowSeconds(boolean showSeconds) {
        this.showSeconds = showSeconds;
    }

    public boolean isWeatherEnabled() {
        return weatherEnabled;
    }

    public void setWeatherEnabled(boolean weatherEnabled) {
        this.weatherEnabled = weatherEnabled;
    }

    public int getContentRotationSeconds() {
        return contentRotationSeconds;
    }

    public void setContentRotationSeconds(int contentRotationSeconds) {
        this.contentRotationSeconds = contentRotationSeconds;
    }

    public boolean isSlidesEnabled() {
        return slidesEnabled;
    }

    public void setSlidesEnabled(boolean slidesEnabled) {
        this.slidesEnabled = slidesEnabled;
    }

    public double getAudioVolume() {
        return audioVolume;
    }

    public void setAudioVolume(double audioVolume) {
        this.audioVolume = audioVolume;
    }

    public boolean isPreFajrAlertEnabled() {
        return preFajrAlertEnabled;
    }

    public void setPreFajrAlertEnabled(boolean preFajrAlertEnabled) {
        this.preFajrAlertEnabled = preFajrAlertEnabled;
    }

    public int getPreFajrAlertMinutes() {
        return preFajrAlertMinutes;
    }

    public void setPreFajrAlertMinutes(int preFajrAlertMinutes) {
        this.preFajrAlertMinutes = preFajrAlertMinutes;
    }

    public String getBackgroundTheme() {
        return backgroundTheme;
    }

    public void setBackgroundTheme(String backgroundTheme) {
        this.backgroundTheme = backgroundTheme;
    }

    public String getCustomBackgroundPath() {
        return customBackgroundPath;
    }

    public void setCustomBackgroundPath(String customBackgroundPath) {
        this.customBackgroundPath = customBackgroundPath;
    }

    public List<String> getAnnouncements() {
        return announcements;
    }

    public void setAnnouncements(List<String> announcements) {
        this.announcements = announcements != null ? new ArrayList<>(announcements) : new ArrayList<>();
    }

    public List<String> getReminders() {
        return reminders;
    }

    public void setReminders(List<String> reminders) {
        this.reminders = reminders != null ? new ArrayList<>(reminders) : new ArrayList<>();
    }
}
