package com.tawkit.view.render;

import com.tawkit.view.TawkitTheme;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Full-bleed background photograph (bundled or user-provided) plus the dark
 * cinematic overlay for the active theme.
 */
public final class BackgroundElement implements DashboardElement {

    private static final String BUNDLED_RESOURCE = "/assets/backgrounds/dome-of-the-rock.jpg";

    private String theme = "night";
    private BufferedImage customImage;
    private BufferedImage bundledImage;

    @Override
    public void render(Graphics2D g2, RenderContext ctx) {
        g2.setColor(TawkitTheme.BLACK);
        g2.fillRect(0, 0, (int) ctx.width(), (int) ctx.height());

        BufferedImage bg = customImage != null ? customImage : bundled();
        if (bg != null) {
            double scale = Math.max(ctx.width() / bg.getWidth(), ctx.height() / bg.getHeight());
            int bw = (int) Math.round(bg.getWidth() * scale);
            int bh = (int) Math.round(bg.getHeight() * scale);
            g2.drawImage(bg, (int) ((ctx.width() - bw) / 2), (int) ((ctx.height() - bh) / 2), bw, bh, null);
        }
        g2.setColor(overlayFor(theme));
        g2.fillRect(0, 0, (int) ctx.width(), (int) ctx.height());
    }

    public void setTheme(String theme) {
        this.theme = theme == null ? "night" : theme;
        this.customImage = null;
    }

    public void setCustomImage(String path) {
        if (path == null || path.isBlank()) return;
        try {
            customImage = ImageIO.read(new File(path));
        } catch (Exception e) {
            System.err.println("[Background] cannot load " + path);
        }
    }

    private BufferedImage bundled() {
        if (bundledImage == null) {
            try (var in = getClass().getResourceAsStream(BUNDLED_RESOURCE)) {
                if (in != null) bundledImage = ImageIO.read(in);
            } catch (Exception ignored) {
            }
        }
        return bundledImage;
    }

    private Color overlayFor(String theme) {
        return switch (theme) {
            case "green" -> new Color(0, 20, 8, 175);
            case "blue" -> new Color(0, 10, 30, 175);
            default -> new Color(0, 0, 0, 185); // dark cinematic overlay (reference-matched)
        };
    }
}
