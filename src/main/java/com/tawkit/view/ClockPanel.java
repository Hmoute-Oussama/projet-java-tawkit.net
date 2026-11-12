package com.tawkit.view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Large digital clock displayed at the top-center of the main screen.
 * Updates every second.
 */
public class ClockPanel extends JPanel {

    private final JLabel timeLabel;
    private final JLabel secondsLabel;
    private final JLabel amPmLabel;
    private boolean showSeconds = true;
    private boolean use24Hour = true;

    public ClockPanel() {
        setLayout(new BorderLayout());
        setOpaque(false);

        timeLabel = new JLabel();
        timeLabel.setFont(new Font("SansSerif", Font.BOLD, 120));
        timeLabel.setForeground(new Color(255, 235, 180));
        timeLabel.setHorizontalAlignment(SwingConstants.CENTER);

        secondsLabel = new JLabel();
        secondsLabel.setFont(new Font("SansSerif", Font.PLAIN, 36));
        secondsLabel.setForeground(new Color(180, 180, 180));
        secondsLabel.setHorizontalAlignment(SwingConstants.CENTER);

        amPmLabel = new JLabel();
        amPmLabel.setFont(new Font("SansSerif", Font.PLAIN, 40));
        amPmLabel.setForeground(new Color(200, 200, 200));
        amPmLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // time + AM/PM in the center; seconds below
        JPanel timeRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        timeRow.setOpaque(false);
        timeRow.add(timeLabel);
        timeRow.add(amPmLabel);

        add(timeRow, BorderLayout.CENTER);
        add(secondsLabel, BorderLayout.SOUTH);

        startTimer();
        updateDisplay();
    }

    private void startTimer() {
        Timer timer = new Timer(1000, (ActionEvent e) -> updateDisplay());
        timer.setRepeats(true);
        timer.start();
    }

    private void updateDisplay() {
        LocalTime now = LocalTime.now();

        if (use24Hour) {
            amPmLabel.setText("");
            timeLabel.setText(now.format(DateTimeFormatter.ofPattern("HH:mm")));
        } else {
            int hour = now.getHour();
            String amPm = hour < 12 ? "AM" : "PM";
            amPmLabel.setText(amPm);
            int displayHour = hour % 12;
            if (displayHour == 0) displayHour = 12;
            timeLabel.setText(String.format("%02d:%02d", displayHour, now.getMinute()));
        }

        if (showSeconds) {
            secondsLabel.setText(String.format(":%02d", now.getSecond()));
        } else {
            secondsLabel.setText("");
        }
    }

    public void setShowSeconds(boolean showSeconds) {
        this.showSeconds = showSeconds;
        updateDisplay();
    }

    public void setUse24Hour(boolean use24Hour) {
        this.use24Hour = use24Hour;
        updateDisplay();
    }
}
