package com.newfangledthings.clickdropprinthelper;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class Config {
    private final Properties properties;
    private final File configFile;

    public Config(String configFilePath) {
        properties = new Properties();
        configFile = new File(configFilePath);
        loadProperties();
    }

    /**
     * Load the properties file
     */
    private void loadProperties() {
        // If this is the first time running the create the config file and end early with a message to ask to update config
        if (!configFile.exists()) {
            createDefaultConfig();
            System.out.println("Please update the " + configFile.getName() + " file with your settings");
            System.exit(0);
        }
        try (FileInputStream input = new FileInputStream(configFile)) {
            properties.load(input);
        } catch (IOException e) {
            System.err.println("Error loading properties: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    /**
     * Create a default config file
     */
    private void createDefaultConfig() {
        try (FileOutputStream output = new FileOutputStream(configFile)) {

            properties.setProperty("ViewerExecutePackingSlip", "C:\\Program Files\\Adobe\\Acrobat DC\\Acrobat\\Acrobat.exe  /t \"%filename%\" \"Paper Printer\"");
            properties.setProperty("ViewerExecuteLabels","C:\\Program Files\\Adobe\\Acrobat DC\\Acrobat\\Acrobat.exe  /t \"%filename%\" \"Label Printer\"");
            properties.setProperty("ViewerExecuteProofOfPostage", "C:\\Program Files\\Adobe\\Acrobat DC\\Acrobat\\Acrobat.exe  /t \"%filename%\" \"Paper Printer\"");

            properties.setProperty("CreatePackingSlips", "yes");
            properties.setProperty("CreateLabels", "yes");
            properties.setProperty("CreateProofOfPostage", "yes");
            properties.setProperty("CreateQRs", "yes");
            properties.setProperty("PackingSlipHeaderImage", "Packing Slip Header.png");
            properties.setProperty("PackingSlipFooterImage", "Packing Slip Footer.png");
            properties.setProperty("WatchFolder", FileSystemView.getFileSystemView().getDefaultDirectory().getAbsolutePath());
            properties.setProperty("ViewerDelay", "2");
            properties.setProperty("StoreFolder", FileSystemView.getFileSystemView().getDefaultDirectory().getAbsolutePath()); //System.getProperty("user.dir"));
            properties.setProperty("StopWatchAfterFirstRun","no");

            properties.store(output, "Default Configuration Settings");
            System.out.println("Default settings saved to config.properties");
        } catch (IOException e) {
            System.err.println("Error creating default config: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    // Get a property from the config file
    // Will cause and exception and return and error message if property does not exist
    public String getProperty(String key) {
        if (!properties.containsKey(key)) {
            var error = "Property '" + key + "' not found in " + configFile.getName() + " file";
            System.err.println(error);
            throw new IllegalArgumentException(error);
        }
        return properties.getProperty(key);
    }

}