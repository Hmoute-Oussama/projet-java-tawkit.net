package com.tawkit.view.render;

import com.tawkit.model.content.ContentItem;
import com.tawkit.view.TawkitTheme;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Central Arabic calligraphy (Qur'anic verse) with ornamental brackets and
 * the subtle "Surat ..." label. Always renders the Arabic text, regardless
 * of the interface language, per the reference design.
 */
public final class CalligraphyElement implements DashboardElement {

    private final List<String> verseLines = new ArrayList<>();
    private final List<String> verseLabels = new ArrayList<>();
    private int contentIndex = 0;

    @Override
    public void render(Graphics2D g2, RenderContext ctx) {
        if (verseLines.isEmpty()) return;
        int idx = contentIndex % verseLines.size();
        String text = verseLines.get(idx);
        String label = verseLabels.isEmpty() ? "" : verseLabels.get(idx % verseLabels.size());
        float vw = (float) ctx.vw();

        boolean arabicScript = text.codePoints().anyMatch(cp -> cp >= 0x0600 && cp <= 0x06FF);
        float size = arabicScript ? 3.5f * vw : 2.4f * vw;
        // Amiri Regular (not Bold) — Bold collides the vowel marks at this size
        g2.setFont(arabicScript ? TawkitTheme.arabic(size) : TawkitTheme.serif(Font.PLAIN, size));

        // Wrap into at most 2 lines within 92% width
        List<String> lines = TextDraw.wrap(g2, text, 92f * vw);
        float lineHeight = size * 1.55f;
        float startY = (float) (47 * ctx.vh());

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (arabicScript) {
                line = "﴿" + line + "﴾";
            }
            TextDraw.drawShadowed(g2, line, ctx.centerX(), startY + i * lineHeight,
                    TextDraw.Anchor.CENTER, TawkitTheme.COPPER_GOLD, 0.08f * vw);
        }

        // Small "Surat X" label — Latin prefix in serif, Arabic sura name in Amiri
        if (!label.isEmpty()) {
            float labelY = startY - 2.2f * (float) ctx.vh();
            Color faintGray = TextDraw.withAlpha(TawkitTheme.GRAY, 0.33f);
            g2.setFont(TawkitTheme.serif(Font.PLAIN, 1.3f * vw));
            String prefix = "Surat ";
            float prefixW = g2.getFontMetrics().stringWidth(prefix);
            TextDraw.draw(g2, prefix, 16 * ctx.vw(), labelY, TextDraw.Anchor.LEFT, faintGray);
            g2.setFont(TawkitTheme.arabic(1.4f * vw));
            TextDraw.draw(g2, label, 16 * ctx.vw() + prefixW, labelY, TextDraw.Anchor.LEFT, faintGray);
        }
    }

    public void setVerses(List<ContentItem> items) {
        verseLines.clear();
        verseLabels.clear();
        if (items != null) {
            for (ContentItem item : items) {
                String text = item.getArabicText(); // Arabic-only calligraphy
                if (text == null || text.isBlank()) continue;
                verseLines.add(text);
                String ref = item.getReference() == null ? "" : item.getReference();
                verseLabels.add(ref.startsWith("Quran") ? ref.substring(5).trim() : ref);
            }
        }
        contentIndex = 0;
    }

    /** Rotates to the next verse; returns true when there was more than one. */
    public boolean advance() {
        if (verseLines.size() <= 1) return false;
        contentIndex = (contentIndex + 1) % verseLines.size();
        return true;
    }
}
