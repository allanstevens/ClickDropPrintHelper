// ConfigWindow.java
package com.newfangledthings.clickdropprinthelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ConfigWindow extends JFrame {
    private final Properties config;
    private final JPanel configPanel;
    private final Map<String, String> friendlyTextLookup;

    public ConfigWindow(Properties config) {
        this.config = config;
        this.friendlyTextLookup = createFriendlyTextLookup();

        setTitle("Configuration");
        setSize(900, 1000);
        setLayout(new BorderLayout());
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        configPanel = new JPanel();
        configPanel.setLayout(new GridLayout(0, 2));
        loadConfigValues();

        JScrollPane scrollPane = new JScrollPane(configPanel);
        add(scrollPane, BorderLayout.CENTER);

        JButton saveButton = new JButton("Save");
        saveButton.setBorder(new EmptyBorder(10, 10, 10, 10)); // Add padding to save button
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveConfig();
            }
        });
        add(saveButton, BorderLayout.SOUTH);

        setLocationRelativeTo(null); // Center the window on the screen
    }

    private Map<String, String> createFriendlyTextLookup() {
        Map<String, String> lookup = new HashMap<>();

        lookup.put("ViewerExecuteProofOfPostage", "Proof Of Postage executable path (\\path\\pdf\\viewer.exe)");
        lookup.put("ViewerExecutePackingSlip", "Packing Slip executable path (\\path\\pdf\\viewer.exe)");
        lookup.put("ViewerExecuteLabels", "Labels executable path (\\path\\pdf\\viewer.exe)");

        lookup.put("ViewerExecutePrintProofOfPostage","Proof Of Postage printable executable path (\\path\\pdf\\viewer.exe /t)");
        lookup.put("ViewerExecutePrintPackingSlip","Packing Slip printable executable path (\\path\\pdf\\viewer.exe /t)");
        lookup.put("ViewerExecutePrintLabels","Labels printable executable path (\\path\\pdf\\viewer.exe /t)");

        lookup.put("CreatePackingSlips", "Create Packing Slips? (yes/no)");
        lookup.put("CreateProofOfPostage", "Create Proof Of Postage (yes/no)");
        lookup.put("CreateLabels", "Create Labels (yes/no)");
        lookup.put("CreateQRs", "Add QRs to Packing Slips (yes/no)");

        lookup.put("PackingSlipHeaderImage", "Packing Slip Header Image (\\path\\header.png)");
        lookup.put("PackingSlipFooterImage", "Packing Slip Footer Image (\\path\\footer.png)");

        lookup.put("WatchFolder", "Watch Folder (\\folder\\where\\clickdrop\\downloads\\pdfs\\)");
        lookup.put("StoreFolder", "Store Folder (\\folder\\to\\store\\created\\pdfs\\)");

        lookup.put("ViewerDelay", "Viewer Delay (seconds)");
        lookup.put("BeforeRun", "PDF found action (stop, prompt, unset will monitor as normal and run)");


        // Add more mappings as needed
        return lookup;
    }

    private void loadConfigValues() {
        for (String key : config.stringPropertyNames()) {
            String friendlyText = friendlyTextLookup.getOrDefault(key, key);
            JLabel label = new JLabel(friendlyText);
            label.setBorder(new EmptyBorder(10, 10, 10, 10)); // Add padding to label
            JTextField textField = new JTextField(config.getProperty(key));
            textField.setBorder(new EmptyBorder(10, 10, 10, 10)); // Add padding to text field
            configPanel.add(label);
            configPanel.add(textField);
        }
    }

    private void saveConfig() {
        Component[] components = configPanel.getComponents();
        for (int i = 0; i < components.length; i += 2) {
            JLabel label = (JLabel) components[i];
            JTextField textField = (JTextField) components[i + 1];
            String key = getKeyFromFriendlyText(label.getText());
            config.setProperty(key, textField.getText());
        }

        try (FileOutputStream out = new FileOutputStream("config.properties")) {
            config.store(out, null);
            JOptionPane.showMessageDialog(this, "Configuration saved. Please restart the application to apply changes.");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error saving configuration: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String getKeyFromFriendlyText(String friendlyText) {
        for (Map.Entry<String, String> entry : friendlyTextLookup.entrySet()) {
            if (entry.getValue().equals(friendlyText)) {
                return entry.getKey();
            }
        }
        return friendlyText; // Fallback to the original key if no match is found
    }
}