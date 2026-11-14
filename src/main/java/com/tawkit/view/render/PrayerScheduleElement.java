package com.tawkit.view.render;

import com.tawkit.model.prayer.DailyPrayerTimes;
import com.tawkit.model.prayer.PrayerType;
import com.tawkit.service.format.TimeText;
import com.tawkit.service.weather.TemperatureCurve;
import com.tawkit.view.TawkitTheme;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

/**
 * Bottom six-column prayer schedule: interpolated temperature (golden),
 * prayer name (serif) and adhan time (large bold white).
 */
public final class PrayerScheduleElement implements DashboardElement {

    private DailyPrayerTimes prayerTimes;
    private boolean use24 = false;
    private boolean hasWeather = false;
    private double dailyMin = Double.NaN;
    private double dailyMax = Double.NaN;

    @Override
    public void render(Graphics2D g2, RenderContext ctx) {
        float vw = (float) ctx.vw();
        PrayerType[] types = PrayerType.values();
        double colWidth = 96.0 / types.length;

        for (int i = 0; i < types.length; i++) {
            double colCenter = 2.0 * ctx.vw() + (i + 0.5) * colWidth * ctx.vw();
            PrayerType type = types[i];

            // Temperature — small, golden
            g2.setFont(TawkitTheme.serif(Font.PLAIN, 1.7f * vw));
            TextDraw.drawShadowed(g2, temperatureOf(type), colCenter, (float) (61.0 * ctx.vh()),
                    TextDraw.Anchor.CENTER, TawkitTheme.GOLDEN_BEIGE, 0.1f * vw);

            // Name — serif, warm white
            g2.setFont(TawkitTheme.serif(Font.PLAIN, 2.3f * vw));
            TextDraw.drawShadowed(g2, displayName(type), colCenter, (float) (65.5 * ctx.vh()),
                    TextDraw.Anchor.CENTER, TawkitTheme.WARM_WHITE, 0.12f * vw);

            // Time — large bold white
            g2.setFont(TawkitTheme.serif(Font.BOLD, 4.2f * vw));
            TextDraw.drawShadowed(g2, timeOf(type), colCenter, (float) (71.5 * ctx.vh()),
                    TextDraw.Anchor.CENTER, Color.WHITE, 0.18f * vw);
        }
    }

    private String displayName(PrayerType type) {
        return switch (type) {
            case FAJR -> "Fajr";
            case SHURUQ -> "Sunrise";
            case DHUHR -> "Dhuhr";
            case ASR -> "Asr";
            case MAGHRIB -> "Maghrib";
            case ISHA -> "Isha";
        };
    }

    private String timeOf(PrayerType type) {
        if (prayerTimes == null) return "--:--";
        var entry = prayerTimes.get(type);
        if (entry == null || entry.getAdhanTime() == null) return "--:--";
        return TimeText.prayerTime(entry.getAdhanTime(), use24);
    }

    /** Interpolated forecast temperature at this prayer's adhan time. */
    private String temperatureOf(PrayerType type) {
        if (!hasWeather || Double.isNaN(dailyMin) || Double.isNaN(dailyMax)) return "";
        if (prayerTimes == null || prayerTimes.get(type) == null
                || prayerTimes.get(type).getAdhanTime() == null) {
            return "";
        }
        double temp = TemperatureCurve.at(dailyMin, dailyMax, prayerTimes.get(type).getAdhanTime());
        return Math.round(temp) + "°";
    }

    public void setPrayerTimes(DailyPrayerTimes times) {
        this.prayerTimes = times;
    }

    public void setClockMode(boolean use24) {
        this.use24 = use24;
    }

    public void setDailyTemps(double min, double max) {
        this.dailyMin = min;
        this.dailyMax = max;
    }

    public void setHasWeather(boolean hasWeather) {
        this.hasWeather = hasWeather;
    }
}
