package com.tawkit.view.render;

import com.tawkit.view.TawkitTheme;

import java.awt.Font;
import java.awt.Graphics2D;

/**
 * "اسم المسجد" — the mosque name in Arabic, directly above the clock.
 */
public final class MosqueNameElement implements DashboardElement {

    private String name = "اسم المسجد";

    @Override
    public void render(Graphics2D g2, RenderContext ctx) {
        g2.setFont(TawkitTheme.arabic(2.6f * (float) ctx.vw()));
        TextDraw.drawShadowed(g2, name, ctx.centerX(), (float) (17.0 * ctx.vh()),
                TextDraw.Anchor.CENTER, TawkitTheme.WARM_WHITE, 0.10f * (float) ctx.vw());
    }

    public void setName(String name) {
        this.name = (name == null || name.isBlank()) ? "اسم المسجد" : name;
    }
}
