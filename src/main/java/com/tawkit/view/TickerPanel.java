package com.tawkit.view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Bottom scrolling announcements ticker, like the Tawkit news bar.
 * Clicking it toggles between the ticker and the hadith view (as Tawkit does).
 */
public class TickerPanel extends JPanel {

    private static final Color BG = new Color(15, 25, 45);
    private static final Color TEXT = new Color(235, 225, 180);

    private final List<String> messages = new ArrayList<>();
    private int offset = Integer.MAX_VALUE / 2;
    private final Timer timer;
    private boolean running = true;

    public TickerPanel() {
        setBackground(BG);
        setFont(new Font("SansSerif", Font.PLAIN, 20));
        setPreferredSize(new Dimension(0, 44));

        timer = new Timer(30, e -> {
            offset -= 2;
            if (offset < -textWidth() - 100) offset = getWidth() + 100;
            repaint();
        });
        timer.start();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                setRunning(!running);
            }
        });
    }

    public void setMessages(List<String> msgs) {
        messages.clear();
        if (msgs != null) messages.addAll(msgs);
        offset = getWidth() + 100;
        repaint();
    }

    public void setRunning(boolean run) {
        this.running = run;
        if (run) timer.start();
        else timer.stop();
        repaint();
    }

    public boolean isRunning() {
        return running;
    }

    private String text() {
        if (messages.isEmpty()) return "";
        return String.join("    •    ", messages);
    }

    private int textWidth() {
        FontMetrics fm = getFontMetrics(getFont());
        return fm.stringWidth(text());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        String s = text();
        if (s.isEmpty()) return;
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(getFont());
        g2.setColor(TEXT);
        if (offset > getWidth()) offset = getWidth() + 100;
        g2.drawString(s, offset, getHeight() / 2 + g2.getFontMetrics().getAscent() / 2 - 2);
    }
}
