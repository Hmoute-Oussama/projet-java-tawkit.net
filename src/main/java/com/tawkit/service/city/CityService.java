package com.tawkit.service.city;

import com.tawkit.model.prayer.Location;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Built-in city database loaded from <code>/data/cities.csv</code>
 * (country | city | latitude | longitude | timezone). Works offline.
 */
public class CityService {

    public record City(String country, String name, double latitude, double longitude, double timezone) {
        public Location toLocation() {
            return new Location(name, country, latitude, longitude, timezone);
        }
    }

    private final List<City> cities = new ArrayList<>();
    private final TreeSet<String> countries = new TreeSet<>();

    public CityService() {
        load();
    }

    private void load() {
        try (InputStream in = getClass().getResourceAsStream("/data/cities.csv")) {
            if (in == null) return;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    String[] f = line.split("\\|", 5);
                    if (f.length < 5) continue;
                    try {
                        City city = new City(f[0].trim(), f[1].trim(),
                                Double.parseDouble(f[2]), Double.parseDouble(f[3]),
                                Double.parseDouble(f[4]));
                        cities.add(city);
                        countries.add(city.country());
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[City] failed loading database: " + e.getMessage());
        }
    }

    public List<String> getCountries() {
        return new ArrayList<>(countries);
    }

    public List<String> getCitiesOf(String country) {
        List<String> out = new ArrayList<>();
        for (City c : cities) {
            if (c.country().equalsIgnoreCase(country)) out.add(c.name());
        }
        return out;
    }

    public City find(String country, String city) {
        for (City c : cities) {
            if (c.country().equalsIgnoreCase(country) && c.name().equalsIgnoreCase(city)) {
                return c;
            }
        }
        return null;
    }

    /** Nearest city to coordinates (simple linear scan). */
    public City nearest(double latitude, double longitude) {
        City best = null;
        double bestDist = Double.MAX_VALUE;
        for (City c : cities) {
            double d = (c.latitude() - latitude) * (c.latitude() - latitude)
                    + (c.longitude() - longitude) * (c.longitude() - longitude);
            if (d < bestDist) {
                bestDist = d;
                best = c;
            }
        }
        return best;
    }

    public int size() {
        return cities.size();
    }
}
