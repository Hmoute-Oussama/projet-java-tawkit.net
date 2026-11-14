package com.tawkit.service.weather;

import java.time.LocalTime;

/**
 * Approximates the temperature at any moment of the day from the forecast's
 * daily minimum/maximum, using a simple solar curve (min near dawn, max
 * mid-afternoon). Keeps the interpolation out of the view layer.
 */
public final class TemperatureCurve {

    private TemperatureCurve() {
    }

    /**
     * Estimated temperature at {@code time}, between {@code min} and
     * {@code max} of the day.
     */
    public static double at(double min, double max, LocalTime time) {
        double hour = time.getHour() + time.getMinute() / 60.0;
        // Solar phase: 0 at dawn (5h), 1 at peak (15h)
        double phase = (hour - 5.0) / 10.0;
        if (phase < 0) phase = Math.max(-0.25, phase / 4.0);       // pre-dawn dips slightly below min
        if (phase > 1) phase = Math.max(0, 1 - (phase - 1) / 4.0); // evening cools
        double f = 0.5 - 0.5 * Math.cos(Math.PI * Math.max(0, Math.min(1, phase)));
        return min + (max - min) * f;
    }
}
