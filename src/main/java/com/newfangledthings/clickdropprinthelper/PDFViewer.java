// src/main/java/com/newfangledthings/clickdropprinthelper/PDFViewer.java
package com.newfangledthings.clickdropprinthelper;

import java.io.IOException;

public class PDFViewer {
    private final Config config;

    public PDFViewer(Config config) {
        this.config = config;
    }

    /**
     * Open the PDF file with the viewer specified in the config
     * file for the viewerExecute key and replace %filename% with
     * the filename if it exists in the viewerExecute value
     *
     * @param filename      is the file to open
     * @param viewerExecute is the viewer to use
     */
    public void openPDF(String filename, String viewerExecute) {
        String viewer = config.getProperty(viewerExecute);
        // Will not open if left blank
        if (viewer.isEmpty()){
            System.out.println("Will not open pdf as the viewer not set for " + viewerExecute);
            return;
        }
        // If the viewer contains %filename% then replace it with the filename
        if (viewer.contains("%filename%")) {
            viewer = viewer.replace("%filename%", "\"" + filename + "\"");
        } else {
            viewer = viewer + " \"" + filename + "\"";
        }
        // Start a new thread to open the viewer
        final String finalViewer = viewer;
        new Thread(() -> {
            try {
                Thread.sleep(config.getProperty("ViewerDelay") != null ? Integer.parseInt(config.getProperty("ViewerDelay")) * 1000L : 0);
                System.out.println("Opening " + filename + " with " + finalViewer);
                Runtime.getRuntime().exec(finalViewer);
            } catch (IOException | InterruptedException e) {
                System.err.println("Error opening PDF: " + e.getMessage());
                e.printStackTrace(System.err);
            }
        }).start();
    }
}