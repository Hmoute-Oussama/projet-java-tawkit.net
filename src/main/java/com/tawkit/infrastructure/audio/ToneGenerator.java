package com.tawkit.infrastructure.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/**
 * Generates simple synthesized sounds in memory (sine-wave chimes).
 * Used as a fallback when no bundled adhan/iqama audio file exists,
 * and for alerts like the pre-Fajr beep.
 */
public final class ToneGenerator {

    private static final float SAMPLE_RATE = 44100f;

    private ToneGenerator() {
    }

    /**
     * Plays a sequence of notes. Each note is {frequencyHz, durationMs}.
     * Blocking — call on a worker thread.
     */
    public static void playSequence(double[][] notes, double volume) throws LineUnavailableException {
        AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
        try (SourceDataLine line = AudioSystem.getSourceDataLine(format)) {
            line.open(format, 8192);
            line.start();
            for (double[] note : notes) {
                playTone(line, note[0], (int) note[1], volume);
            }
            line.drain();
        }
    }

    private static void playTone(SourceDataLine line, double freqHz, int durationMs, double volume) {
        int frames = (int) (SAMPLE_RATE * durationMs / 1000.0);
        byte[] data = new byte[frames * 2];
        double fade = Math.min(0.03, durationMs / 1000.0 / 4.0); // 30ms fade in/out
        for (int i = 0; i < frames; i++) {
            double t = i / SAMPLE_RATE;
            double envelope = 1.0;
            double remaining = (frames - i) / SAMPLE_RATE;
            if (t < fade) envelope = t / fade;
            if (remaining < fade) envelope = remaining / fade;
            double value = Math.sin(2 * Math.PI * freqHz * t)
                    * 0.35 * volume * envelope
                    * 32767.0;
            int sample = (int) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, value));
            data[i * 2] = (byte) (sample & 0xFF);
            data[i * 2 + 1] = (byte) ((sample >> 8) & 0xFF);
        }
        line.write(data, 0, data.length);
    }

    /** Classic three-beep alert. */
    public static double[][] alertBeeps() {
        return new double[][]{
                {880, 180}, {0, 120}, {880, 180}, {0, 120}, {880, 300}
        };
    }

    /** Short pleasant chime used as adhan fallback. */
    public static double[][] adhanChime() {
        return new double[][]{
                {523.25, 350}, {659.25, 350}, {783.99, 500}, {659.25, 350}, {523.25, 700}
        };
    }

    /** Two-note chime for iqama fallback. */
    public static double[][] iqamaChime() {
        return new double[][]{
                {659.25, 300}, {523.25, 500}
        };
    }
}
