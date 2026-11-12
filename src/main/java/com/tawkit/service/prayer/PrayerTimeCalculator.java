package com.tawkit.service.prayer;

import com.tawkit.model.prayer.AsrMethod;
import com.tawkit.model.prayer.CalculationMethod;
import com.tawkit.model.prayer.PrayerType;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.Map;

/**
 * Astronomical prayer-times calculation engine, based on the well-known
 * PrayTimes.org algorithm. Pure Java — works fully offline.
 *
 * All internal computations use hours as doubles (0..24), angles in degrees.
 */
public final class PrayerTimeCalculator {

    /** Elevation in meters above sea level (affects sunrise/sunset angle). */
    private double elevation = 0.0;

    /** Extra minutes added to Dhuhr (midday) for safety, standard practice. */
    private static final int DHUHR_MINUTES = 1;

    public PrayerTimeCalculator() {
    }

    public PrayerTimeCalculator withElevation(double meters) {
        this.elevation = meters;
        return this;
    }

    /**
     * Computes the five prayers + sunrise for the given date and location.
     *
     * @param date       civil date
     * @param latitude   degrees, north positive
     * @param longitude  degrees, east positive
     * @param tzHours    UTC offset in hours for the location
     * @param method     Fajr/Isha definition
     * @param asrMethod  shadow factor for Asr
     * @return times keyed by prayer; a value is null when not computable (polar regions)
     */
    public Map<PrayerType, LocalTime> compute(LocalDate date, double latitude, double longitude,
                                              double tzHours, CalculationMethod method, AsrMethod asrMethod) {
        double jDate = julianDay(date) - longitude / (15.0 * 24.0);

        Map<PrayerType, Double> times = new EnumMap<>(PrayerType.class);
        times.put(PrayerType.FAJR, 5.0);
        times.put(PrayerType.SHURUQ, 6.0);
        times.put(PrayerType.DHUHR, 12.0);
        times.put(PrayerType.ASR, 13.0);
        times.put(PrayerType.MAGHRIB, 18.0);
        times.put(PrayerType.ISHA, 18.0);

        // Iterate to converge each time on its own sun position
        for (int i = 0; i < 2; i++) {
            times.put(PrayerType.FAJR,
                    sunAngleTime(method.getFajrAngle(), times.get(PrayerType.FAJR), latitude, jDate, true));
            times.put(PrayerType.SHURUQ,
                    sunAngleTime(riseSetAngle(), times.get(PrayerType.SHURUQ), latitude, jDate, true));
            times.put(PrayerType.DHUHR, midDay(times.get(PrayerType.DHUHR), jDate));
            times.put(PrayerType.ASR, asrTime(asrMethod.getShadowFactor(), times.get(PrayerType.ASR), latitude, jDate));
            double sunset = sunAngleTime(riseSetAngle(), times.get(PrayerType.MAGHRIB), latitude, jDate, false);
            double maghrib = method.hasMaghribAngle()
                    ? sunAngleTime(method.getMaghribAngle(), times.get(PrayerType.MAGHRIB), latitude, jDate, false)
                    : sunset;
            times.put(PrayerType.MAGHRIB, maghrib);
            times.put(PrayerType.ISHA, method.hasIshaInterval()
                    ? maghrib + method.getIshaInterval() / 60.0
                    : sunAngleTime(method.getIshaAngle(), times.get(PrayerType.ISHA), latitude, jDate, false));
        }

        adjustHighLats(times, latitude, method);

        // Shift from solar to clock time
        double offset = tzHours - longitude / 15.0;
        for (Map.Entry<PrayerType, Double> e : times.entrySet()) {
            Double v = e.getValue();
            if (v == null || v.isNaN()) {
                e.setValue(null);
            } else {
                double adjusted = fixHour(v + offset);
                if (e.getKey() == PrayerType.DHUHR) adjusted = fixHour(adjusted + DHUHR_MINUTES / 60.0);
                e.setValue(adjusted);
            }
        }

        Map<PrayerType, LocalTime> result = new EnumMap<>(PrayerType.class);
        for (Map.Entry<PrayerType, Double> e : times.entrySet()) {
            result.put(e.getKey(), e.getValue() == null ? null : hoursToLocalTime(e.getValue()));
        }
        return result;
    }

    // ---------------------------------------------------------------- core

    private double midDay(double t, double jDate) {
        double eqt = sunPosition(jDate + t).equationOfTime;
        return fixHour(12.0 - eqt);
    }

    private double sunAngleTime(double angle, double t, double lat, double jDate, boolean beforeNoon) {
        SunPosition pos = sunPosition(jDate + t);
        double decl = pos.declination;
        double noon = fixHour(12.0 - pos.equationOfTime);
        double cosH = (-sin(angle) - sin(decl) * sin(lat)) / (cos(decl) * cos(lat));
        if (cosH > 1 || cosH < -1) return Double.NaN; // sun never reaches angle
        double t2 = (1.0 / 15.0) * acos(cosH);
        return noon + (beforeNoon ? -t2 : t2);
    }

    private double asrTime(double shadowFactor, double t, double lat, double jDate) {
        double decl = sunPosition(jDate + t).declination;
        double angle = -arcCot(shadowFactor + tan(abs(lat - decl)));
        return sunAngleTime(angle, t, lat, jDate, false);
    }

    private double riseSetAngle() {
        return 0.833 + 0.034 * Math.sqrt(Math.max(0, elevation));
    }

    // ------------------------------------------------- high latitude fix

    private void adjustHighLats(Map<PrayerType, Double> times, double lat, CalculationMethod method) {
        double sunset = times.get(PrayerType.MAGHRIB);
        double sunrise = times.get(PrayerType.SHURUQ);
        if (Double.isNaN(sunset) || Double.isNaN(sunrise)) return;

        double night = timeDiff(sunset, sunrise); // sunset -> sunrise next morning

        times.put(PrayerType.FAJR,
                adjustHLTime(times.get(PrayerType.FAJR), sunrise, method.getFajrAngle(), night, true));
        times.put(PrayerType.ISHA,
                adjustHLTime(times.get(PrayerType.ISHA), sunset, method.getIshaAngle(), night, false));
    }

    private double adjustHLTime(double time, double base, double angle, double night, boolean beforeNoon) {
        if (Double.isNaN(time)) {
            return beforeNoon ? fixHour(base - nightPortion(angle, night))
                              : fixHour(base + nightPortion(angle, night));
        }
        double diff = beforeNoon ? timeDiff(time, base) : timeDiff(base, time);
        if (diff > nightPortion(angle, night)) {
            return beforeNoon ? fixHour(base - nightPortion(angle, night))
                              : fixHour(base + nightPortion(angle, night));
        }
        return time;
    }

    /** Angle-based portion of the night used for Fajr/Isha in extreme latitudes. */
    private double nightPortion(double angle, double night) {
        return (angle / 60.0) * night;
    }

    private static double timeDiff(double a, double b) {
        // time from a to b, forward (0..24)
        double d = b - a;
        if (d < 0) d += 24.0;
        return d;
    }

    // ------------------------------------------------------------ sun math

    private static final class SunPosition {
        final double declination;      // degrees
        final double equationOfTime;   // hours

        SunPosition(double declination, double equationOfTime) {
            this.declination = declination;
            this.equationOfTime = equationOfTime;
        }
    }

    private SunPosition sunPosition(double jd) {
        double d = jd - 2451545.0;
        double g = fixAngle(357.529 + 0.98560028 * d);
        double q = fixAngle(280.459 + 0.98564736 * d);
        double l = fixAngle(q + 1.915 * sin(g) + 0.020 * sin(2 * g));
        double e = 23.439 - 0.00000036 * d;
        double decl = arcsin(sin(e) * sin(l));
        double ra = atan2(cos(e) * sin(l), cos(l)) / 15.0;
        ra = fixHour(ra);
        double eqt = q / 15.0 - ra;
        return new SunPosition(decl, eqt);
    }

    private static double julianDay(LocalDate date) {
        int y = date.getYear();
        int m = date.getMonthValue();
        int d = date.getDayOfMonth();
        if (m <= 2) {
            y -= 1;
            m += 12;
        }
        double a = Math.floor(y / 100.0);
        double b = 2 - a + Math.floor(a / 4.0);
        return Math.floor(365.25 * (y + 4716)) + Math.floor(30.6001 * (m + 1)) + d + b - 1524.5;
    }

    // ------------------------------------------------------------- helpers

    private static LocalTime hoursToLocalTime(double hours) {
        int totalSeconds = (int) Math.round(hours * 3600.0) % 86400;
        if (totalSeconds < 0) totalSeconds += 86400;
        return LocalTime.ofSecondOfDay(totalSeconds);
    }

    private static double fixAngle(double a) {
        a = a - 360.0 * Math.floor(a / 360.0);
        return a < 0 ? a + 360.0 : a;
    }

    private static double fixHour(double h) {
        h = h - 24.0 * Math.floor(h / 24.0);
        return h < 0 ? h + 24.0 : h;
    }

    private static double dtr(double d) { return d * Math.PI / 180.0; }
    private static double rtd(double r) { return r * 180.0 / Math.PI; }

    private static double sin(double d)  { return Math.sin(dtr(d)); }
    private static double cos(double d)  { return Math.cos(dtr(d)); }
    private static double tan(double d)  { return Math.tan(dtr(d)); }
    private static double arcsin(double x) { return rtd(Math.asin(x)); }
    private static double acos(double x)   { return rtd(Math.acos(x)); }
    private static double atan2(double y, double x) { return rtd(Math.atan2(y, x)); }
    private static double arcCot(double x) { return rtd(Math.atan(1.0 / x)); }
    private static double abs(double v) { return Math.abs(v); }
}
