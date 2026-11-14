package com.tawkit.view.render;

import com.tawkit.view.TawkitTheme;

import java.awt.Font;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Bottom Arabic footer (dhikr) with rotation over user announcements, plus
 * the extremely subtle corner labels of the reference.
 */
public final class FooterElement implements DashboardElement {

    private static final String DEFAULT_DHIKR = "لا حول ولا قوة إلا بالله";

    private final List<String> texts = new ArrayList<>(List.of(DEFAULT_DHIKR));
    private int index = 0;

    @Override
    public void render(Graphics2D g2, RenderContext ctx) {
        float vw = (float) ctx.vw();

        g2.setFont(TawkitTheme.arabic(2.6f * vw));
        TextDraw.drawShadowed(g2, current(), ctx.centerX(), (float) (94.0 * ctx.vh()),
                TextDraw.Anchor.CENTER, TawkitTheme.WARM_WHITE, 0.18f * vw);

        g2.setFont(TawkitTheme.serif(Font.PLAIN, 0.95f * vw));
        TextDraw.drawShadowed(g2, "TAWKIT DESKTOP", 1.2 * ctx.vw(), (float) (98.6 * ctx.vh()),
                TextDraw.Anchor.LEFT, TextDraw.withAlpha(TawkitTheme.GRAY, 0.55f), 0);
        TextDraw.drawShadowed(g2, "JAVA SWING EDITION", ctx.width() - 1.2 * ctx.vw(),
                (float) (98.6 * ctx.vh()), TextDraw.Anchor.RIGHT,
                TextDraw.withAlpha(TawkitTheme.GRAY, 0.55f), 0);
    }

    private String current() {
        return texts.get(index % texts.size());
    }

    /** Replaces the rotation texts, keeping the dhikr line first. */
    public void setTexts(List<String> userTexts) {
        texts.clear();
        texts.add(DEFAULT_DHIKR);
        if (userTexts != null) {
            texts.addAll(userTexts);
        }
        index = 0;
    }

    /** Rotates to the next footer line; returns true when there was more than one. */
    public boolean advance() {
        if (texts.size() <= 1) return false;
        index = (index + 1) % texts.size();
        return true;
    }
}
