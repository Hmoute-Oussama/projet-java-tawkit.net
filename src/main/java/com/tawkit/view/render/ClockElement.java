package com.tawkit.view.render;

import com.tawkit.service.format.TimeText;
import com.tawkit.view.TawkitTheme;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.time.LocalTime;

/** Main digital clock with seconds and AM/PM hugging the digits. */
public final class ClockElement implements DashboardElement {

    private boolean use24 = false;
    private boolean showSeconds = true;

    @Override
    public void render(Graphics2D g2, RenderContext ctx) {
        LocalTime now = LocalTime.now();
        String digits = TimeText.clockDigits(now, use24);
        String ampm = TimeText.amPm(now, use24);
        String seconds = showSeconds ? TimeText.seconds(now) : "";
        float vw = (float) ctx.vw();

        // Clock digits: centered horizontally, top ~22% — ~8vw
        g2.setFont(TawkitTheme.serif(Font.BOLD, 8.0f * vw));
        FontMetrics fm = g2.getFontMetrics();
        float centerX = (float) ctx.centerX();
        float baselineY = (float) (22.0 * ctx.vh()) + fm.getAscent() * 0.9f;
        float digitsWidth = fm.stringWidth(digits);
        TextDraw.drawShadowed(g2, digits, centerX, baselineY,
                TextDraw.Anchor.CENTER, TawkitTheme.WARM_WHITE, 0.28f * vw);

        // Seconds immediately right of the digits
        if (!seconds.isEmpty()) {
            g2.setFont(TawkitTheme.serif(Font.BOLD, 3.0f * vw));
            FontMetrics sm = g2.getFontMetrics();
            float secX = centerX + digitsWidth / 2 + sm.stringWidth("00") / 2 + 1.1f * vw;
            TextDraw.drawShadowed(g2, seconds, secX, baselineY - sm.getAscent() * 0.25f,
                    TextDraw.Anchor.CENTER, TawkitTheme.WARM_WHITE, 0.12f * vw);
        }

        // AM / PM below the seconds
        if (!ampm.isEmpty()) {
            g2.setFont(TawkitTheme.serif(Font.PLAIN, 2.4f * vw));
            FontMetrics am = g2.getFontMetrics();
            float secX = centerX + digitsWidth / 2 + 1.1f * vw + am.stringWidth("00") / 2;
            TextDraw.drawShadowed(g2, ampm, secX, baselineY + 3.0f * (float) ctx.vh(),
                    TextDraw.Anchor.CENTER, TawkitTheme.GOLDEN_BEIGE, 0.1f * vw);
        }
    }

    public void setClockMode(boolean use24, boolean showSeconds) {
        this.use24 = use24;
        this.showSeconds = showSeconds;
    }
}
