package com.tawkit.view;

import com.tawkit.model.common.HijriDate;
import com.tawkit.model.content.ContentItem;
import com.tawkit.model.prayer.DailyPrayerTimes;
import com.tawkit.model.prayer.PrayerType;
import com.tawkit.model.weather.WeatherData;
import com.tawkit.service.format.TimeText;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.time.LocalDate;
import java.util.List;

/**
 * Main application window hosting the Tawkit dashboard screen.
 * Supports full-screen kiosk mode (F11), screen dimming during prayer,
 * custom background images, user slides overlay, and reminder alerts.
 */
public class MainFrame extends JFrame {

    private final TawkitScreen screen;
    private final SlidesPanel slidesPanel;
    private final CardLayout cards;
    private final JPanel cardHolder;

    private final JComponent dimOverlay;
    private final JLabel alertLabel;
    private final Timer alertTimer;

    private boolean fullScreen = false;
    private Rectangle savedBounds;
    private int savedExtendedState;
    private Runnable settingsOpener;

    public MainFrame() {
        super("Tawkit - Prayer Times");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        screen = new TawkitScreen();
        screen.setOnMenuClick(this::openSettings);

        slidesPanel = new SlidesPanel(new File(System.getProperty("user.home"), ".tawkit/slides"));

        cards = new CardLayout();
        cardHolder = new JPanel(cards);
        cardHolder.setOpaque(false);
        cardHolder.add(screen, "screen");
        cardHolder.add(slidesPanel, "slides");
        setContentPane(cardHolder);

        // ---- Dim overlay + 12s alert (glass pane) ----
        dimOverlay = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(new Color(0, 0, 0, 175));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        dimOverlay.setVisible(false);
        dimOverlay.setLayout(new BorderLayout());

        alertLabel = new JLabel("", SwingConstants.CENTER);
        alertLabel.setFont(TawkitTheme.serif(Font.BOLD, 34));
        alertLabel.setForeground(TawkitTheme.GOLDEN_BEIGE);
        alertLabel.setBorder(BorderFactory.createEmptyBorder(18, 40, 18, 40));
        alertLabel.setOpaque(true);
        alertLabel.setBackground(new Color(10, 10, 10));
        JPanel alertWrapper = new JPanel(new GridBagLayout());
        alertWrapper.setOpaque(false);
        alertWrapper.add(alertLabel);
        dimOverlay.add(alertWrapper, BorderLayout.NORTH);

        alertTimer = new Timer(12_000, e -> {
            alertLabel.setText("");
            dimOverlay.setVisible(false);
        });
        alertTimer.setRepeats(false);

        setGlassPane(dimOverlay);

        installKeyBindings();

        setSize(1280, 720);
        setMinimumSize(new Dimension(800, 450));
        setLocationRelativeTo(null);
        screen.applyTheme("night");
    }

    private void installKeyBindings() {
        JRootPane rp = getRootPane();
        rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0), "toggleFullScreen");
        rp.getActionMap().put("toggleFullScreen", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                toggleFullScreen();
            }
        });
        rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "exitFullScreen");
        rp.getActionMap().put("exitFullScreen", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (fullScreen) toggleFullScreen();
            }
        });
        rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK), "quit");
        rp.getActionMap().put("quit", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                System.exit(0);
            }
        });
    }

    public void toggleFullScreen() {
        if (!fullScreen) {
            savedBounds = getBounds();
            savedExtendedState = getExtendedState();
            dispose();
            setUndecorated(true);
            setExtendedState(JFrame.MAXIMIZED_BOTH);
            fullScreen = true;
            setVisible(true);
        } else {
            dispose();
            setUndecorated(false);
            setBounds(savedBounds);
            setExtendedState(savedExtendedState);
            fullScreen = false;
            setVisible(true);
        }
    }

    public void openSettings() {
        if (settingsOpener != null) {
            settingsOpener.run();
        }
    }

    /** Lets the controller intercept the settings dialog. */
    public void setSettingsOpener(Runnable opener) {
        this.settingsOpener = opener;
    }

    // ---------------- Public update methods (controller calls these) ----

    public void setMosqueName(String name) {
        screen.setMosqueName(name);
    }

    /** City / coordinates info shown top-left under the weather. */
    public void setCityInfo(String cityName, double latitude, double longitude) {
        screen.setCityInfo(cityName, latitude, longitude);
    }

    private boolean use24HourFlag = false;
    private boolean secondsFlag = true;

    public void setUse24Hour(boolean use24) {
        use24HourFlag = use24;
        screen.setClockMode(use24HourFlag, secondsFlag);
    }

    public void setShowSeconds(boolean show) {
        secondsFlag = show;
        screen.setClockMode(use24HourFlag, secondsFlag);
    }

    public void setHijriDate(HijriDate hijri) {
        // combined with the Gregorian date in setDate; stored until both known
        this.pendingHijri = hijri;
        refreshDate();
    }

    private HijriDate pendingHijri;
    private LocalDate pendingGregorian;

    public void setGregorianDate(LocalDate date) {
        this.pendingGregorian = date;
        refreshDate();
    }

    private void refreshDate() {
        if (pendingGregorian == null || pendingHijri == null) return;
        screen.setDate(pendingGregorian, pendingHijri.getDay(),
                pendingHijri.getMonth(), pendingHijri.getYear());
    }

    public void setWeather(WeatherData data) {
        screen.setWeather(data);
    }

    public void setPrayerTimes(DailyPrayerTimes times) {
        screen.setPrayerTimes(times);
    }

    public void setHighlightedPrayer(PrayerType type) {
        // highlighted by the countdown circle label in this design
    }

    public void setIqamaCountdown(String text) {
        // iqama countdown is merged into the circle by setNextPrayer
    }

    public void setNextPrayer(String name, String countdown, double progress, String iqamaInfo) {
        boolean iqamaActive = iqamaInfo != null && !iqamaInfo.isBlank();
        String digits;
        String label;
        if (iqamaActive) {
            digits = iqamaInfo.replace("Iqama", "").trim();
            label = "Iqama";
        } else {
            digits = countdown == null ? "--:--" : countdown;
            // H:MM:SS -> H:MM (the circle shows h:mm in the reference design)
            digits = TimeText.withoutSeconds(digits);
            label = name == null || name.isBlank() ? "Next Prayer" : name;
        }
        screen.setCountdown(digits, label, progress);
    }

    public void setContentItems(List<ContentItem> items) {
        boolean arabic = "ar".equalsIgnoreCase(
                com.tawkit.service.localization.Messages.getLocale().getLanguage());
        screen.setVerses(items, arabic);
    }

    public void setRotationSeconds(int seconds) {
        screen.setRotationSeconds(seconds);
    }

    public void setSlides(List<com.tawkit.model.content.Slide> slides) {
        slidesPanel.setSlides(slides);
    }

    public void showSlides() {
        cards.show(cardHolder, "slides");
        slidesPanel.setActive(true);
    }

    public void showContent() {
        slidesPanel.setActive(false);
        cards.show(cardHolder, "screen");
    }

    public void setTickerMessages(List<String> messages) {
        screen.setFooterTexts(messages);
    }

    public void setDimmed(boolean dimmed) {
        if (dimmed && alertLabel.getText() != null && !alertLabel.getText().isEmpty()) {
            return; // alert display has priority
        }
        getGlassPane().setVisible(dimmed);
    }

    /** Shows a reminder message for 12 seconds over the screen. */
    public void showAlertMessage(String message) {
        if (message == null || message.isBlank()) return;
        alertLabel.setText(message);
        getGlassPane().setVisible(true);
        alertTimer.restart();
    }

    public void applyTheme(String theme) {
        screen.applyTheme(theme);
    }

    public void applyCustomBackground(String path) {
        screen.applyCustomImage(path);
    }

    /** RTL for Arabic; LTR otherwise (screen handles text direction internally). */
    public void setLanguage(String code) {
        // The screen renders fixed-language content per the reference design;
        // verse translation/Arabic selection happens in setContentItems.
    }

    public TawkitScreen getScreen() {
        return screen;
    }
}
