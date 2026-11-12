package com.tawkit.view;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Central visual identity of the Tawkit dashboard — palette and typography.
 *
 * Palette (fixed, from the reference design):
 *   black          #050505
 *   dark charcoal  #101010
 *   warm white     #F5F2E8
 *   golden beige   #C5A66A
 *   copper gold    #B77B43
 *   secondary gray #999999
 *
 * Fonts: Amiri (bundled TTF) for Arabic calligraphy; a serif family
 * (Georgia when available) for English text and the digital clock.
 */
public final class TawkitTheme {

    public static final Color BLACK          = new Color(0x05, 0x05, 0x05);
    public static final Color CHARCOAL       = new Color(0x10, 0x10, 0x10);
    public static final Color WARM_WHITE     = new Color(0xF5, 0xF2, 0xE8);
    public static final Color GOLDEN_BEIGE   = new Color(0xC5, 0xA6, 0x6A);
    public static final Color COPPER_GOLD    = new Color(0xB7, 0x7B, 0x43);
    public static final Color GRAY           = new Color(0x99, 0x99, 0x99);
    public static final Color SUN_YELLOW     = new Color(0xE8, 0xC4, 0x4A);
    public static final Color MENU_GRAY      = new Color(0xAC, 0xA0, 0x91);

    private static Font amiriRegular;
    private static Font amiriBold;
    private static String serifFamily = "Serif";

    static {
        amiriRegular = loadFont("/assets/fonts/Amiri-Regular.ttf");
        amiriBold    = loadFont("/assets/fonts/Amiri-Bold.ttf");
        detectSerifFamily();
    }

    private TawkitTheme() {
    }

    private static Font loadFont(String resource) {
        try (InputStream in = TawkitTheme.class.getResourceAsStream(resource)) {
            if (in != null) {
                return Font.createFont(Font.TRUETYPE_FONT, in);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static void detectSerifFamily() {
        Set<String> families = new HashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String candidate : new String[]{"Georgia", "Times New Roman", "FreeSerif"}) {
            if (families.contains(candidate)) {
                serifFamily = candidate;
                return;
            }
        }
    }

    /** Serif font for English text (size in px). */
    public static Font serif(int style, float sizePx) {
        return new Font(serifFamily, style, Math.max(1, Math.round(sizePx)));
    }

    /** Amiri for Arabic calligraphy (size in px); falls back to a serif. */
    public static Font arabic(float sizePx) {
        if (amiriRegular != null) return amiriRegular.deriveFont(Font.PLAIN, sizePx);
        return new Font("Serif", Font.PLAIN, Math.max(1, Math.round(sizePx)));
    }

    public static Font arabicBold(float sizePx) {
        if (amiriBold != null) return amiriBold.deriveFont(Font.BOLD, sizePx);
        if (amiriRegular != null) return amiriRegular.deriveFont(Font.BOLD, sizePx);
        return new Font("Serif", Font.BOLD, Math.max(1, Math.round(sizePx)));
    }
}
