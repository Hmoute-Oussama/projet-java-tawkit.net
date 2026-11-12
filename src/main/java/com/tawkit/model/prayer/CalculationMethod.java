package com.tawkit.model.prayer;

/**
 * Prayer-time calculation methods.
 * Each method defines the angles (in degrees below the horizon) used for
 * Fajr and Isha, or a fixed Isha interval after Maghrib.
 *
 * Values are based on the well-known PrayTimes.org specification.
 */
public enum CalculationMethod {

    MUSLIM_WORLD_LEAGUE("Muslim World League", 18.0, 17.0, 0, 0),
    ISNA("Islamic Society of North America", 15.0, 15.0, 0, 0),
    EGYPTIAN("Egyptian General Authority", 19.5, 17.5, 0, 0),
    MAKKAH("Umm al-Qura, Makkah", 18.5, 0.0, 0, 90),
    KARACHI("University of Karachi", 18.0, 18.0, 0, 0),
    TEHRAN("Institute of Geophysics, Tehran", 17.7, 14.0, 4.5, 0),
    JAFARI("Shia Ithna-Ashari (Jafari)", 16.0, 14.0, 4.0, 0);

    private final String displayName;
    private final double fajrAngle;
    private final double ishaAngle;
    private final double maghribAngle;   // 0 means use sunset
    private final int ishaInterval;       // minutes after Maghrib (0 = use angle)

    CalculationMethod(String displayName, double fajrAngle, double ishaAngle,
                      double maghribAngle, int ishaInterval) {
        this.displayName = displayName;
        this.fajrAngle = fajrAngle;
        this.ishaAngle = ishaAngle;
        this.maghribAngle = maghribAngle;
        this.ishaInterval = ishaInterval;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getFajrAngle() {
        return fajrAngle;
    }

    public double getIshaAngle() {
        return ishaAngle;
    }

    public double getMaghribAngle() {
        return maghribAngle;
    }

    public int getIshaInterval() {
        return ishaInterval;
    }

    public boolean hasIshaInterval() {
        return ishaInterval > 0;
    }

    public boolean hasMaghribAngle() {
        return maghribAngle > 0;
    }
}
