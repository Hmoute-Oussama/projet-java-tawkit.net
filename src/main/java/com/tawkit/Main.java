package com.tawkit;

import com.tawkit.controller.AppController;
import com.tawkit.view.MainFrame;
import com.tawkit.view.SettingsDialog;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Entry point: creates the window, wires the controller, shows the kiosk.
 */
public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }

            MainFrame frame = new MainFrame();
            AppController controller = new AppController(frame);

            frame.setSettingsOpener(() -> {
                SettingsDialog dialog = new SettingsDialog(frame);
                dialog.loadFromSettings(controller.getSettings());
                dialog.setVisible(true);
                if (dialog.isSaved()) {
                    dialog.applyToSettings(controller.getSettings());
                    controller.settingsChanged();
                }
            });

            controller.start();
            frame.setVisible(true);
        });
    }
}
