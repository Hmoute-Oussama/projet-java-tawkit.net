package com.tawkit.view;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;

/**
 * "Next prayer" block in the Tawkit style: the prayer name, a large digital
 * countdown, and a circular progress ring around it (like the SVG ring in
 * tawkit.net). Doubles as the iqama countdown when the adhan has fired.
 */
public class NextPrayerPanel extends JComponent {

    private static final Color RING_BG = new Color(45, 60, 85);
    private static final Color RING_FG = new Color(120, 230, 140);
    private static final Color TEXT = new Color(255, 245, 210);
    private static final Color SUBTLE = new Color(170, 190, 215);

    private String prayerName = "";
    private String countdown = "--:--:--";
    private String iqamaInfo = "";
    private double progress = 0.0;   // 0..1, 1 = ring full
    private boolean big = false;

    public NextPrayerPanel() {
        setFontBase();
    }

    private void setFontBase() {
        setFont(new Font("SansSerif", Font.BOLD, 26));
    }

    public void setPrayerName(String name) {
        this.prayerName = name == null ? "" : name;
        repaint();
    }

    public void setCountdown(String text) {
        this.countdown = text == null || text.isBlank() ? "--:--:--" : text;
        repaint();
    }

    public void setIqamaInfo(String text) {
        this.iqamaInfo = text == null ? "" : text;
        repaint();
    }

    /** progress 0..1 of time elapsed toward the next event. */
    public void setProgress(double progress) {
        this.progress = Math.max(0.0, Math.min(1.0, progress));
        repaint();
    }

    /** Enlarged mode (user clicked, like Tawkit). */
    public void setBig(boolean big) {
        this.big = big;
        revalidate();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int side = Math.min(getWidth(), getHeight());
        int ringSize = (int) (side * (big ? 0.92 : 0.82));
        int x = (getWidth() - ringSize) / 2;
        int y = (getHeight() - ringSize) / 2;

        // Background ring
        g2.setStroke(new BasicStroke(Math.max(3, ringSize / 24f)));
        g2.setColor(RING_BG);
        g2.draw(new Arc2D.Double(x, y, ringSize, ringSize, 0, 360, Arc2D.OPEN));

        // Progress arc
        if (progress > 0) {
            g2.setColor(RING_FG);
            g2.draw(new Arc2D.Double(x, y, ringSize, ringSize, 90, -360.0 * progress, Arc2D.OPEN));
        }

        // Texts
        Font titleFont = new Font("SansSerif", Font.BOLD, (int) (ringSize * (big ? 0.16 : 0.13)));
        Font timeFont = new Font("SansSerif", Font.BOLD, (int) (ringSize * (big ? 0.22 : 0.19)));
        Font subFont = new Font("SansSerif", Font.PLAIN, (int) (ringSize * 0.10));

        FontMetrics tm = g2.getFontMetrics(titleFont);
        FontMetrics ctm = g2.getFontMetrics(timeFont);
        FontMetrics sm = g2.getFontMetrics(subFont);

        int cx = getWidth() / 2;
        int cy = getHeight() / 2;

        g2.setFont(titleFont);
        g2.setColor(SUBTLE);
        g2.drawString(prayerName, cx - tm.stringWidth(prayerName) / 2f,
                cy - ctm.getHeight() / 2f - tm.getDescent() - 6);

        g2.setFont(timeFont);
        g2.setColor(TEXT);
        g2.drawString(countdown, cx - ctm.stringWidth(countdown) / 2f,
                cy + ctm.getAscent() / 2f);

        if (!iqamaInfo.isEmpty()) {
            g2.setFont(subFont);
            g2.setColor(RING_FG);
            g2.drawString(iqamaInfo, cx - sm.stringWidth(iqamaInfo) / 2f,
                    cy + ctm.getDescent() + sm.getAscent() + 4);
        }

        g2.dispose();
    }
}
