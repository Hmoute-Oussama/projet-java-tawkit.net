package com.tawkit.view;

import com.tawkit.model.prayer.DailyPrayerTimes;
import com.tawkit.model.prayer.PrayerEntry;
import com.tawkit.model.prayer.PrayerType;

import javax.swing.*;
import java.awt.*;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.Map;

/**
 * Displays the six daily prayer times (Fajr, Shuruq, Dhuhr, Asr, Maghrib, Isha)
 * with adhan and iqama times in a grid. The next upcoming prayer is highlighted.
 */
public class PrayerTimesPanel extends JPanel {

    private static final Color BG_COLOR = new Color(15, 25, 45);
    private static final Color HIGHLIGHT_BG = new Color(20, 60, 40);
    private static final Color TEXT_COLOR = new Color(255, 255, 255);
    private static final Color SECONDARY_COLOR = new Color(170, 190, 210);
    private static final Color HIGHLIGHT_TEXT = new Color(120, 230, 140);
    private static final Color PAST_COLOR = new Color(110, 125, 150);

    private final Map<PrayerType, JPanel> rowPanels = new EnumMap<>(PrayerType.class);
    private final Map<PrayerType, JLabel> nameLabels = new EnumMap<>(PrayerType.class);
    private final Map<PrayerType, JLabel> adhanLabels = new EnumMap<>(PrayerType.class);
    private final Map<PrayerType, JLabel> iqamaLabels = new EnumMap<>(PrayerType.class);
    private DailyPrayerTimes lastSetTimes;

    private PrayerType highlightedPrayer = null;
    private LocalTime iqamaCountdownEnd = null;
    private final JLabel countdownLabel;

    public PrayerTimesPanel() {
        setLayout(new BorderLayout());
        setOpaque(false);

        // Grid for the six prayers: 3 columns x 2 rows
        JPanel grid = new JPanel(new GridLayout(2, 3, 8, 8));
        grid.setOpaque(false);

        for (PrayerType type : PrayerType.values()) {
            JPanel cell = createPrayerCell(type);
            rowPanels.put(type, cell);
            grid.add(cell);
        }

        add(grid, BorderLayout.CENTER);

        // Iqama countdown shown at the bottom
        countdownLabel = new JLabel("");
        countdownLabel.setFont(new Font("SansSerif", Font.BOLD, 28));
        countdownLabel.setForeground(HIGHLIGHT_TEXT);
        countdownLabel.setHorizontalAlignment(SwingConstants.CENTER);
        countdownLabel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        add(countdownLabel, BorderLayout.SOUTH);
    }

    private JPanel createPrayerCell(PrayerType type) {
        JPanel cell = new JPanel(new BorderLayout());
        cell.setBackground(BG_COLOR);
        cell.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(40, 60, 90), 1),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

        JLabel nameLabel = new JLabel(
                com.tawkit.service.localization.Messages.prayerName(type), SwingConstants.CENTER);
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 26));
        nameLabel.setForeground(TEXT_COLOR);

        JPanel timesPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        timesPanel.setOpaque(false);

        JLabel adhanLabel = new JLabel("--:--", SwingConstants.CENTER);
        adhanLabel.setFont(new Font("SansSerif", Font.BOLD, 32));
        adhanLabel.setForeground(SECONDARY_COLOR);

        JLabel iqamaLabel = new JLabel("", SwingConstants.CENTER);
        iqamaLabel.setFont(new Font("SansSerif", Font.PLAIN, 20));
        iqamaLabel.setForeground(new Color(130, 150, 175));

        timesPanel.add(adhanLabel);
        timesPanel.add(iqamaLabel);

        cell.add(nameLabel, BorderLayout.NORTH);
        cell.add(timesPanel, BorderLayout.CENTER);

        if (type == PrayerType.SHURUQ) {
            iqamaLabel.setText("Sunrise");
            iqamaLabel.setForeground(new Color(180, 150, 80));
        }

        rowPanels.put(type, cell);
        nameLabels.put(type, nameLabel);
        adhanLabels.put(type, adhanLabel);
        iqamaLabels.put(type, iqamaLabel);

        return cell;
    }

    /**
     * Updates the displayed times from the given DailyPrayerTimes.
     */
    public void setPrayerTimes(DailyPrayerTimes times) {
        if (times == null) return;
        this.lastSetTimes = times;
        for (PrayerType type : PrayerType.values()) {
            PrayerEntry entry = times.get(type);
            if (entry != null) {
                adhanLabels.get(type).setText(entry.getAdhanDisplay());
                if (type != PrayerType.SHURUQ && entry.getIqamaTime() != null) {
                    iqamaLabels.get(type).setText("Iqama: " + entry.getIqamaDisplay());
                }
            }
        }
        markPastPrayers();
    }

    /** Past prayers shown in a softer color, like Tawkit. */
    public void markPastPrayers() {
        if (lastSetTimes == null) return;
        LocalTime now = LocalTime.now();
        for (PrayerType type : PrayerType.values()) {
            PrayerEntry entry = lastSetTimes.get(type);
            if (entry == null || entry.getAdhanTime() == null) continue;
            boolean past = entry.getAdhanTime().isBefore(now)
                    && type != highlightedPrayer;
            nameLabels.get(type).setForeground(
                    past ? PAST_COLOR : (type == highlightedPrayer ? HIGHLIGHT_TEXT : TEXT_COLOR));
            adhanLabels.get(type).setForeground(
                    past ? PAST_COLOR : (type == highlightedPrayer ? HIGHLIGHT_TEXT : SECONDARY_COLOR));
        }
    }

    /**
     * Highlights the next upcoming prayer.
     */
    public void setHighlightedPrayer(PrayerType type) {
        // Reset old highlight
        if (highlightedPrayer != null) {
            rowPanels.get(highlightedPrayer).setBackground(BG_COLOR);
            nameLabels.get(highlightedPrayer).setForeground(TEXT_COLOR);
            adhanLabels.get(highlightedPrayer).setForeground(SECONDARY_COLOR);
        }
        highlightedPrayer = type;
        if (type != null) {
            rowPanels.get(type).setBackground(HIGHLIGHT_BG);
            nameLabels.get(type).setForeground(HIGHLIGHT_TEXT);
            adhanLabels.get(type).setForeground(HIGHLIGHT_TEXT);
        }
    }

    /**
     * Shows an iqama countdown (e.g. "Iqama in 3:00").
     */
    public void setIqamaCountdown(String text) {
        countdownLabel.setText(text == null ? "" : text);
    }

    /** Re-applies localized prayer names (after a language change). */
    public void refreshNames() {
        for (PrayerType type : PrayerType.values()) {
            nameLabels.get(type).setText(
                    com.tawkit.service.localization.Messages.prayerName(type));
        }
    }

    public PrayerType getHighlightedPrayer() {
        return highlightedPrayer;
    }
}
