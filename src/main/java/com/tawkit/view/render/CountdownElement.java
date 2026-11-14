package com.tawkit.view.render;

import com.tawkit.view.TawkitTheme;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;

/**
 * Upper-right countdown circle: dark interior, thin concentric borders,
 * golden-beige digits and a small serif label. No progress ring (per the
 * reference design).
 */
public final class CountdownElement implements DashboardElement {

    private String digits = "--:--";
    private String label = "Next Prayer";

    @Override
    public void render(Graphics2D g2, RenderContext ctx) {
        double cx = 87.5 * ctx.vw();
        double cy = 27 * ctx.vh();
        double r = 7.75 * ctx.vw();
        float vw = (float) ctx.vw();

        // Dark interior
        g2.setColor(new Color(5, 5, 5, 165));
        g2.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));

        // Thin concentric borders
        g2.setColor(new Color(197, 166, 106, 60));
        g2.setStroke(new BasicStroke(Math.max(1.2f, (float) (r * 0.014))));
        g2.draw(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
        g2.draw(new Ellipse2D.Double(cx - r * 0.86, cy - r * 0.86, r * 1.72, r * 1.72));

        // Digits
        g2.setFont(TawkitTheme.serif(Font.BOLD, 3.2f * vw));
        TextDraw.drawShadowed(g2, digits, cx, (float) (cy + 0.4 * ctx.vh()),
                TextDraw.Anchor.CENTER, TawkitTheme.GOLDEN_BEIGE, 0.14f * vw);

        // Label
        g2.setFont(TawkitTheme.serif(Font.PLAIN, 1.15f * vw));
        TextDraw.drawShadowed(g2, label, cx, (float) (cy + r * 0.5), TextDraw.Anchor.CENTER,
                TawkitTheme.GRAY, 0);
    }

    public void setCountdown(String digits, String label) {
        this.digits = digits == null || digits.isBlank() ? "--:--" : digits;
        this.label = label == null ? "" : label;
    }
}
