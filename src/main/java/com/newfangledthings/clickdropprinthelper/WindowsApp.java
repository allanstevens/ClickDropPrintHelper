// WindowsApp.java
package com.newfangledthings.clickdropprinthelper;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

public class WindowsApp {
    public static void main(String[] args) throws IOException, InterruptedException {
        // Initialize the console window first
        ConsoleWindow consoleWindow = new ConsoleWindow();
        //consoleWindow.setVisible(true);

        Config config;
        boolean consoleMode = false;
        config = new Config("config.properties");

        if (!SystemTray.isSupported()) {
            System.err.println("System tray not supported!");
            return;
        }

        SystemTray tray = SystemTray.getSystemTray();
        URL imageUrl = WindowsApp.class.getResource("/icon.png");
        if (imageUrl == null) {
            System.err.println("Icon image not found!");
            return;
        }
        Image image = Toolkit.getDefaultToolkit().getImage(imageUrl);
        TrayIcon trayIcon = new TrayIcon(image, "Click & Drop Print Helper");
        trayIcon.setImageAutoSize(true);

        PopupMenu popup = new PopupMenu();
        MenuItem consoleItem = new MenuItem("Console");
        consoleItem.addActionListener(e -> consoleWindow.setVisible(true));
        popup.add(consoleItem);

        MenuItem configItem = new MenuItem("Configuration");
        configItem.addActionListener(e -> {
            Properties properties = config.getProperties();
            ConfigWindow configWindow = new ConfigWindow(properties);
            configWindow.setVisible(true);
        });
        popup.add(configItem);

        MenuItem exitItem = new MenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));
        popup.add(exitItem);

        trayIcon.setPopupMenu(popup);

        // Add left-click listener to open console window
        trayIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) { // Left-click
                    consoleWindow.setVisible(true);
                }
            }
        });

        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            System.err.println("Error adding tray icon: " + e.getMessage());
        }

        FileWatcher fileWatcher = new FileWatcher(config, consoleMode);
        fileWatcher.watch();
    }
}