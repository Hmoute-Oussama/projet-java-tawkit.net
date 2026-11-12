package com.tawkit.model.weather;

import java.time.LocalDate;
import java.util.List;

/** Weather snapshot: current conditions + multi-day forecast. */
public class WeatherData {

    private final double currentTemp;
    private final int currentCode;          // WMO weather code
    private final double currentWindKmh;
    private final List<WeatherDay> days;

    public WeatherData(double currentTemp, int currentCode, double currentWindKmh, List<WeatherDay> days) {
        this.currentTemp = currentTemp;
        this.currentCode = currentCode;
        this.currentWindKmh = currentWindKmh;
        this.days = days;
    }

    public double getCurrentTemp() {
        return currentTemp;
    }

    public int getCurrentCode() {
        return currentCode;
    }

    public double getCurrentWindKmh() {
        return currentWindKmh;
    }

    public List<WeatherDay> getDays() {
        return days;
    }

    /** One forecast day. */
    public record WeatherDay(LocalDate date, int code, double minTemp, double maxTemp,
                             double precipitationMm) {
    }

    /**
     * Maps WMO weather codes used by Open-Meteo to a friendly icon glyph
     * (emoji works on all platforms with the Segoe UI Emoji font on Windows).
     */
    public static String iconFor(int code) {
        return switch (code) {
            case 0 -> "☀";          // clear
            case 1, 2 -> "🌤";       // mostly clear / partly cloudy
            case 3 -> "☁";          // overcast
            case 45, 48 -> "🌫";     // fog
            case 51, 53, 55, 56, 57 -> "🌦"; // drizzle
            case 61, 63, 65, 66, 67, 80, 81, 82 -> "🌧"; // rain
            case 71, 73, 75, 77, 85, 86 -> "🌨"; // snow
            case 95, 96, 99 -> "⛈"; // thunderstorm
            default -> "☁";
        };
    }

    public static String descriptionFor(int code) {
        return switch (code) {
            case 0 -> "Clear sky";
            case 1, 2 -> "Partly cloudy";
            case 3 -> "Overcast";
            case 45, 48 -> "Fog";
            case 51, 53, 55, 56, 57 -> "Drizzle";
            case 61, 63, 65, 66, 67 -> "Rain";
            case 80, 81, 82 -> "Showers";
            case 71, 73, 75, 77, 85, 86 -> "Snow";
            case 95, 96, 99 -> "Thunderstorm";
            default -> "Cloudy";
        };
    }
}
