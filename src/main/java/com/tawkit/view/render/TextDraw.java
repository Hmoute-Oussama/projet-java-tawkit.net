package com.tawkit.view.render;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

/** Text drawing helpers shared by all dashboard elements. */
public final class TextDraw {

    /** Horizontal anchor of a drawn text. */
    public enum Anchor { LEFT, CENTER, RIGHT }

    private TextDraw() {
    }

    /** Draws {@code text} anchored at (x, y) — the baseline. */
    public static void draw(Graphics2D g2, String text, double x, double y,
                            Anchor anchor, Color color) {
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

    /** Draws {@code text} with a subtle drop shadow offset (in px). */
    public static void drawShadowed(Graphics2D g2, String text, double x, double y,
                                    Anchor anchor, Color color, float shadowOffset) {
        if (text == null || text.isEmpty()) return;
        if (shadowOffset > 0) {
            draw(g2, text, x + shadowOffset, y + shadowOffset, anchor,
                    new Color(0, 0, 0, 130));
        }
        draw(g2, text, x, y, anchor, color);
    }

    /** Greedy word-wrap into lines no wider than {@code maxWidth}px. */
    public static List<String> wrap(Graphics2D g2, String text, float maxWidth) {
        FontMetrics fm = g2.getFontMetrics();
        if (fm.stringWidth(text) <= maxWidth) return List.of(text);
        List<String> out = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split("\\s+")) {
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

    /** Same color with a fractional alpha (0..1). */
    public static Color withAlpha(Color c, float alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (255 * alpha));
    }
}
