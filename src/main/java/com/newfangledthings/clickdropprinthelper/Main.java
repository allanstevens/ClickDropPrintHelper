package com.newfangledthings.clickdropprinthelper;

import java.awt.event.KeyListener;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.awt.Toolkit;

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
        this.config = new Config(configFilename);
        this.proofOfPostageCreator = new ProofOfPostageCreator(config);
        this.pdfViewer = new PDFViewer(config);
    }

    /**
     * Main static method to execute the application with the config file
     * as an argument or without any arguments to use the default config
     * file (config.properties)
     *
     * @param args is the config file
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
        String storeFolder = config.getProperty("StoreFolder");
        boolean createProofOfPostage = config.getProperty("CreateProofOfPostage").equals("yes");
        boolean createLabels = config.getProperty("CreateLabels").equals("yes");
        boolean createPackingSlips = config.getProperty("CreatePackingSlips").equals("yes");
        boolean createQRs = config.getProperty("CreateQRs").equals("yes");
        String beforeRun = config.getProperty("BeforeRun");

        // Display all the var from above
        System.out.println("Properties loaded from "+ config.getConfigName());

        System.out.println("Will "+(createProofOfPostage?"":"NOT ")+"create proof of postage pdf");
        System.out.println("Will "+(createQRs?"":"NOT ")+"create additional QR page");
        System.out.println("Will "+(createPackingSlips?"":"NOT ")+"create packing slips pdf");
        System.out.println("Will "+(createLabels?"":"NOT ")+"create labels pdf");

        System.out.println("Created pdf's will be stored in folder " + storeFolder);
        // Create watch service to monitor download folder
        WatchService watchService = FileSystems.getDefault().newWatchService();
        Path path = Paths.get(watchFolder);
        path.register(watchService, ENTRY_CREATE);//, ENTRY_MODIFY, ENTRY_DELETE);
        System.out.println("Monitoring download folder " + watchFolder);

        // Adds a key listener to all you to close the app if you press X
        System.out.println("Type EXIT↵ to exit the application");
        Thread keyListenerThread = new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String input = scanner.nextLine();
                if ("exit".equalsIgnoreCase(input)) {
                    System.out.println("Stopping watcher");
                    System.exit(0);
                    break;
                }
            }
        });
        keyListenerThread.start();

        // Poll for events (look for pdf files in watch folder)
        boolean poll = true;
        System.out.println("I have started watching for files...");
        WatchKey key = watchService.take();

        while (poll) {

            for (WatchEvent<?> event : key.pollEvents()) {

                // Quick pause to stop file locking
                TimeUnit.SECONDS.sleep(1);

                // Get the filename from the event context
                String filename = event.context().toString();

                // If an "order*.pdf" is created in the watch folder
                if (filename.startsWith("order") && filename.endsWith(".pdf")) {

                    //System.out.println("Processing file " + filename);
                    if ("prompt".equals(beforeRun)){
                        //Make a beep ound
                        Toolkit.getDefaultToolkit().beep();
                        // Ask for user interaction
                        Scanner scanner = new Scanner(System.in);
                        System.out.println("Found " + filename + ". Do you want to process or ignore this file? (process/ignore)");
                        String userInput = scanner.nextLine();

                        if (userInput.toLowerCase().startsWith("i")) {
                            System.out.println("Ignoring file " + filename);
                            continue;
                        }
                    }

                    // PDF found, will now see if it's a Royal Mail shipping label
                    var proofOfPostages = proofOfPostageCreator.createProofOfPostage(filename);
                    if (proofOfPostages == null) {
                        System.out.println("NOT recognised as  Click & Drop file, finished processing early");
                        //System.err.println("Error creating proof of postage for file " + filename);
                        continue;
                    }
                    for (ProofOfPostage proofOfPostage : proofOfPostages) {

                        //It's a Royal Mail shipping label, will now create the QR codes
                        if (createQRs) {
                            proofOfPostageCreator.addQRCodesToProofOfPostage(proofOfPostage);
                        }

                        // If you have asked for no proof of postage then delete the first page or delete the file is qr codes don't exist
                        if (!createProofOfPostage) {
                            //If no qr codes then just delete the proof of postage
                            if (!createQRs) {
                                System.out.println("Deleting pdf " + filename);
                                Files.delete(Paths.get(watchFolder + "\\" + event.context()));
                            } else {
                                proofOfPostageCreator.removeFirstPage(proofOfPostage);
                                System.out.println("Removed proof of postage page from  " + filename);
                            }
                        }

                        // Open the proof of postage pdf
                        pdfViewer.openPDF(storeFolder + "\\" + proofOfPostage.getFilename(), "ViewerExecuteProofOfPostage");

                        // Only do the next sections if it's the first pdf
                        if (proofOfPostage.getImageIndex()==0) {

                            // If createPackingSlips is true copy original pdf to working folder then remove every 5th page (which is the label)
                            if (createPackingSlips) {
                                var packingFilename = FilenameGenerator.generateFilename(filename, "packing");
                                proofOfPostageCreator.createPackingSlips(proofOfPostage, packingFilename);
                                pdfViewer.openPDF(storeFolder + "\\" + packingFilename, "ViewerExecutePackingSlip");
                                System.out.println("Created packing slips pdf " + packingFilename);
                            }

                            // If createLabels is true then open the pdf with the labels
                            if (createLabels) {
                                var labelsFilename = FilenameGenerator.generateFilename(filename, "labels", "pdf");
                                proofOfPostageCreator.createLabels(proofOfPostage, labelsFilename);
                                pdfViewer.openPDF(storeFolder + "\\" + labelsFilename, "ViewerExecuteLabels");
                                System.out.println("Created labels pdf " + labelsFilename);
                            }
                        }

                        // If BeforeRun is set to stop
                        if(beforeRun=="stop"){
                            System.out.println("Stopping watching folder after first run");
                            // Stop the application with code 0
                            System.exit(0);
                        }
                    }
                    System.out.println("Finished processing file " + filename);
                }
                // Ignore file types/events
                else {
                    System.out.println("Ignoring file " + event.context());
                }
            }
            // Reset the key
            poll = key.reset();
        }
    }

}