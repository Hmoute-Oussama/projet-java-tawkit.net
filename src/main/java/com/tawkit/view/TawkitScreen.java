package com.tawkit.view;

import com.tawkit.model.content.ContentItem;
import com.tawkit.model.prayer.DailyPrayerTimes;
import com.tawkit.model.prayer.PrayerEntry;
import com.tawkit.model.prayer.PrayerType;
import com.tawkit.model.weather.WeatherData;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The full-screen Tawkit dashboard, custom-painted from a single set of
 * CSS-fraction coordinates so every element sits exactly where the reference
 * design puts it:
 *
 * <pre>
 *   [menu ☰ 40° ☀]                Monday 17 Dhul'Qidah 1447 . 04/05/2026
 *   [coords 41° 27']                                  (top center)
 *   [flag]
 *   (o)                        اسم المسجد
 *  (emblem)                1:07            30 PM        (2:31)
 *                            لِكُلِّ نَبَإٍ مُسْتَقَرٌّ...      Next Prayer
 *                 10°        15°      10° ...            (circle)
 *                 Fajr    Sunrise   Dhuhr ...
 *                 4:26      5:48     12:18 ...
 *                  لا حول ولا قوة إلا بالله
 * </pre>
 */
public class TawkitScreen extends JComponent {

    // ------------------------------------------------------------- state

    private String dateText = "";
    private String mosqueName = "اسم المسجد";
    private String coordsText = "";
    private String tempText = "--°";
    private String weatherIcon = "☀";
    private boolean hasWeather = false;

    private boolean use24 = false;
    private boolean showSeconds = true;

    private String countdownDigits = "--:--";
    private String countdownLabel = "Next Prayer";
    private double countdownProgress = 0.0;

    private final List<String> verseLines = new ArrayList<>();
    private final List<String> verseLabels = new ArrayList<>();
    private int contentIndex = 0;
    private int rotationSeconds = 18;

    private final String[] footerTexts = {"لا حول ولا قوة إلا بالله"};
    private int footerIndex = 0;

    private DailyPrayerTimes prayerTimes;
    private double dailyMin = Double.NaN;
    private double dailyMax = Double.NaN;

    private String theme = "night";
    private transient BufferedImage customImage;

    private Runnable onMenuClick;

    private final Timer secondTimer;
    private final Timer rotationTimer;

    public TawkitScreen() {
        setOpaque(true);
        secondTimer = new Timer(1000, e -> repaint());
        secondTimer.start();
        rotationTimer = new Timer(rotationSeconds * 1000, e -> advanceContent());
        rotationTimer.start();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                double vw = getWidth() / 100.0;
                double vh = getHeight() / 100.0;
                if (e.getX() < 4.5 * vw && e.getY() < 6.5 * vh && onMenuClick != null) {
                    onMenuClick.run();
                }
            }
        });
    }

    public void setOnMenuClick(Runnable r) {
        this.onMenuClick = r;
    }

    // ------------------------------------------------------- data setters

    public void setDate(LocalDate gregorian, int hijriDay, int hijriMonth, int hijriYear) {
        String weekday = gregorian.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        String month = hijriMonth >= 1 && hijriMonth <= 12
                ? TawkitHijriNames.ENGLISH[hijriMonth - 1] : "";
        this.dateText = weekday + " " + hijriDay + " " + month + " " + hijriYear
                + " . " + String.format("%02d/%02d/%04d",
                        gregorian.getDayOfMonth(), gregorian.getMonthValue(), gregorian.getYear());
        repaint();
    }

    public void setMosqueName(String name) {
        this.mosqueName = (name == null || name.isBlank()) ? "اسم المسجد" : name;
        repaint();
    }

    public void setCityInfo(String cityName, double latitude, double longitude) {
        int latDeg = (int) Math.floor(Math.abs(latitude));
        int latMin = (int) Math.round((Math.abs(latitude) - latDeg) * 60);
        if (latMin >= 60) { latDeg += 1; latMin -= 60; }
        int lonDeg = (int) Math.floor(Math.abs(longitude));
        int lonMin = (int) Math.round((Math.abs(longitude) - lonDeg) * 60);
        if (lonMin >= 60) { lonDeg += 1; lonMin -= 60; }
        this.coordsText = (latitude >= 0 ? "N" : "S") + " " + latDeg + "° " + String.format("%02d'", latMin)
                + "   " + (longitude >= 0 ? "E" : "W") + " " + lonDeg + "° " + String.format("%02d'", lonMin);
        repaint();
    }

    public void setWeather(WeatherData data) {
        if (data == null) {
            hasWeather = false;
            dailyMin = dailyMax = Double.NaN;
        } else {
            hasWeather = true;
            tempText = Math.round(data.getCurrentTemp()) + "°";
            weatherIcon = WeatherData.iconFor(data.getCurrentCode());
            if (!data.getDays().isEmpty()) {
                dailyMin = data.getDays().get(0).minTemp();
                dailyMax = data.getDays().get(0).maxTemp();
            }
        }
        repaint();
    }

    public void setClockMode(boolean use24, boolean showSeconds) {
        this.use24 = use24;
        this.showSeconds = showSeconds;
        repaint();
    }

    public void setCountdown(String digits, String label, double progress) {
        this.countdownDigits = digits == null || digits.isBlank() ? "--:--" : digits;
        this.countdownLabel = label == null ? "" : label;
        this.countdownProgress = Math.max(0, Math.min(1, progress));
        repaint();
    }

    public void setVerses(List<ContentItem> items, boolean arabic) {
        verseLines.clear();
        verseLabels.clear();
        if (items != null) {
            for (ContentItem item : items) {
                // The central calligraphy is Arabic-only per the reference design,
                // regardless of the interface language.
                String text = item.getArabicText();
                if (text == null || text.isBlank()) continue;
                verseLines.add(text);
                String ref = item.getReference() == null ? "" : item.getReference();
                verseLabels.add(ref.startsWith("Quran") ? ref.substring(5).trim() : ref);
            }
        }
        contentIndex = 0;
        repaint();
    }

    public void setRotationSeconds(int seconds) {
        rotationSeconds = Math.max(3, seconds);
        rotationTimer.setDelay(rotationSeconds * 1000);
    }

    public void setFooterTexts(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            footerIndex = 0;
        } else {
            List<String> all = new ArrayList<>();
            all.add(footerTexts[0]);
            all.addAll(texts);
            // replace content but keep the dhikr line first
            // (footerTexts is final array; use a mutable copy below)
            footerList.clear();
            footerList.addAll(all);
            footerIndex = 0;
        }
        repaint();
    }

    private final List<String> footerList = new ArrayList<>(List.of(footerTexts[0]));

    public void setPrayerTimes(DailyPrayerTimes times) {
        this.prayerTimes = times;
        repaint();
    }

    public void applyTheme(String theme) {
        this.theme = theme == null ? "night" : theme;
        this.customImage = null;
        if ("custom".equals(theme)) {
            // custom image is applied via applyCustomImage
        }
        repaint();
    }

    public void applyCustomImage(String path) {
        if (path == null || path.isBlank()) return;
        try {
            customImage = ImageIO.read(new File(path));
        } catch (Exception e) {
            System.err.println("[Background] cannot load " + path);
        }
        repaint();
    }

    private void advanceContent() {
        if (verseLines.size() > 1) {
            contentIndex = (contentIndex + 1) % verseLines.size();
            repaint();
        }
        if (footerList.size() > 1) {
            footerIndex = (footerIndex + 1) % footerList.size();
            repaint();
        }
    }

    // ------------------------------------------------------------ painting

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        double w = getWidth();
        double h = getHeight();
        double vw = w / 100.0;
        double vh = h / 100.0;

        // ---- 1. Background photo + dark overlay ----
        g2.setColor(TawkitTheme.BLACK);
        g2.fillRect(0, 0, (int) w, (int) h);
        BufferedImage bg = customImage != null ? customImage : loadBundledBackground();
        if (bg != null) {
            double scale = Math.max(w / bg.getWidth(), h / bg.getHeight());
            int bw = (int) Math.round(bg.getWidth() * scale);
            int bh = (int) Math.round(bg.getHeight() * scale);
            g2.drawImage(bg, (int) ((w - bw) / 2), (int) ((h - bh) / 2), bw, bh, null);
        }
        g2.setColor(overlayFor(theme));
        g2.fillRect(0, 0, (int) w, (int) h);

        // ---- 2. Header: menu, weather, coordinates, flag (left) ----
        paintHeaderLeft(g2, vw, vh);

        // ---- 3. Header date (top center) ----
        g2.setFont(TawkitTheme.serif(Font.PLAIN, (float) (1.9 * vw)));
        drawTextShadowed(g2, dateText, (float) (w / 2), (float) (5.0 * vh), TextAnchor.CENTER,
                TawkitTheme.WARM_WHITE, 0.12f * (float) vw);

        // ---- 4. Left decorative emblem ----
        paintEmblem(g2, vw, vh);

        // ---- 5. Countdown circle (upper right) ----
        paintCountdownCircle(g2, vw, vh);

        // ---- 6. Mosque name above clock (Arabic) ----
        g2.setFont(TawkitTheme.arabic(2.6f * (float) vw));
        drawTextShadowed(g2, mosqueName, (float) (w / 2), (float) (17.0 * vh), TextAnchor.CENTER,
                TawkitTheme.WARM_WHITE, 0.10f * (float) vw);

        // ---- 7. Main digital clock ----
        paintClock(g2, vw, vh);

        // ---- 8. Central Arabic calligraphy ----
        paintCalligraphy(g2, vw, vh);

        // ---- 9. Prayer schedule (bottom) ----
        paintPrayerSchedule(g2, vw, vh);

        // ---- 10. Footer ----
        g2.setFont(TawkitTheme.arabic(2.6f * (float) vw));
        drawTextShadowed(g2, currentFooter(), (float) (w / 2), (float) (94.0 * vh), TextAnchor.CENTER,
                TawkitTheme.WARM_WHITE, 0.18f * (float) vw);

        g2.setFont(TawkitTheme.serif(Font.PLAIN, 0.95f * (float) vw));
        drawTextShadowed(g2, "TAWKIT DESKTOP", (float) (1.2 * vw), (float) (98.6 * vh), TextAnchor.LEFT,
                withAlpha(TawkitTheme.GRAY, 0.55f), 0);
        drawTextShadowed(g2, "JAVA SWING EDITION", (float) (w - 1.2 * vw), (float) (98.6 * vh), TextAnchor.RIGHT,
                withAlpha(TawkitTheme.GRAY, 0.55f), 0);

        g2.dispose();
    }

    private String currentFooter() {
        return footerList.isEmpty() ? footerTexts[0] : footerList.get(footerIndex % footerList.size());
    }

    private transient BufferedImage bundledBackground;

    private BufferedImage loadBundledBackground() {
        if (bundledBackground == null) {
            try (var in = getClass().getResourceAsStream("/assets/backgrounds/dome-of-the-rock.jpg")) {
                if (in != null) bundledBackground = ImageIO.read(in);
            } catch (Exception ignored) {
            }
        }
        return bundledBackground;
    }

    private Color overlayFor(String theme) {
        return switch (theme) {
            case "green" -> new Color(0, 20, 8, 175);
            case "blue" -> new Color(0, 10, 30, 175);
            default -> new Color(0, 0, 0, 185); // dark cinematic overlay (reference-matched)
        };
    }

    // ------------------------------------------------------- header left

    private void paintHeaderLeft(Graphics2D g2, double vw, double vh) {
        float x = (float) (2.0 * vw);

        // Hamburger menu (three lines), top of the cluster
        g2.setColor(TawkitTheme.MENU_GRAY);
        g2.setStroke(new BasicStroke(Math.max(1.5f, (float) (0.18 * vw))));
        for (int i = 0; i < 3; i++) {
            float ly = (float) (2.2 * vh) + i * (float) (0.7 * vh);
            g2.drawLine((int) Math.round(x), (int) Math.round(ly),
                    (int) Math.round(x + 1.6 * vw), (int) Math.round(ly));
        }

        // Temperature + sun icon
        float tempY = (float) (7.4 * vh);
        g2.setFont(TawkitTheme.serif(Font.BOLD, 1.9f * (float) vw));
        drawText(g2, tempText, x, tempY, TextAnchor.LEFT, TawkitTheme.WARM_WHITE);
        paintSun(g2, x + 5.0f * (float) vw, tempY - 0.7f * (float) vh, 0.95f * (float) vw);

        // Coordinates
        g2.setFont(TawkitTheme.serif(Font.PLAIN, 1.05f * (float) vw));
        drawText(g2, coordsText, x, (float) (11.0 * vh), TextAnchor.LEFT, TawkitTheme.GRAY);

        // Small Jordan-style flag
        paintFlag(g2, x, (float) (12.8 * vh), (float) (2.2 * vw), (float) (1.4 * vw));
    }

    /** Small sun: yellow disc with rays (drawn, never a tofu-box emoji). */
    private void paintSun(Graphics2D g2, float cx, float cy, float r) {
        g2.setColor(TawkitTheme.SUN_YELLOW);
        g2.fill(new Ellipse2D.Float(cx - r * 0.55f, cy - r * 0.55f, r * 1.1f, r * 1.1f));
        g2.setStroke(new BasicStroke(Math.max(1f, r * 0.16f)));
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI / 4 * i;
            float x1 = cx + (float) Math.cos(angle) * r * 0.8f;
            float y1 = cy + (float) Math.sin(angle) * r * 0.8f;
            float x2 = cx + (float) Math.cos(angle) * r * 1.35f;
            float y2 = cy + (float) Math.sin(angle) * r * 1.35f;
            g2.drawLine(Math.round(x1), Math.round(y1), Math.round(x2), Math.round(y2));
        }
    }

    private void paintFlag(Graphics2D g2, float x, float y, float fw, float fh) {
        Color black = new Color(0x10, 0x10, 0x10);
        Color white = new Color(0xEE, 0xEE, 0xEE);
        Color green = new Color(0x00, 0x7A, 0x3D);
        Color red = new Color(0xCE, 0x11, 0x22);

        float stripe = fh / 3f;
        g2.setColor(black);
        g2.fill(new RoundRectangle2D.Float(x, y, fw, stripe, 0, 0));
        g2.setColor(white);
        g2.fill(new RoundRectangle2D.Float(x, y + stripe, fw, stripe, 0, 0));
        g2.setColor(green);
        g2.fill(new RoundRectangle2D.Float(x, y + 2 * stripe, fw, stripe, 0, 0));
        g2.setColor(red);
        java.awt.geom.Path2D triangle = new java.awt.geom.Path2D.Float();
        triangle.moveTo(x, y);
        triangle.lineTo(x + fw * 0.45f, y + fh / 2f);
        triangle.lineTo(x, y + fh);
        triangle.closePath();
        g2.fill(triangle);
        g2.setColor(white);
        float starR = fh * 0.10f;
        g2.fill(new Ellipse2D.Float(x + fw * 0.12f - starR, y + fh / 2f - starR, starR * 2, starR * 2));
    }

    // ------------------------------------------------------------ emblem

    private void paintEmblem(Graphics2D g2, double vw, double vh) {
        double cx = 11.5 * vw;
        double cy = 33 * vh;
        double r = 8 * vw;

        g2.setColor(new Color(8, 8, 8, 140));
        g2.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));

        // Concentric borders (silver/gray)
        g2.setColor(new Color(200, 200, 200, 70));
        g2.setStroke(new BasicStroke(Math.max(1.2f, (float) (r * 0.012))));
        g2.draw(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
        g2.draw(new Ellipse2D.Double(cx - r * 0.90, cy - r * 0.90, r * 1.8, r * 1.8));
        g2.setColor(new Color(170, 170, 170, 40));
        g2.draw(new Ellipse2D.Double(cx - r * 0.80, cy - r * 0.80, r * 1.6, r * 1.6));

        // Mosque silhouette (dome + minarets + base) in silver
        Color silhouette = new Color(190, 190, 190, 90);
        g2.setColor(silhouette);
        double baseW = r * 0.9;
        double baseH = r * 0.32;
        double baseX = cx - baseW / 2;
        double baseY = cy + r * 0.05;
        g2.fill(new RoundRectangle2D.Double(baseX, baseY, baseW, baseH, baseH * 0.25, baseH * 0.25));

        // Central dome
        double domeW = r * 0.55;
        double domeH = r * 0.48;
        double domeX = cx - domeW / 2;
        double domeY = baseY - domeH;
        g2.fill(new Arc2D.Double(domeX, domeY, domeW, domeH * 2, 0, 180, Arc2D.CHORD));
        // finial
        g2.fill(new RoundRectangle2D.Double(cx - r * 0.012, domeY - r * 0.10,
                r * 0.024, r * 0.10, 0, 0));

        // Minarets
        double minaretW = r * 0.055;
        double minaretH = r * 0.62;
        for (double side : new double[]{-1, 1}) {
            double mx = cx + side * r * 0.52 - minaretW / 2;
            double my = baseY + baseH - minaretH;
            g2.fill(new RoundRectangle2D.Double(mx, my, minaretW, minaretH, minaretW / 2, minaretW / 2));
            g2.fill(new Arc2D.Double(mx - minaretW * 0.35, my - minaretW * 0.8,
                    minaretW * 1.7, minaretW * 1.6, 0, 180, Arc2D.CHORD));
        }
    }

    // --------------------------------------------------- countdown circle

    private void paintCountdownCircle(Graphics2D g2, double vw, double vh) {
        double cx = 87.5 * vw;
        double cy = 27 * vh;
        double r = 7.75 * vw;

        // Dark interior
        g2.setColor(new Color(5, 5, 5, 165));
        g2.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));

        // Thin concentric borders
        g2.setColor(new Color(197, 166, 106, 60));
        g2.setStroke(new BasicStroke(Math.max(1.2f, (float) (r * 0.014))));
        g2.draw(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
        g2.draw(new Ellipse2D.Double(cx - r * 0.86, cy - r * 0.86, r * 1.72, r * 1.72));

        // Digits
        g2.setFont(TawkitTheme.serif(Font.BOLD, 3.2f * (float) vw));
        drawTextShadowed(g2, countdownDigits, (float) cx, (float) (cy + 0.4 * vh), TextAnchor.CENTER,
                TawkitTheme.GOLDEN_BEIGE, 0.14f * (float) vw);

        // Label
        g2.setFont(TawkitTheme.serif(Font.PLAIN, 1.15f * (float) vw));
        drawTextShadowed(g2, countdownLabel, (float) cx, (float) (cy + r * 0.5), TextAnchor.CENTER,
                TawkitTheme.GRAY, 0);
    }

    // -------------------------------------------------------------- clock

    private void paintClock(Graphics2D g2, double vw, double vh) {
        LocalTime now = LocalTime.now();
        int hour = now.getHour();
        String ampm;
        if (use24) {
            ampm = "";
        } else {
            ampm = hour < 12 ? "AM" : "PM";
            hour = hour % 12;
            if (hour == 0) hour = 12;
        }
        String minutes = String.format("%02d", now.getMinute());
        String digits = hour + ":" + minutes;
        String seconds = showSeconds ? String.format("%02d", now.getSecond()) : "";

        // Clock digits: centered horizontally, top ~22% — ~8vw
        float fontSize = 8.0f * (float) vw;
        g2.setFont(TawkitTheme.serif(Font.BOLD, fontSize));
        FontMetrics fm = g2.getFontMetrics();
        float centerX = (float) (getWidth() / 2);
        float baselineY = (float) (22.0 * vh) + fm.getAscent() * 0.9f;
        float digitsWidth = fm.stringWidth(digits);
        drawTextShadowed(g2, digits, centerX, baselineY, TextAnchor.CENTER,
                TawkitTheme.WARM_WHITE, 0.28f * (float) vw);

        // Seconds immediately right of the digits
        if (!seconds.isEmpty()) {
            g2.setFont(TawkitTheme.serif(Font.BOLD, 3.0f * (float) vw));
            FontMetrics sm = g2.getFontMetrics();
            float secX = centerX + digitsWidth / 2 + sm.stringWidth("00") / 2 + 1.1f * (float) vw;
            drawTextShadowed(g2, seconds, secX, baselineY - sm.getAscent() * 0.25f,
                    TextAnchor.CENTER, TawkitTheme.WARM_WHITE, 0.12f * (float) vw);
        }

        // AM / PM below the seconds
        if (!ampm.isEmpty()) {
            g2.setFont(TawkitTheme.serif(Font.PLAIN, 2.4f * (float) vw));
            FontMetrics am = g2.getFontMetrics();
            float secX = centerX + digitsWidth / 2 + 1.1f * (float) vw + am.stringWidth("00") / 2;
            drawTextShadowed(g2, ampm, secX, baselineY + 3.0f * (float) vh, TextAnchor.CENTER,
                    TawkitTheme.GOLDEN_BEIGE, 0.1f * (float) vw);
        }
    }

    // ------------------------------------------------------- calligraphy

    private void paintCalligraphy(Graphics2D g2, double vw, double vh) {
        if (verseLines.isEmpty()) return;
        int idx = contentIndex % verseLines.size();
        String text = verseLines.get(idx);
        String label = verseLabels.isEmpty() ? "" : verseLabels.get(idx % verseLabels.size());

        boolean arabicScript = text.codePoints().anyMatch(cp -> cp >= 0x0600 && cp <= 0x06FF);
        float size = arabicScript ? 3.5f * (float) vw : 2.4f * (float) vw;
        // Amiri Regular (not Bold) — Bold collides the vowel marks at this size
        g2.setFont(arabicScript ? TawkitTheme.arabic(size) : TawkitTheme.serif(Font.PLAIN, size));

        // Wrap into at most 2 lines within 92% width
        List<String> lines = wrap(g2, text, (float) (92 * vw));
        float lineHeight = size * 1.55f;
        float startY = (float) (47 * vh);

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            float y = startY + i * lineHeight;
            if (arabicScript) {
                line = "﴿" + line + "﴾";
            }
            drawTextShadowed(g2, line, (float) (getWidth() / 2), y, TextAnchor.CENTER,
                    TawkitTheme.COPPER_GOLD, 0.08f * (float) vw);
        }

        // Small "Surat X" label, subtle, above the verse —
        // Latin prefix in the serif, Arabic sura name in Amiri
        if (!label.isEmpty()) {
            float labelY = startY - 2.2f * (float) vh;
            g2.setFont(TawkitTheme.serif(Font.PLAIN, 1.3f * (float) vw));
            String prefix = "Surat ";
            float prefixW = g2.getFontMetrics().stringWidth(prefix);
            drawText(g2, prefix, (float) (16 * vw), labelY, TextAnchor.LEFT,
                    new Color(TawkitTheme.GRAY.getRed(), TawkitTheme.GRAY.getGreen(),
                            TawkitTheme.GRAY.getBlue(), 85));
            g2.setFont(TawkitTheme.arabic(1.4f * (float) vw));
            drawText(g2, label, (float) (16 * vw) + prefixW, labelY, TextAnchor.LEFT,
                    new Color(TawkitTheme.GRAY.getRed(), TawkitTheme.GRAY.getGreen(),
                            TawkitTheme.GRAY.getBlue(), 85));
        }
    }

    private List<String> wrap(Graphics2D g2, String text, float maxWidth) {
        FontMetrics fm = g2.getFontMetrics();
        if (fm.stringWidth(text) <= maxWidth) return List.of(text);
        List<String> out = new ArrayList<>();
        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            String attempt = line.isEmpty() ? word : line + " " + word;
            if (fm.stringWidth(attempt) > maxWidth && !line.isEmpty()) {
                out.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(attempt);
            }
        }
        if (!line.isEmpty()) out.add(line.toString());
        return out;
    }

    // --------------------------------------------------- prayer schedule

    private void paintPrayerSchedule(Graphics2D g2, double vw, double vh) {
        PrayerType[] types = PrayerType.values();
        int n = types.length; // 6
        double colWidth = 96.0 / n;

        for (int i = 0; i < n; i++) {
            double colCenter = 2.0 * vw + (i + 0.5) * colWidth * vw;
            PrayerType type = types[i];

            // Temperature (interpolated from forecast) — small, golden
            g2.setFont(TawkitTheme.serif(Font.PLAIN, 1.7f * (float) vw));
            drawTextShadowed(g2, prayerTemp(type, vw), (float) colCenter, (float) (61.0 * vh),
                    TextAnchor.CENTER, TawkitTheme.GOLDEN_BEIGE, 0.1f * (float) vw);

            // Name — serif, warm white
            g2.setFont(TawkitTheme.serif(Font.PLAIN, 2.3f * (float) vw));
            drawTextShadowed(g2, prayerName(type), (float) colCenter, (float) (65.5 * vh),
                    TextAnchor.CENTER, TawkitTheme.WARM_WHITE, 0.12f * (float) vw);

            // Time — large bold white
            g2.setFont(TawkitTheme.serif(Font.BOLD, 4.2f * (float) vw));
            drawTextShadowed(g2, prayerTime(type), (float) colCenter, (float) (71.5 * vh),
                    TextAnchor.CENTER, Color.WHITE, 0.18f * (float) vw);
        }
    }

    private String prayerName(PrayerType type) {
        return switch (type) {
            case FAJR -> "Fajr";
            case SHURUQ -> "Sunrise";
            case DHUHR -> "Dhuhr";
            case ASR -> "Asr";
            case MAGHRIB -> "Maghrib";
            case ISHA -> "Isha";
        };
    }

    private String prayerTime(PrayerType type) {
        if (prayerTimes == null) return "--:--";
        PrayerEntry e = prayerTimes.get(type);
        if (e == null || e.getAdhanTime() == null) return "--:--";
        LocalTime t = e.getAdhanTime();
        if (use24) return String.format("%02d:%02d", t.getHour(), t.getMinute());
        int hr = t.getHour() % 12;
        if (hr == 0) hr = 12;
        return hr + ":" + String.format("%02d", t.getMinute());
    }

    /** Approximate temperature at each prayer time from the day's min/max. */
    private String prayerTemp(PrayerType type, double vw) {
        if (Double.isNaN(dailyMin) || Double.isNaN(dailyMax) || !hasWeather) return "";
        LocalTime t = prayerTimes != null && prayerTimes.get(type) != null
                ? prayerTimes.get(type).getAdhanTime() : null;
        if (t == null) return "";
        double hour = t.getHour() + t.getMinute() / 60.0;
        // simple solar curve: min at dawn (5h), max at 15h
        double phase = (hour - 5.0) / 10.0; // 5h..15h -> 0..1
        if (phase < 0) phase = Math.max(-0.25, phase / 4.0);      // pre-dawn dips slightly below min
        if (phase > 1) phase = Math.max(0, 1 - (phase - 1) / 4.0); // evening cools
        double f = 0.5 - 0.5 * Math.cos(Math.PI * Math.max(0, Math.min(1, phase)));
        double temp = dailyMin + (dailyMax - dailyMin) * f;
        return Math.round(temp) + "°";
    }

    // -------------------------------------------------------- text utils

    private enum TextAnchor { LEFT, CENTER, RIGHT }

    private void drawText(Graphics2D g2, String text, double x, double y, TextAnchor anchor, Color color) {
        if (text == null || text.isEmpty()) return;
        FontMetrics fm = g2.getFontMetrics();
        float px = switch (anchor) {
            case LEFT -> (float) x;
            case CENTER -> (float) (x - fm.stringWidth(text) / 2.0);
            case RIGHT -> (float) (x - fm.stringWidth(text));
        };
        g2.setColor(color);
        g2.drawString(text, px, (float) y);
    }

    private void drawTextShadowed(Graphics2D g2, String text, double x, double y,
                                  TextAnchor anchor, Color color, float shadowOffset) {
        if (text == null || text.isEmpty()) return;
        if (shadowOffset > 0) {
            drawText(g2, text, x + shadowOffset, y + shadowOffset, anchor,
                    new Color(0, 0, 0, 130));
        }
        drawText(g2, text, x, y, anchor, color);
    }

    private static Color withAlpha(Color c, float alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (255 * alpha));
    }

    // ------------------------------------------------------------- hijri

    /** English (transliterated) Hijri month names as used by Tawkit. */
    public static final class TawkitHijriNames {
        public static final String[] ENGLISH = {
                "Muharram", "Safar", "Rabi' Al-Awwal", "Rabi' Al-Thani",
                "Jumada Al-Awwal", "Jumada Al-Thani", "Rajab", "Sha'ban",
                "Ramadan", "Shawwal", "Dhul'Qidah", "Dhul'Hijjah"
        };
    }
}
