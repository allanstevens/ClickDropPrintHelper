package com.newfangledthings.clickdropprinthelper;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;

import static java.nio.file.StandardWatchEventKinds.*;

public class Main {

    // Config class to load the properties file
    private final Config config;
    private final ProofOfPostageCreator proofOfPostageCreator;
    private final PDFViewer pdfViewer;

    /**
     * Default constructor to load the config.properties file
     */
    public Main() {
        this("config.properties");
    }
    /**
     * Constructor to load the config.properties file
     *
     * @param configFilename is the file that will be loaded
     */
    public Main(String configFilename)  {
        // Initialize config class, will create a new config.properties file if it doesn't exist
        if (!configFilename.endsWith(".properties")) {
            configFilename += ".properties";
        }
        // Load the config file
        this.config = new Config("config.properties");
        this.proofOfPostageCreator = new ProofOfPostageCreator(config);
        this.pdfViewer = new PDFViewer(config);
    }

    /**
     * Main static method to execute the application with the config file
     * as an argument or without any arguments to use the default config
     * file (config.properties)
     *
     * @param args is the config file
     * @throws IOException          if the file is not found
     * @throws InterruptedException if the thread is interrupted
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        Main mainApp;
        // If args set then use this as the config file
        if (args.length == 1) {
            mainApp = new Main(args[0]);
        } else {
            mainApp = new Main();
        }
        // Run the application
        mainApp.run();
    }

    /**
     * Run the application to monitor the watch folder for pdfs
     * and process them as required by the config.properties file
     *
     * @throws IOException          if the file is not found
     * @throws InterruptedException if the thread is interrupted
     */
    private void run() throws IOException, InterruptedException {

        // Get settings from config.properties
        String watchFolder = config.getProperty("WatchFolder");
        String workingFolder = config.getProperty("WorkingFolder");
        boolean createProofOfPostage = config.getProperty("CreateProofOfPostage").equals("yes");
        boolean createLabels = config.getProperty("CreateLabels").equals("yes");
        boolean createPackingSlips = config.getProperty("CreatePackingSlips").equals("yes");
        boolean createQRs = config.getProperty("CreateQRs").equals("yes");
        boolean stopWatchingAfterFirstRun = config.getProperty("StopWatchAfterFirstRun").equals("yes");

        // Display all the var from above
        System.out.println("Properties loaded from config.properties");

        // Create watch service to monitor download folder
        WatchService watchService = FileSystems.getDefault().newWatchService();
        Path path = Paths.get(watchFolder);
        path.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
        System.out.println("Monitoring download folder [" + watchFolder + "] for pdfs");

        // Poll for events (look for pdf files in watch folder)
        boolean poll = true;
        WatchKey key = watchService.take();
        while (poll) {
            for (WatchEvent<?> event : key.pollEvents()) {
                // Quick pause to stop file locking
                TimeUnit.SECONDS.sleep(1);
                // If a pdf is created in the watch folder
                if (event.kind() == ENTRY_CREATE && event.context().toString().endsWith(".pdf")) {
                    System.out.println("Processing event kind : " + event.kind() + " - File : " + event.context());
                    // Get the filename from the event context
                    String filename = event.context().toString();
                    // PDF found, will now see if it's a Royal Mail shipping label
                    for (ProofOfPostage proofOfPostage : proofOfPostageCreator.createProofOfPostage(watchFolder + "\\" + filename)) {
                        //Now the proof of postage has been created with the ProofOfPostageCreator class we can add some extra helper functions

                        //It's a Royal Mail shipping label, will now create the QR codes
                        if (createQRs) {
                            proofOfPostageCreator.addQRCodesToProofOfPostage(proofOfPostage);
                        }

                        // If you have asked for no proof of postage then delete the first page or delete the file is qr codes don't exist
                        if (!createProofOfPostage) {
                            //If no qr codes then just delete the proof of postage
                            if(!createQRs){
                                System.out.println("Deleting pdf " + filename);
                                Files.delete(Paths.get(watchFolder + "\\" + event.context()));
                            }
                            else {
                                proofOfPostageCreator.removeFirstPage(proofOfPostage);
                            }
                        }

                        // If createPackingSlips is true copy original pdf to working folder then remove every 5th page (which is the label)
                        if (createPackingSlips) {
                            var packingFilename = FilenameGenerator.generateTimestampedFilename(filename,"packing");
                            proofOfPostageCreator.createPackingSlips(proofOfPostage,packingFilename);
                            pdfViewer.openPDF(workingFolder + "\\" + packingFilename, "ViewerExecutePackingSlip");
                        }

                        // If createLabels is true then open the pdf with the labels
                        if (createLabels) {
                            var labelsFilename = FilenameGenerator.generateTimestampedFilename(filename,"labels", "pdf");
                            proofOfPostageCreator.createLabels(proofOfPostage,labelsFilename);
                            pdfViewer.openPDF(workingFolder + "\\" + labelsFilename, "viewerExecuteLabels");
                        }

                        // Open the proof of postage pdf
                        pdfViewer.openPDF(workingFolder + "\\" + proofOfPostage.getFilename(), "viewerExecuteProofOfPostage");

                        // If stopWatchingAfterFirstRun is true then stop watching the folder
                        if(stopWatchingAfterFirstRun){
                            System.out.println("Stopping watching folder after first run");
                            // Stop the application with code 0
                            System.exit(0);
                        }
                    }
                }
                // Ignore file types/events
                else {
                    System.out.println("Ignoring event kind : " + event.kind() + " - File : " + event.context());
                }
            }
            // Reset the key
            poll = key.reset();
        }
    }

}