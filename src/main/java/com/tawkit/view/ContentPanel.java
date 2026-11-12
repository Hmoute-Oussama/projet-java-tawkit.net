package com.tawkit.view;

import com.tawkit.model.content.ContentItem;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Rotating content area at the bottom of the screen: cycles through
 * Quran verses, hadith, and adhkar with a fade transition.
 */
public class ContentPanel extends JPanel {

    private final JLabel arabicLabel;
    private final JLabel translationLabel;
    private final JLabel referenceLabel;

    private List<ContentItem> items;
    private int currentIndex = 0;
    private int rotationSeconds = 18;
    private Timer rotationTimer;

    public ContentPanel() {
        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(10, 40, 10, 40));

        arabicLabel = new JLabel("", SwingConstants.CENTER);
        arabicLabel.setFont(new Font("SansSerif", Font.PLAIN, 32));
        arabicLabel.setForeground(new Color(255, 240, 200));
        arabicLabel.setHorizontalAlignment(SwingConstants.CENTER);

        translationLabel = new JLabel("", SwingConstants.CENTER);
        translationLabel.setFont(new Font("SansSerif", Font.PLAIN, 22));
        translationLabel.setForeground(new Color(220, 220, 220));
        translationLabel.setHorizontalAlignment(SwingConstants.CENTER);

        referenceLabel = new JLabel("", SwingConstants.CENTER);
        referenceLabel.setFont(new Font("SansSerif", Font.ITALIC, 18));
        referenceLabel.setForeground(new Color(150, 165, 185));
        referenceLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        arabicLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        translationLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        referenceLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        textPanel.add(arabicLabel);
        textPanel.add(Box.createVerticalStrut(8));
        textPanel.add(translationLabel);
        textPanel.add(Box.createVerticalStrut(4));
        textPanel.add(referenceLabel);

        add(textPanel, BorderLayout.CENTER);
    }

    public void setItems(List<ContentItem> items) {
        this.items = items;
        this.currentIndex = 0;
        showCurrent();
        startRotation();
    }

    public void setRotationSeconds(int seconds) {
        this.rotationSeconds = seconds;
        startRotation();
    }

    private void startRotation() {
        if (rotationTimer != null) {
            rotationTimer.cancel();
        }
        if (items == null || items.size() <= 1) return;

        rotationTimer = new Timer("ContentRotation", true);
        rotationTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> {
                    currentIndex = (currentIndex + 1) % items.size();
                    showCurrent();
                });
            }
        }, rotationSeconds * 1000L, rotationSeconds * 1000L);
    }

    private void showCurrent() {
        if (items == null || items.isEmpty()) {
            arabicLabel.setText("");
            translationLabel.setText("");
            referenceLabel.setText("");
            return;
        }

        ContentItem item = items.get(currentIndex);
        arabicLabel.setText(item.hasArabic() ? item.getArabicText() : "");
        translationLabel.setText(item.getTranslation());
        referenceLabel.setText(item.getReference());
    }
}
