package com.tawkit.service.audio;

import com.tawkit.infrastructure.audio.ToneGenerator;
import com.tawkit.model.prayer.PrayerType;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Plays adhan, iqama and alert sounds.
 *
 * <p>WAV files bundled in <code>/assets/audio/...</code> are played when
 * present; otherwise a synthesized chime is used so the app always makes
 * sound. Supported bundled files (16-bit PCM WAV, mono or stereo):
 * <pre>
 *   /assets/audio/adhan/adhan.wav        full adhan
 *   /assets/audio/adhan/adhan_fajr.wav   Fajr-specific adhan (optional)
 *   /assets/audio/iqama/short_iqama.wav  short iqama
 *   /assets/audio/alerts/alert.wav       generic alert
 * </pre>
 * Playback runs on a background thread; sounds never block the UI.
 */
public class AudioService {

    public enum Sound { ADHAN, ADHAN_FAJR, SHORT_ADHAN, IQAMA, ALERT }

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "tawkit-audio");
        t.setDaemon(true);
        return t;
    });

    private volatile double volume = 1.0;
    private volatile boolean enabled = true;
    private volatile Clip currentClip;

    public void setVolume(double volume) {
        this.volume = Math.max(0.0, Math.min(1.0, volume));
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void play(Sound sound, PrayerType forPrayer) {
        if (!enabled) return;
        executor.submit(() -> {
            try {
                if (!playBundled(sound, forPrayer)) {
                    playFallback(sound);
                }
            } catch (Exception e) {
                System.err.println("[Audio] playback failed: " + e.getMessage());
            }
        });
    }

    public void playAlert() {
        play(Sound.ALERT, null);
    }

    /** Stops whatever is currently playing (e.g. screen click). */
    public void stop() {
        Clip clip = currentClip;
        if (clip != null && clip.isRunning()) {
            clip.stop();
            clip.close();
        }
    }

    // ------------------------------------------------------------ internals

    private boolean playBundled(Sound sound, PrayerType forPrayer) {
        String path = resourcePath(sound, forPrayer);
        URL url = getClass().getResource(path);
        if (url == null) return false;
        try {
            return playClip(url);
        } catch (Exception e) {
            return false;
        }
    }

    private static String resourcePath(Sound sound, PrayerType forPrayer) {
        return switch (sound) {
            case ADHAN -> "/assets/audio/adhan/adhan.wav";
            case ADHAN_FAJR -> forPrayer == PrayerType.FAJR
                    ? "/assets/audio/adhan/adhan_fajr.wav"
                    : "/assets/audio/adhan/adhan.wav";
            case SHORT_ADHAN -> "/assets/audio/adhan/short_adhan.wav";
            case IQAMA -> "/assets/audio/iqama/short_iqama.wav";
            case ALERT -> "/assets/audio/alerts/alert.wav";
        };
    }

    private boolean playClip(URL url) throws IOException,
            UnsupportedAudioFileException, LineUnavailableException {
        try (InputStream raw = url.openStream();
             AudioInputStream ais = AudioSystem.getAudioInputStream(new BufferedInputStream(raw))) {

            Clip clip = AudioSystem.getClip();
            clip.open(ais);
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    clip.close();
                }
            });
            currentClip = clip;
            clip.start();
            return true;
        }
    }

    private void playFallback(Sound sound) throws LineUnavailableException {
        switch (sound) {
            case ADHAN, ADHAN_FAJR -> ToneGenerator.playSequence(ToneGenerator.adhanChime(), volume);
            case IQAMA -> ToneGenerator.playSequence(ToneGenerator.iqamaChime(), volume);
            default -> ToneGenerator.playSequence(ToneGenerator.alertBeeps(), volume);
        }
    }
}
