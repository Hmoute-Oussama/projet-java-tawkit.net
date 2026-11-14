package com.tawkit.view.render;

import java.awt.Graphics2D;

/**
 * Strategy interface for one visual element of the dashboard (background,
 * header, clock, schedule...). Each element owns its own state and paints
 * itself; the screen composes them in a fixed order (Composite pattern).
 */
public interface DashboardElement {

    /** Renders this element onto the panel. */
    void render(Graphics2D g2, RenderContext ctx);
}
