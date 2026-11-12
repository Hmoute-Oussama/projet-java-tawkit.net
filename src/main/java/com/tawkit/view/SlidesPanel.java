package com.tawkit.view;

import com.tawkit.model.content.Slide;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

/**
 * Full-area slideshow panel showing user slides (text / picture / URL)
 * between prayers, like Tawkit. Shown/hidden by the controller.
 */
public class SlidesPanel extends JPanel {

    private final JLabel textLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel imageLabel = new JLabel("", SwingConstants.CENTER);
    private final CardLayout cards = new CardLayout();
    private final JPanel cardHolder = new JPanel(cards);
    private final File slidesFolder;

    private List<Slide> slides;
    private int index = 0;
    private final Timer rotationTimer;

    public SlidesPanel(File slidesFolder) {
        this.slidesFolder = slidesFolder;
        setLayout(new BorderLayout());
        setOpaque(false);

        textLabel.setFont(new Font("SansSerif", Font.BOLD, 44));
        textLabel.setForeground(new Color(255, 245, 200));
        textLabel.setHorizontalAlignment(SwingConstants.CENTER);

        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);

        cardHolder.setOpaque(false);
        cardHolder.add(textLabel, "text");
        cardHolder.add(imageLabel, "image");
        add(cardHolder, BorderLayout.CENTER);

        rotationTimer = new Timer(15_000, e -> showNext());
        rotationTimer.setRepeats(true);
    }

    public void setSlides(List<Slide> slides) {
        this.slides = slides;
        this.index = 0;
        if (slides != null && !slides.isEmpty()) {
            showCurrent();
            rotationTimer.restart();
        } else {
            rotationTimer.stop();
            textLabel.setText("");
            imageLabel.setIcon(null);
        }
    }

    public void showNext() {
        if (slides == null || slides.isEmpty()) return;
        index = (index + 1) % slides.size();
        showCurrent();
    }

    public void setActive(boolean active) {
        if (active && slides != null && !slides.isEmpty()) {
            rotationTimer.start();
        } else {
            rotationTimer.stop();
        }
    }

    private void showCurrent() {
        if (slides == null || slides.isEmpty()) return;
        Slide slide = slides.get(index);
        switch (slide.getKind()) {
            case TEXT -> {
                textLabel.setText(slide.toDisplayText());
                cards.show(cardHolder, "text");
            }
            case IMAGE -> {
                File file = slide.resolveImage(slidesFolder);
                if (file != null) {
                    imageLabel.setIcon(loadScaled(file));
                    cards.show(cardHolder, "image");
                } else {
                    textLabel.setText(slide.getContent());
                    cards.show(cardHolder, "text");
                }
            }
            case URL -> {
                textLabel.setText(slide.getContent());
                cards.show(cardHolder, "text");
            }
        }
    }

    private Icon loadScaled(File file) {
        try {
            BufferedImage img = ImageIO.read(file);
            if (img == null) return null;
            Rectangle bounds = getBounds();
            if (bounds.width <= 0) bounds = new Rectangle(1280, 720);
            double scale = Math.min(1.0, Math.min(
                    bounds.getWidth() / img.getWidth(),
                    bounds.getHeight() / img.getHeight()));
            int w = (int) (img.getWidth() * scale);
            int h = (int) (img.getHeight() * scale);
            Image scaled = img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } catch (Exception e) {
            return null;
        }
    }
}
