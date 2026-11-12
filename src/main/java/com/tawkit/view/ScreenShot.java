package com.tawkit.view;

import com.tawkit.model.content.ContentItem;
import com.tawkit.model.prayer.DailyPrayerTimes;
import com.tawkit.model.prayer.PrayerEntry;
import com.tawkit.model.prayer.PrayerType;
import com.tawkit.model.weather.WeatherData;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Dev harness: renders the TawkitScreen to a PNG for pixel comparison
 * against the reference design. Not part of the app runtime.
 *
 * Run:  java -cp target/classes com.tawkit.view.ScreenShot [out.png]
 */
public final class ScreenShot {

    public static void main(String[] args) throws Exception {
        String out = args.length > 0 ? args[0]
                : System.getProperty("java.io.tmpdir") + File.separator + "tawkit-screen.png";

        TawkitScreen screen = new TawkitScreen();
        screen.setSize(1920, 1080);

        // Reference sample data
        screen.setDate(LocalDate.of(2026, 5, 4), 17, 11, 1447);
        screen.setMosqueName("اسم المسجد");
        screen.setCityInfo("Amman", 31.95, 35.93);
        screen.setWeather(new WeatherData(40, 0, 12, List.of(
                new WeatherData.WeatherDay(LocalDate.now(), 0, 10, 15, 0))));
        screen.setClockMode(false, true);
        screen.setCountdown("2:31", "Next Prayer", 0.62);
        screen.setVerses(List.of(new ContentItem(com.tawkit.model.content.ContentType.QURAN_VERSE,
                "لِكُلِّ نَبَإٍ مُسْتَقَرٌّ وَسَوْفَ تَعْلَمُونَ",
                "", "الرعد")), true);

        DailyPrayerTimes times = new DailyPrayerTimes(LocalDate.now());
        times.put(PrayerType.FAJR, new PrayerEntry(PrayerType.FAJR, LocalTime.of(4, 26), null));
        times.put(PrayerType.SHURUQ, new PrayerEntry(PrayerType.SHURUQ, LocalTime.of(5, 48), null));
        times.put(PrayerType.DHUHR, new PrayerEntry(PrayerType.DHUHR, LocalTime.of(12, 18), null));
        times.put(PrayerType.ASR, new PrayerEntry(PrayerType.ASR, LocalTime.of(15, 38), null));
        times.put(PrayerType.MAGHRIB, new PrayerEntry(PrayerType.MAGHRIB, LocalTime.of(18, 48), null));
        times.put(PrayerType.ISHA, new PrayerEntry(PrayerType.ISHA, LocalTime.of(20, 18), null));
        screen.setPrayerTimes(times);

        BufferedImage img = new BufferedImage(1920, 1080, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = img.createGraphics();
        screen.paint(g);
        g.dispose();

        ImageIO.write(img, "png", new File(out));
        System.out.println("Saved " + out);
    }
}
