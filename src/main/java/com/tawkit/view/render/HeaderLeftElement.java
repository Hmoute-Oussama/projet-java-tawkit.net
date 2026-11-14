package com.tawkit.view.render;

import com.tawkit.model.weather.WeatherData;
import com.tawkit.view.TawkitTheme;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Top-left cluster: hamburger menu, temperature, drawn sun icon, geographic
 * coordinates and the small flag — exactly the compact stack of the reference.
 */
public final class HeaderLeftElement implements DashboardElement {

    private String tempText = "--°";
    private String coordsText = "";
    private boolean hasWeather = false;

    @Override
    public void render(Graphics2D g2, RenderContext ctx) {
        float x = (float) (2.0 * ctx.vw());
        double vw = ctx.vw();
        double vh = ctx.vh();

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
        g2.setFont(TawkitTheme.serif(java.awt.Font.BOLD, 1.9f * (float) vw));
        TextDraw.draw(g2, tempText, x, tempY, TextDraw.Anchor.LEFT, TawkitTheme.WARM_WHITE);
        paintSun(g2, x + 5.0f * (float) vw, tempY - 0.7f * (float) vh, 0.95f * (float) vw);

        // Coordinates
        g2.setFont(TawkitTheme.serif(java.awt.Font.PLAIN, 1.05f * (float) vw));
        TextDraw.draw(g2, coordsText, x, (float) (11.0 * vh), TextDraw.Anchor.LEFT, TawkitTheme.GRAY);

        // Small Jordan-style flag
        paintFlag(g2, x, (float) (12.8 * vh), (float) (2.2 * vw), (float) (1.4 * vw));
    }

    /** True when the click hits the hamburger zone (opens settings). */
    public boolean isMenuClick(int x, int y, RenderContext ctx) {
        return x < 4.5 * ctx.vw() && y < 6.5 * ctx.vh();
    }

    public void setWeather(WeatherData data) {
        if (data == null) {
            hasWeather = false;
        } else {
            hasWeather = true;
            tempText = Math.round(data.getCurrentTemp()) + "°";
        }
    }

    public void setCityInfo(double latitude, double longitude) {
        this.coordsText = formatDms(latitude, "N", "S") + "   " + formatDms(longitude, "E", "W");
    }

    private String formatDms(double degrees, String positiveHemi, String negativeHemi) {
        int deg = (int) Math.floor(Math.abs(degrees));
        int min = (int) Math.round((Math.abs(degrees) - deg) * 60);
        if (min >= 60) {
            deg += 1;
            min -= 60;
        }
        return (degrees >= 0 ? positiveHemi : negativeHemi) + " " + deg + "° "
                + String.format("%02d'", min);
    }

    /** Small sun: yellow disc with rays (drawn, never a tofu-box emoji). */
    private void paintSun(Graphics2D g2, float cx, float cy, float r) {
        g2.setColor(TawkitTheme.SUN_YELLOW);
        g2.fill(new Ellipse2D.Float(cx - r * 0.55f, cy - r * 0.55f, r * 1.1f, r * 1.1f));
        g2.setStroke(new BasicStroke(Math.max(1f, r * 0.16f)));
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI / 4 * i;
            g2.drawLine(Math.round(cx + (float) Math.cos(angle) * r * 0.8f),
                    Math.round(cy + (float) Math.sin(angle) * r * 0.8f),
                    Math.round(cx + (float) Math.cos(angle) * r * 1.35f),
                    Math.round(cy + (float) Math.sin(angle) * r * 1.35f));
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
        Path2D triangle = new Path2D.Float();
        triangle.moveTo(x, y);
        triangle.lineTo(x + fw * 0.45f, y + fh / 2f);
        triangle.lineTo(x, y + fh);
        triangle.closePath();
        g2.fill(triangle);
        g2.setColor(white);
        float starR = fh * 0.10f;
        g2.fill(new Ellipse2D.Float(x + fw * 0.12f - starR, y + fh / 2f - starR, starR * 2, starR * 2));
    }

    boolean hasWeather() {
        return hasWeather;
    }
}
