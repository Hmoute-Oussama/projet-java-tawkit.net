package com.tawkit.view;

import com.tawkit.model.content.ContentItem;
import com.tawkit.model.prayer.DailyPrayerTimes;
import com.tawkit.model.weather.WeatherData;
import com.tawkit.view.render.BackgroundElement;
import com.tawkit.view.render.CalligraphyElement;
import com.tawkit.view.render.ClockElement;
import com.tawkit.view.render.CountdownElement;
import com.tawkit.view.render.DashboardElement;
import com.tawkit.view.render.DateHeaderElement;
import com.tawkit.view.render.EmblemElement;
import com.tawkit.view.render.FooterElement;
import com.tawkit.view.render.HeaderLeftElement;
import com.tawkit.view.render.MosqueNameElement;
import com.tawkit.view.render.PrayerScheduleElement;
import com.tawkit.view.render.RenderContext;

import javax.swing.JComponent;
import javax.swing.Timer;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.util.List;

/**
 * Composite view of the full-screen dashboard (Composite + Strategy
 * patterns): a fixed ordered list of {@link DashboardElement}s, each owning
 * its own state and painting itself with CSS-fraction coordinates, exactly
 * where the reference design puts it:
 *
 * <pre>
 *   [menu ☰ 40° ☀]                Monday 17 Dhul'Qidah 1447 . 04/05/2026
 *   [coords / flag]                                   (top center)
 *   (o)                        اسم المسجد
 *  (emblem)                1:07       30 PM         (2:31)
 *                     لِكُلِّ نَبَإٍ مُسْتَقَرٌّ...           Next Prayer
 *                 10°       15°       10° ...            (circle)
 *                 Fajr    Sunrise    Dhuhr ...
 *                 4:26      5:48     12:18 ...
 *                  لا حول ولا قوة إلا بالله
 * </pre>
 */
public class TawkitScreen extends JComponent {

    // Composite: paint order = visual stacking order (background first)
    private final BackgroundElement background = new BackgroundElement();
    private final HeaderLeftElement headerLeft = new HeaderLeftElement();
    private final DateHeaderElement dateHeader = new DateHeaderElement();
    private final EmblemElement emblem = new EmblemElement();
    private final MosqueNameElement mosqueName = new MosqueNameElement();
    private final ClockElement clock = new ClockElement();
    private final CountdownElement countdown = new CountdownElement();
    private final CalligraphyElement calligraphy = new CalligraphyElement();
    private final PrayerScheduleElement schedule = new PrayerScheduleElement();
    private final FooterElement footer = new FooterElement();

    private final List<DashboardElement> elements = List.of(
            background, headerLeft, dateHeader, emblem, mosqueName,
            clock, countdown, calligraphy, schedule, footer);

    private Runnable onMenuClick;
    private final Timer secondTimer;
    private final Timer rotationTimer;

    public TawkitScreen() {
        setOpaque(true);
        secondTimer = new Timer(1000, e -> repaint());
        secondTimer.start();
        rotationTimer = new Timer(18_000, e -> advanceContent());
        rotationTimer.start();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (onMenuClick != null
                        && headerLeft.isMenuClick(e.getX(), e.getY(), context())) {
                    onMenuClick.run();
                }
            }
        });
    }

    public void setOnMenuClick(Runnable r) {
        this.onMenuClick = r;
    }

    // --------------------------------------------------- data facade (API)

    public void setDate(LocalDate gregorian, int hijriDay, int hijriMonth, int hijriYear) {
        dateHeader.setDate(gregorian, hijriDay, hijriMonth, hijriYear);
        repaint();
    }

    public void setMosqueName(String name) {
        mosqueName.setName(name);
        repaint();
    }

    public void setCityInfo(String cityName, double latitude, double longitude) {
        headerLeft.setCityInfo(latitude, longitude);
        repaint();
    }

    public void setWeather(WeatherData data) {
        headerLeft.setWeather(data);
        schedule.setHasWeather(data != null);
        if (data != null && !data.getDays().isEmpty()) {
            schedule.setDailyTemps(data.getDays().get(0).minTemp(), data.getDays().get(0).maxTemp());
        } else {
            schedule.setDailyTemps(Double.NaN, Double.NaN);
        }
        repaint();
    }

    public void setClockMode(boolean use24, boolean showSeconds) {
        clock.setClockMode(use24, showSeconds);
        schedule.setClockMode(use24);
        repaint();
    }

    public void setCountdown(String digits, String label, double progress) {
        countdown.setCountdown(digits, label);
        repaint();
    }

    public void setVerses(List<ContentItem> items, boolean arabic) {
        calligraphy.setVerses(items);
        repaint();
    }

    public void setRotationSeconds(int seconds) {
        rotationTimer.setDelay(Math.max(3, seconds) * 1000);
    }

    public void setFooterTexts(List<String> texts) {
        footer.setTexts(texts);
        repaint();
    }

    public void setPrayerTimes(DailyPrayerTimes times) {
        schedule.setPrayerTimes(times);
        repaint();
    }

    public void applyTheme(String theme) {
        background.setTheme(theme);
        repaint();
    }

    public void applyCustomImage(String path) {
        background.setCustomImage(path);
        repaint();
    }

    // ------------------------------------------------------------- painting

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        RenderContext ctx = context();
        for (DashboardElement element : elements) {
            element.render(g2, ctx);
        }
        g2.dispose();
    }

    private void advanceContent() {
        boolean any = calligraphy.advance();
        any |= footer.advance();
        if (any) repaint();
    }

    private RenderContext context() {
        return new RenderContext(getWidth(), getHeight());
    }
}
