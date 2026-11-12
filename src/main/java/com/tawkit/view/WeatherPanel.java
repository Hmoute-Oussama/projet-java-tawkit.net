package com.tawkit.view;

import com.tawkit.model.weather.WeatherData;
import com.tawkit.model.weather.WeatherData.WeatherDay;

import javax.swing.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;

/**
 * Weather widget: current temperature on the left, a 7-day forecast strip on
 * the right. Styled to blend into the dark kiosk screen.
 */
public class WeatherPanel extends JPanel {

    private static final Color TEXT = new Color(225, 230, 240);
    private static final Color SUBTLE = new Color(150, 165, 190);

    private final JLabel iconLabel = new JLabel("-", SwingConstants.CENTER);
    private final JLabel tempLabel = new JLabel("--°", SwingConstants.CENTER);
    private final JPanel forecastStrip = new JPanel(new GridLayout(1, 7, 10, 0));

    public WeatherPanel() {
        setLayout(new BorderLayout(16, 0));
        setOpaque(false);

        JPanel current = new JPanel(new BorderLayout(4, 0));
        current.setOpaque(false);
        iconLabel.setFont(new Font("Dialog", Font.PLAIN, 42));
        iconLabel.setForeground(TEXT);
        tempLabel.setFont(new Font("SansSerif", Font.BOLD, 34));
        tempLabel.setForeground(TEXT);
        current.add(iconLabel, BorderLayout.WEST);
        current.add(tempLabel, BorderLayout.CENTER);

        forecastStrip.setOpaque(false);
        for (int i = 0; i < 7; i++) {
            JLabel dayLabel = new JLabel("", SwingConstants.CENTER);
            dayLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
            dayLabel.setForeground(SUBTLE);
            forecastStrip.add(dayLabel);
        }

        add(current, BorderLayout.WEST);
        add(forecastStrip, BorderLayout.CENTER);
    }

    public void setWeather(WeatherData data) {
        if (data == null) return;
        iconLabel.setText(WeatherData.iconFor(data.getCurrentCode()));
        iconLabel.setToolTipText(WeatherData.descriptionFor(data.getCurrentCode())
                + " — wind " + Math.round(data.getCurrentWindKmh()) + " km/h");
        tempLabel.setText(Math.round(data.getCurrentTemp()) + "°");

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEE d");
        Component[] comps = forecastStrip.getComponents();
        var days = data.getDays();
        for (int i = 0; i < comps.length && i < days.size(); i++) {
            WeatherDay d = days.get(i);
            JLabel label = (JLabel) comps[i];
            label.setText("<html><div style='text-align:center'>"
                    + d.date().format(fmt)
                    + "<br><span style='font-size:16px'>" + WeatherData.iconFor(d.code()) + "</span>"
                    + "<br>" + Math.round(d.maxTemp()) + "°"
                    + "<br><span style='color:rgb(150,165,190)'>" + Math.round(d.minTemp()) + "°"
                    + "</span></div></html>");
            label.setToolTipText(WeatherData.descriptionFor(d.code())
                    + " — " + d.precipitationMm() + " mm");
        }
    }
}
