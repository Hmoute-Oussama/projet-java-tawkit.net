package com.tawkit.model.prayer;

/**
 * Asr calculation method based on shadow length.
 * <ul>
 *   <li>STANDARD (Shafi'i, Maliki, Hanbali) — shadow equals the object's length (factor 1)</li>
 *   <li>HANAFI — shadow equals twice the object's length (factor 2)</li>
 * </ul>
 */
public enum AsrMethod {
    STANDARD(1),
    HANAFI(2);

    private final int shadowFactor;

    AsrMethod(int shadowFactor) {
        this.shadowFactor = shadowFactor;
    }

    public int getShadowFactor() {
        return shadowFactor;
    }
}
