package com.tawkit.view.render;

import com.tawkit.model.common.HijriMonthNames;
import com.tawkit.view.TawkitTheme;

import java.awt.Font;
import java.awt.Graphics2D;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

/** Top-center date header, e.g. "Monday 17 Dhul'Qidah 1447 . 04/05/2026". */
public final class DateHeaderElement implements DashboardElement {

    private String dateText = "";

    @Override
    public void render(Graphics2D g2, RenderContext ctx) {
        g2.setFont(TawkitTheme.serif(Font.PLAIN, (float) (1.9 * ctx.vw())));
        TextDraw.drawShadowed(g2, dateText, ctx.centerX(), (float) (5.0 * ctx.vh()),
                TextDraw.Anchor.CENTER, TawkitTheme.WARM_WHITE, 0.12f * (float) ctx.vw());
    }

    public void setDate(LocalDate gregorian, int hijriDay, int hijriMonth, int hijriYear) {
        String weekday = gregorian.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        String month = HijriMonthNames.english(hijriMonth);
        this.dateText = weekday + " " + hijriDay + " " + month + " " + hijriYear
                + " . " + String.format("%02d/%02d/%04d",
                gregorian.getDayOfMonth(), gregorian.getMonthValue(), gregorian.getYear());
    }
}
