package com.tawkit.service.weather;

import com.tawkit.model.weather.WeatherData;
import com.tawkit.model.weather.WeatherData.WeatherDay;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Weather from Open-Meteo.com (free, no API key), matching Tawkit's provider.
 * Fetches current conditions + 7-day forecast. Runs asynchronously and keeps
 * the last successful result so the app keeps working offline.
 */
public class WeatherService {

    public interface Listener {
        default void onWeatherUpdated(WeatherData data) {}
        default void onWeatherError(String message) {}
    }

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final Listener listener;
    private volatile WeatherData lastData;
    private volatile LocalDate lastFetchDate;

    public WeatherService(Listener listener) {
        this.listener = listener;
    }

    public WeatherData getLastData() {
        return lastData;
    }

    /** Fetches weather for the given coordinates asynchronously. */
    public void refreshAsync(double latitude, double longitude) {
        Thread t = new Thread(() -> refresh(latitude, longitude), "tawkit-weather");
        t.setDaemon(true);
        t.start();
    }

    /** Blocking fetch; returns the data or null on failure. */
    public WeatherData refresh(double latitude, double longitude) {
        try {
            String url = buildUrl(latitude, longitude);
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();
            HttpResponse<String> response =
                    http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                fail("Weather HTTP " + response.statusCode());
                return lastData;
            }
            WeatherData data = parse(response.body());
            lastData = data;
            lastFetchDate = LocalDate.now();
            if (listener != null) listener.onWeatherUpdated(data);
            return data;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            fail(e.getMessage());
            return lastData;
        }
    }

    private void fail(String message) {
        if (listener != null) listener.onWeatherError(message == null ? "weather unavailable" : message);
    }

    static String buildUrl(double lat, double lon) {
        return "https://api.open-meteo.com/v1/forecast?latitude=" + lat
                + "&longitude=" + lon
                + "&current=temperature_2m,weather_code,wind_speed_10m"
                + "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum"
                + "&forecast_days=7&timezone="
                + URLEncoder.encode("auto", StandardCharsets.UTF_8);
    }

    /**
     * Minimal JSON parsing targeted at the Open-Meteo response shape —
     * avoids any third-party dependency.
     */
    static WeatherData parse(String json) {
        double curTemp = extractNumber(json, "temperature_2m");
        int curCode = (int) extractNumber(json, "weather_code");
        double wind = extractNumber(json, "wind_speed_10m");

        String daily = extractObject(json, "daily");
        if (daily == null) return new WeatherData(curTemp, curCode, wind, List.of());

        double[] codes = extractArray(daily, "weather_code");
        double[] max = extractArray(daily, "temperature_2m_max");
        double[] min = extractArray(daily, "temperature_2m_min");
        double[] prec = extractArray(daily, "precipitation_sum");
        List<String> dates = extractStringArray(daily, "time");

        List<WeatherDay> days = new ArrayList<>();
        int n = codes == null ? 0 : codes.length;
        for (int i = 0; i < n; i++) {
            LocalDate date = dates != null && i < dates.size()
                    ? LocalDate.parse(dates.get(i)) : LocalDate.now().plusDays(i);
            days.add(new WeatherDay(date,
                    (int) codes[i],
                    min != null && i < min.length ? min[i] : 0,
                    max != null && i < max.length ? max[i] : 0,
                    prec != null && i < prec.length ? prec[i] : 0));
        }
        return new WeatherData(curTemp, curCode, wind, List.copyOf(days));
    }

    // ------------------------------------------------------------ JSON utils

    private static double extractNumber(String json, String key) {
        int i = json.indexOf("\"" + key + "\"");
        if (i < 0) return 0;
        i = json.indexOf(':', i) + 1;
        StringBuilder sb = new StringBuilder();
        while (i < json.length() && (Character.isDigit(json.charAt(i))
                || json.charAt(i) == '-' || json.charAt(i) == '.' || json.charAt(i) == 'e'
                || json.charAt(i) == 'E' || json.charAt(i) == '+')) {
            sb.append(json.charAt(i++));
        }
        try {
            return Double.parseDouble(sb.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String extractObject(String json, String key) {
        int i = json.indexOf("\"" + key + "\"");
        if (i < 0) return null;
        int start = json.indexOf('{', i);
        int depth = 0;
        for (int j = start; j < json.length(); j++) {
            char c = json.charAt(j);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return json.substring(start, j + 1);
            }
        }
        return null;
    }

    private static double[] extractArray(String json, String key) {
        List<Double> values = new ArrayList<>();
        int i = json.indexOf("\"" + key + "\"");
        if (i < 0) return null;
        int start = json.indexOf('[', i);
        int end = json.indexOf(']', start);
        if (start < 0 || end < 0) return null;
        String body = json.substring(start + 1, end);
        for (String token : body.split(",")) {
            try {
                values.add(Double.parseDouble(token.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        double[] out = new double[values.size()];
        for (int k = 0; k < out.length; k++) out[k] = values.get(k);
        return out;
    }

    private static List<String> extractStringArray(String json, String key) {
        List<String> values = new ArrayList<>();
        int i = json.indexOf("\"" + key + "\"");
        if (i < 0) return values;
        int start = json.indexOf('[', i);
        int end = json.indexOf(']', start);
        if (start < 0 || end < 0) return values;
        String body = json.substring(start + 1, end);
        for (String token : body.split(",")) {
            String v = token.trim().replace("\"", "");
            if (!v.isEmpty()) values.add(v);
        }
        return values;
    }
}
