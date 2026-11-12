package com.tawkit.view;

import com.tawkit.model.common.HijriDate;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Displays the Hijri and Gregorian dates below the clock.
 */
public class DatePanel extends JPanel {

    private final JLabel hijriLabel;
    private final JLabel gregorianLabel;

    public DatePanel() {
        setLayout(new FlowLayout(FlowLayout.CENTER, 30, 2));
        setOpaque(false);

        hijriLabel = new JLabel("—");
        hijriLabel.setFont(new Font("SansSerif", Font.PLAIN, 24));
        hijriLabel.setForeground(new Color(220, 220, 220));

        gregorianLabel = new JLabel("—");
        gregorianLabel.setFont(new Font("SansSerif", Font.PLAIN, 24));
        gregorianLabel.setForeground(new Color(200, 200, 200));

        add(hijriLabel);
        add(gregorianLabel);
    }

    /** Adjusts both date labels to a smaller size for the header row. */
    public void setFontSizes(int size) {
        hijriLabel.setFont(new Font("SansSerif", Font.PLAIN, size));
        gregorianLabel.setFont(new Font("SansSerif", Font.PLAIN, size));
    }

    public void setHijriDate(HijriDate hijri) {
        hijriLabel.setText(hijri == null ? "" : hijri.toDisplayString());
    }

    public void setGregorianDate(LocalDate date) {
        if (date == null) {
            gregorianLabel.setText("");
            return;
        }
        String formatted = date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH));
        gregorianLabel.setText(formatted);
    }
}
