package com.tawkit.view.render;

import com.tawkit.view.TawkitTheme;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Left decorative circular emblem: dark disc, concentric silver borders and
 * a mosque silhouette (dome + minarets + base) drawn as vector art.
 */
public final class EmblemElement implements DashboardElement {

    @Override
    public void render(Graphics2D g2, RenderContext ctx) {
        double cx = 11.5 * ctx.vw();
        double cy = 33 * ctx.vh();
        double r = 8 * ctx.vw();

        g2.setColor(new Color(8, 8, 8, 140));
        g2.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));

        // Concentric borders (silver/gray)
        g2.setColor(new Color(200, 200, 200, 70));
        g2.setStroke(new BasicStroke(Math.max(1.2f, (float) (r * 0.012))));
        g2.draw(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
        g2.draw(new Ellipse2D.Double(cx - r * 0.90, cy - r * 0.90, r * 1.8, r * 1.8));
        g2.setColor(new Color(170, 170, 170, 40));
        g2.draw(new Ellipse2D.Double(cx - r * 0.80, cy - r * 0.80, r * 1.6, r * 1.6));

        // Mosque silhouette in silver
        g2.setColor(new Color(190, 190, 190, 90));
        double baseW = r * 0.9;
        double baseH = r * 0.32;
        double baseX = cx - baseW / 2;
        double baseY = cy + r * 0.05;
        g2.fill(new RoundRectangle2D.Double(baseX, baseY, baseW, baseH, baseH * 0.25, baseH * 0.25));

        // Central dome + finial
        double domeW = r * 0.55;
        double domeH = r * 0.48;
        double domeY = baseY - domeH;
        g2.fill(new Arc2D.Double(cx - domeW / 2, domeY, domeW, domeH * 2, 0, 180, Arc2D.CHORD));
        g2.fill(new RoundRectangle2D.Double(cx - r * 0.012, domeY - r * 0.10,
                r * 0.024, r * 0.10, 0, 0));

        // Minarets
        double minaretW = r * 0.055;
        double minaretH = r * 0.62;
        for (double side : new double[]{-1, 1}) {
            double mx = cx + side * r * 0.52 - minaretW / 2;
            double my = baseY + baseH - minaretH;
            g2.fill(new RoundRectangle2D.Double(mx, my, minaretW, minaretH, minaretW / 2, minaretW / 2));
            g2.fill(new Arc2D.Double(mx - minaretW * 0.35, my - minaretW * 0.8,
                    minaretW * 1.7, minaretW * 1.6, 0, 180, Arc2D.CHORD));
        }
    }
}
