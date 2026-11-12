package com.tawkit.view;

import com.tawkit.model.common.HijriDate;
import com.tawkit.model.content.ContentItem;
import com.tawkit.model.prayer.DailyPrayerTimes;
import com.tawkit.model.prayer.PrayerType;
import com.tawkit.model.weather.WeatherData;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/**
 * Main application window, in the Tawkit kiosk style:
 *
 * <pre>
 *   [ mosque name ]                    [ weather ]
 *        [ big clock + dates ]
 *   [ next-prayer ring ]  [ prayer times grid ]
 *   [ content: verses/hadith/azkar or slides ]
 *   [ announcements ticker ]
 * </pre>
 *
 * Supports full-screen mode, screen dimming during prayer, background
 * themes/custom image, and RTL layout for Arabic.
 */
public class MainFrame extends JFrame {

    private static final Color BG_COLOR = new Color(8, 16, 32);

    private final JPanel root;                       // themed background
    private final JLabel mosqueLabel;
    private final ClockPanel clockPanel;
    private final DatePanel datePanel;
    private final WeatherPanel weatherPanel;
    private final NextPrayerPanel nextPrayerPanel;
    private final PrayerTimesPanel prayerTimesPanel;
    private final ContentPanel contentPanel;
    private final SlidesPanel slidesPanel;
    private final TickerPanel tickerPanel;

    private final JComponent dimOverlay;
    private final CardLayout contentCards;
    private final JPanel contentHolder;

    private boolean fullScreen = false;
    private boolean ringBig = false;
    private boolean use24HourMenu = true;
    private Runnable settingsOpener;
    private JLabel alertLabel;
    private Timer alertTimer;
    private Rectangle savedBounds;
    private int savedExtendedState;

    public MainFrame() {
        super("Tawkit — Prayer Times");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        root = new BackgroundPanel();
        root.setLayout(new BorderLayout(0, 0));
        setContentPane(root);

        // ---- Header: mosque name, dates, weather ----
        JPanel header = new JPanel(new BorderLayout(20, 0));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(12, 20, 4, 20));

        mosqueLabel = new JLabel("", SwingConstants.LEFT);
        mosqueLabel.setFont(new Font("SansSerif", Font.BOLD, 28));
        mosqueLabel.setForeground(new Color(220, 228, 245));

        datePanel = new DatePanel();
        datePanel.setFontSizes(20);

        weatherPanel = new WeatherPanel();

        JPanel headerCenter = new JPanel(new BorderLayout(20, 0));
        headerCenter.setOpaque(false);
        headerCenter.add(datePanel, BorderLayout.WEST);
        headerCenter.add(mosqueLabel, BorderLayout.CENTER);

        header.add(headerCenter, BorderLayout.CENTER);
        header.add(weatherPanel, BorderLayout.EAST);

        // ---- Clock ----
        clockPanel = new ClockPanel();
        clockPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel north = new JPanel();
        north.setOpaque(false);
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(header);
        north.add(clockPanel);

        // ---- Center: next-prayer ring + prayer grid ----
        nextPrayerPanel = new NextPrayerPanel();
        nextPrayerPanel.setPreferredSize(new Dimension(320, 260));
        nextPrayerPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                ringBig = !ringBig;
                nextPrayerPanel.setBig(ringBig);   // click = enlarge, like Tawkit
            }
        });

        prayerTimesPanel = new PrayerTimesPanel();
        prayerTimesPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JPanel center = new JPanel(new BorderLayout(20, 0));
        center.setOpaque(false);
        center.add(nextPrayerPanel, BorderLayout.WEST);
        center.add(prayerTimesPanel, BorderLayout.CENTER);

        // ---- South: content (verses / slides) + ticker ----
        contentPanel = new ContentPanel();
        slidesPanel = new SlidesPanel(new File(System.getProperty("user.home"), ".tawkit/slides"));

        contentCards = new CardLayout();
        contentHolder = new JPanel(contentCards);
        contentHolder.setOpaque(false);
        contentHolder.add(contentPanel, "content");
        contentHolder.add(slidesPanel, "slides");

        tickerPanel = new TickerPanel();

        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        south.add(contentHolder, BorderLayout.CENTER);
        south.add(tickerPanel, BorderLayout.SOUTH);

        root.add(north, BorderLayout.NORTH);
        root.add(center, BorderLayout.CENTER);
        root.add(south, BorderLayout.SOUTH);

        // ---- Dim overlay (glass pane) with 12s alert message ----
        dimOverlay = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(new Color(0, 0, 0, 170));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        dimOverlay.setVisible(false);
        dimOverlay.setLayout(new BorderLayout());

        alertLabel = new JLabel("", SwingConstants.CENTER);
        alertLabel.setFont(new Font("SansSerif", Font.BOLD, 34));
        alertLabel.setForeground(new Color(255, 240, 180));
        alertLabel.setBorder(BorderFactory.createEmptyBorder(18, 40, 18, 40));
        alertLabel.setOpaque(true);
        alertLabel.setBackground(new Color(20, 40, 30));
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

        buildMenuBar();
        installKeyBindings();

        setSize(1280, 720);
        setMinimumSize(new Dimension(800, 450));
        setLocationRelativeTo(null);
        applyTheme("night");
        datePanel.setGregorianDate(LocalDate.now());
    }

    private boolean isRingBig() {
        return ringBig;
    }

    private void buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu(com.tawkit.service.localization.Messages.get("app.file"));
        JMenuItem exitItem = new JMenuItem(com.tawkit.service.localization.Messages.get("app.exit"));
        exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK));
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);

        JMenu viewMenu = new JMenu(com.tawkit.service.localization.Messages.get("app.view"));
        JMenuItem fullScreenItem = new JMenuItem(com.tawkit.service.localization.Messages.get("app.fullscreen"));
        fullScreenItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0));
        fullScreenItem.addActionListener(e -> toggleFullScreen());
        viewMenu.add(fullScreenItem);
        viewMenu.addSeparator();
        JMenuItem toggle24hItem = new JMenuItem(com.tawkit.service.localization.Messages.get("app.clock"));
        toggle24hItem.addActionListener(e -> {
            use24HourMenu = !use24HourMenu;
            clockPanel.setUse24Hour(use24HourMenu);
        });
        viewMenu.add(toggle24hItem);

        JMenu settingsMenu = new JMenu(com.tawkit.service.localization.Messages.get("app.settings"));
        JMenuItem prefsItem = new JMenuItem(com.tawkit.service.localization.Messages.get("app.preferences"));
        prefsItem.addActionListener(e -> openSettings());
        settingsMenu.add(prefsItem);

        JMenu helpMenu = new JMenu(com.tawkit.service.localization.Messages.get("app.help"));
        JMenuItem aboutItem = new JMenuItem(com.tawkit.service.localization.Messages.get("app.about"));
        aboutItem.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "<html><b>Tawkit Desktop</b><br>Free prayer times app<br>Inspired by tawkit.net</html>",
                "About", JOptionPane.INFORMATION_MESSAGE));
        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(viewMenu);
        menuBar.add(settingsMenu);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }

    private void installKeyBindings() {
        KeyStroke f11 = KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(f11, "toggleFullScreen");
        getRootPane().getActionMap().put("toggleFullScreen", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                toggleFullScreen();
            }
        });

        KeyStroke esc = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(esc, "exitFullScreen");
        getRootPane().getActionMap().put("exitFullScreen", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (fullScreen) toggleFullScreen();
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
        } else {
            SettingsDialog dialog = new SettingsDialog(this);
            dialog.setVisible(true);
        }
    }

    /** Lets the controller intercept the settings dialog. */
    public void setSettingsOpener(Runnable opener) {
        this.settingsOpener = opener;
    }

    // ---- Public update methods (controller calls these) ----

    public void setMosqueName(String name) {
        mosqueLabel.setText(name == null || name.isBlank() ? "" : name);
    }

    public void setUse24Hour(boolean use24) {
        clockPanel.setUse24Hour(use24);
    }

    public void setShowSeconds(boolean show) {
        clockPanel.setShowSeconds(show);
    }

    public void setHijriDate(HijriDate hijri) {
        datePanel.setHijriDate(hijri);
    }

    public void setGregorianDate(LocalDate date) {
        datePanel.setGregorianDate(date);
    }

    public void setWeather(WeatherData data) {
        weatherPanel.setWeather(data);
    }

    public void setPrayerTimes(DailyPrayerTimes times) {
        prayerTimesPanel.setPrayerTimes(times);
    }

    public void setHighlightedPrayer(PrayerType type) {
        prayerTimesPanel.setHighlightedPrayer(type);
    }

    public PrayerTimesPanel getPrayerTimesPanel() {
        return prayerTimesPanel;
    }

    public void setIqamaCountdown(String text) {
        prayerTimesPanel.setIqamaCountdown(text);
    }

    public void setNextPrayer(String name, String countdown, double progress, String iqamaInfo) {
        nextPrayerPanel.setPrayerName(name);
        nextPrayerPanel.setCountdown(countdown);
        nextPrayerPanel.setProgress(progress);
        nextPrayerPanel.setIqamaInfo(iqamaInfo);
    }

    public void setContentItems(List<ContentItem> items) {
        contentPanel.setItems(items);
        contentPanel.setRotationSeconds(18);
        showContent();
    }

    public void setRotationSeconds(int seconds) {
        contentPanel.setRotationSeconds(seconds);
    }

    public void setSlides(List<com.tawkit.model.content.Slide> slides) {
        slidesPanel.setSlides(slides);
    }

    public void showSlides() {
        contentCards.show(contentHolder, "slides");
        slidesPanel.setActive(true);
    }

    public void showContent() {
        slidesPanel.setActive(false);
        contentCards.show(contentHolder, "content");
    }

    public void setTickerMessages(List<String> messages) {
        tickerPanel.setMessages(messages);
    }

    public void setDimmed(boolean dimmed) {
        if (alertLabel != null && !alertLabel.getText().isEmpty()) {
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
        ((BackgroundPanel) root).setTheme(theme);
        getContentPane().revalidate();
        getContentPane().repaint();
    }

    public void applyCustomBackground(String path) {
        ((BackgroundPanel) root).setCustomImage(path);
        getContentPane().revalidate();
        getContentPane().repaint();
    }

    /** RTL for Arabic; LTR otherwise. */
    public void setLanguage(String code) {
        Locale locale = "ar".equalsIgnoreCase(code)
                ? new Locale("ar") : new Locale(code == null ? "en" : code);
        setComponentOrientation(ComponentOrientation.getOrientation(locale));
    }

    private static final class BackgroundPanel extends JPanel {
        private String theme = "night";
        private transient BufferedImage customImage;

        BackgroundPanel() {
            setBackground(BG_COLOR);
            setOpaque(true);
        }

        void setTheme(String t) {
            this.theme = t == null ? "night" : t;
            this.customImage = null;
            repaint();
        }

        void setCustomImage(String path) {
            if (path == null || path.isBlank()) return;
            try {
                customImage = ImageIO.read(new File(path));
            } catch (Exception e) {
                System.err.println("[Background] cannot load " + path);
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (customImage != null) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                double scale = Math.max(getWidth() / (double) customImage.getWidth(),
                        getHeight() / (double) customImage.getHeight());
                int w = (int) (customImage.getWidth() * scale);
                int h = (int) (customImage.getHeight() * scale);
                g2.drawImage(customImage, (getWidth() - w) / 2, (getHeight() - h) / 2, w, h, null);
                return;
            }
            GradientPaint paint = switch (theme) {
                case "green" -> new GradientPaint(0, 0, new Color(6, 34, 22),
                        getWidth(), getHeight(), new Color(12, 55, 35));
                case "blue" -> new GradientPaint(0, 0, new Color(8, 20, 45),
                        getWidth(), getHeight(), new Color(18, 45, 85));
                default -> new GradientPaint(0, 0, new Color(6, 12, 26),
                        getWidth(), getHeight(), new Color(16, 30, 55));
            };
            ((Graphics2D) g).setPaint(paint);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }
}
