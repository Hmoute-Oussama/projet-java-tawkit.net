package com.tawkit.service.settings;

import com.tawkit.model.prayer.AsrMethod;
import com.tawkit.model.prayer.CalculationMethod;
import com.tawkit.model.prayer.IqamaSettings;
import com.tawkit.model.prayer.Location;
import com.tawkit.model.prayer.PrayerType;
import com.tawkit.model.settings.AppSettings;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Persists {@link AppSettings} to a properties file in
 * <code>&lt;user.home&gt;/.tawkit/settings.properties</code>, and supports
 * exporting/importing settings to any file (like Tawkit's export/import).
 */
public class SettingsService {

    private final Path storeFile;

    public SettingsService() {
        this(Path.of(System.getProperty("user.home"), ".tawkit", "settings.properties"));
    }

    public SettingsService(Path storeFile) {
        this.storeFile = storeFile;
    }

    public Path getStoreFile() {
        return storeFile;
    }

    public AppSettings load() {
        AppSettings settings = new AppSettings();
        Properties props = new Properties();
        if (Files.exists(storeFile)) {
            try (InputStream in = Files.newInputStream(storeFile)) {
                props.load(new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
                apply(settings, props);
            } catch (IOException e) {
                System.err.println("[Settings] load failed: " + e.getMessage());
            }
        }
        return settings;
    }

    public void save(AppSettings settings) {
        try {
            Files.createDirectories(storeFile.getParent());
            Properties props = toProperties(settings);
            try (OutputStream out = Files.newOutputStream(storeFile)) {
                props.store(new java.io.OutputStreamWriter(out, StandardCharsets.UTF_8),
                        "Tawkit Desktop settings");
            }
        } catch (IOException e) {
            System.err.println("[Settings] save failed: " + e.getMessage());
        }
    }

    /** Export settings to an arbitrary file (Tawkit "Export Settings"). */
    public void exportTo(Path file, AppSettings settings) throws IOException {
        Files.createDirectories(file.getParent() == null ? Path.of(".") : file.getParent());
        try (OutputStream out = Files.newOutputStream(file)) {
            toProperties(settings).store(new java.io.OutputStreamWriter(out, StandardCharsets.UTF_8),
                    "Tawkit Desktop settings export");
        }
    }

    /** Import settings from a file (Tawkit "Import Local file"). */
    public AppSettings importFrom(Path file) throws IOException {
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            props.load(new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
        }
        AppSettings settings = new AppSettings();
        apply(settings, props);
        return settings;
    }

    // ------------------------------------------------------------ mapping

    private Properties toProperties(AppSettings s) {
        Properties p = new Properties();
        p.setProperty("mosqueName", nz(s.getMosqueName()));
        p.setProperty("language", nz(s.getLanguage()));
        p.setProperty("adhanEnabled", String.valueOf(s.isAdhanEnabled()));
        p.setProperty("screenDimDuringPrayer", String.valueOf(s.isScreenDimDuringPrayer()));
        p.setProperty("prayerDimMinutes", String.valueOf(s.getPrayerDimMinutes()));
        p.setProperty("hijriOffsetDays", String.valueOf(s.getHijriOffsetDays()));

        Location loc = s.getLocation();
        if (loc != null) {
            p.setProperty("location", loc.getName() + "|" + loc.getCountry() + "|"
                    + loc.getLatitude() + "|" + loc.getLongitude() + "|" + loc.getTimezone());
        }
        p.setProperty("calculationMethod", s.getCalculationMethod().name());
        p.setProperty("asrMethod", s.getAsrMethod().name());

        IqamaSettings iq = s.getIqamaSettings();
        for (PrayerType type : new PrayerType[]{PrayerType.FAJR, PrayerType.DHUHR,
                PrayerType.ASR, PrayerType.MAGHRIB, PrayerType.ISHA}) {
            String mode = iq.getMode(type) == IqamaSettings.IqamaMode.FIXED ? "fixed" : "offset";
            String value = mode.equals("fixed") ? String.valueOf(iq.getFixedTime(type)) 
                    : String.valueOf(iq.getOffsetMinutes(type));
            p.setProperty("iqama." + type.name(), mode + "|" + value);
        }

        for (PrayerType type : PrayerType.values()) {
            p.setProperty("adjust." + type.name(), String.valueOf(s.getAdjust(type)));
        }

        p.setProperty("use24HourClock", String.valueOf(s.isUse24HourClock()));
        p.setProperty("showSeconds", String.valueOf(s.isShowSeconds()));
        p.setProperty("weatherEnabled", String.valueOf(s.isWeatherEnabled()));
        p.setProperty("contentRotationSeconds", String.valueOf(s.getContentRotationSeconds()));
        p.setProperty("slidesEnabled", String.valueOf(s.isSlidesEnabled()));
        p.setProperty("audioVolume", String.valueOf(s.getAudioVolume()));
        p.setProperty("preFajrAlertEnabled", String.valueOf(s.isPreFajrAlertEnabled()));
        p.setProperty("preFajrAlertMinutes", String.valueOf(s.getPreFajrAlertMinutes()));
        p.setProperty("backgroundTheme", nz(s.getBackgroundTheme()));
        p.setProperty("customBackgroundPath", nz(s.getCustomBackgroundPath()));

        putList(p, "announcement", s.getAnnouncements());
        putList(p, "reminder", s.getReminders());
        return p;
    }

    private void apply(AppSettings s, Properties p) {
        s.setMosqueName(p.getProperty("mosqueName", ""));
        s.setLanguage(p.getProperty("language", "en"));
        s.setAdhanEnabled(Boolean.parseBoolean(p.getProperty("adhanEnabled", "true")));
        s.setScreenDimDuringPrayer(Boolean.parseBoolean(p.getProperty("screenDimDuringPrayer", "true")));
        s.setPrayerDimMinutes(intVal(p, "prayerDimMinutes", 10));
        s.setHijriOffsetDays(intVal(p, "hijriOffsetDays", 0));

        String locStr = p.getProperty("location", "");
        if (!locStr.isBlank()) {
            String[] f = locStr.split("\\|", 5);
            if (f.length >= 5) {
                try {
                    s.setLocation(new Location(f[0], f[1],
                            Double.parseDouble(f[2]), Double.parseDouble(f[3]),
                            Double.parseDouble(f[4])));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        s.setCalculationMethod(enumVal(p, "calculationMethod", CalculationMethod.MUSLIM_WORLD_LEAGUE));
        s.setAsrMethod(enumVal(p, "asrMethod", AsrMethod.STANDARD));

        IqamaSettings iq = s.getIqamaSettings();
        for (PrayerType type : new PrayerType[]{PrayerType.FAJR, PrayerType.DHUHR,
                PrayerType.ASR, PrayerType.MAGHRIB, PrayerType.ISHA}) {
            String spec = p.getProperty("iqama." + type.name(), "");
            if (!spec.isBlank()) {
                String[] f = spec.split("\\|", 2);
                if ("fixed".equals(f[0]) && f.length > 1) {
                    try {
                        iq.setFixed(type, java.time.LocalTime.parse(f[1]));
                    } catch (Exception ignored) {
                    }
                } else if (f.length > 1) {
                    iq.setOffset(type, intValStr(f[1], 15));
                }
            }
        }

        for (PrayerType type : PrayerType.values()) {
            s.setAdjust(type, intVal(p, "adjust." + type.name(), 0));
        }

        s.setUse24HourClock(Boolean.parseBoolean(p.getProperty("use24HourClock", "true")));
        s.setShowSeconds(Boolean.parseBoolean(p.getProperty("showSeconds", "true")));
        s.setWeatherEnabled(Boolean.parseBoolean(p.getProperty("weatherEnabled", "true")));
        s.setContentRotationSeconds(intVal(p, "contentRotationSeconds", 18));
        s.setSlidesEnabled(Boolean.parseBoolean(p.getProperty("slidesEnabled", "true")));
        s.setAudioVolume(doubleVal(p, "audioVolume", 1.0));
        s.setPreFajrAlertEnabled(Boolean.parseBoolean(p.getProperty("preFajrAlertEnabled", "true")));
        s.setPreFajrAlertMinutes(intVal(p, "preFajrAlertMinutes", 10));
        s.setBackgroundTheme(p.getProperty("backgroundTheme", "night"));
        s.setCustomBackgroundPath(p.getProperty("customBackgroundPath", ""));

        s.setAnnouncements(readList(p, "announcement"));
        s.setReminders(readList(p, "reminder"));
    }

    private static void putList(Properties p, String key, List<String> values) {
        p.setProperty(key + ".count", String.valueOf(values.size()));
        for (int i = 0; i < values.size(); i++) {
            p.setProperty(key + "." + i, values.get(i));
        }
    }

    private static List<String> readList(Properties p, String key) {
        int count = intVal(p, key + ".count", 0);
        List<String> out = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String v = p.getProperty(key + "." + i);
            if (v != null && !v.isBlank()) out.add(v);
        }
        return out;
    }

    private static String nz(String v) {
        return v == null ? "" : v;
    }

    private static int intVal(Properties p, String key, int def) {
        return intValStr(p.getProperty(key), def);
    }

    private static int intValStr(String v, int def) {
        try {
            return Integer.parseInt(v.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private static double doubleVal(Properties p, String key, double def) {
        try {
            return Double.parseDouble(p.getProperty(key).trim());
        } catch (Exception e) {
            return def;
        }
    }

    private static <T extends Enum<T>> T enumVal(Properties p, String key, T def) {
        try {
            return T.valueOf(def.getDeclaringClass(), p.getProperty(key, def.name()).trim());
        } catch (Exception e) {
            return def;
        }
    }
}
