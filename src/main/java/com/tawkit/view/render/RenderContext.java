package com.tawkit.view.render;

/**
 * Immutable geometry context passed to every dashboard element during
 * painting. Coordinates follow the CSS-like convention of the reference
 * design: {@code vw()} is 1% of the panel width, {@code vh()} is 1% of its
 * height, so every element can express positions in the same units used
 * by the design spec (e.g. "clock at 50vw, 22vh").
 */
public final class RenderContext {

    private final double width;
    private final double height;

    public RenderContext(double width, double height) {
        this.width = width;
        this.height = height;
    }

    public double width() {
        return width;
    }

    public double height() {
        return height;
    }

    /** 1% of the panel width (CSS "vw"). */
    public double vw() {
        return width / 100.0;
    }

    /** 1% of the panel height (CSS "vh"). */
    public double vh() {
        return height / 100.0;
    }

    /** Horizontal center of the panel. */
    public double centerX() {
        return width / 2.0;
    }
}
