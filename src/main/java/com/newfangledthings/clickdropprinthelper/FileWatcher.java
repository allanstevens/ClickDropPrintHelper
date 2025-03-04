// FileWatcher.java
package com.newfangledthings.clickdropprinthelper;

import java.io.IOException;
import java.nio.file.*;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import javax.swing.*;
import java.awt.Toolkit;

public class FileWatcher {
    private final Config config;
    private final ProofOfPostageCreator proofOfPostageCreator;
    private final PDFViewer pdfViewer;
    private final boolean consoleMode;

    public FileWatcher(Config config, boolean consoleMode) {
        this.config = config;
        this.proofOfPostageCreator = new ProofOfPostageCreator(config);
        this.pdfViewer = new PDFViewer(config);
        this.consoleMode = consoleMode;
    }

    public void watch() throws IOException, InterruptedException {
        String watchFolder = config.getProperty("WatchFolder");
        String storeFolder = config.getProperty("StoreFolder");
        boolean createProofOfPostage = config.getProperty("CreateProofOfPostage").equals("yes");
        boolean createLabels = config.getProperty("CreateLabels").equals("yes");
        boolean createPackingSlips = config.getProperty("CreatePackingSlips").equals("yes");
        boolean createQRs = config.getProperty("CreateQRs").equals("yes");
        String beforeRun = config.getProperty("BeforeRun");
        boolean includePrintOption = pdfViewer.checkPrintIsOption();
        System.out.println("Properties loaded from " + config.getConfigName());
        System.out.println("Will " + (createProofOfPostage ? "" : "NOT ") + "create proof of postage pdf");
        System.out.println("Will " + (createQRs ? "" : "NOT ") + "create additional QR page");
        System.out.println("Will " + (createPackingSlips ? "" : "NOT ") + "create packing slips pdf");
        System.out.println("Will " + (createLabels ? "" : "NOT ") + "create labels pdf");
        System.out.println("Will " + (includePrintOption ? "" : "NOT ") + "include print option");
        System.out.println("Created pdf's will be stored in folder " + storeFolder);

        WatchService watchService = FileSystems.getDefault().newWatchService();
        Path path = Paths.get(watchFolder);
        path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
        System.out.println("Monitoring download folder " + watchFolder);

        boolean poll = true;
        WatchKey key = watchService.take();

        while (poll) {
            for (WatchEvent<?> event : key.pollEvents()) {
                TimeUnit.SECONDS.sleep(1);
                String filename = event.context().toString();
                int userResponse = 0;

                if (filename.startsWith("order") && filename.endsWith(".pdf")) {
                    Toolkit.getDefaultToolkit().beep();

                    if ("prompt".equals(beforeRun)) {
                        if (consoleMode) {
                            Scanner scanner = new Scanner(System.in);
                            System.out.println("Found " + filename + ". What do you want to do?");
                            System.out.println("Options: " + (includePrintOption ? "1. Ignore 2. Create 3. Create & Print" : "1. Ignore 2. Create"));
                            userResponse = scanner.nextInt() - 1;
                        } else {
                            JDialog dialog = new JDialog();
                            dialog.setAlwaysOnTop(true);
                            dialog.setModal(true);
                            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

                            userResponse = JOptionPane.showOptionDialog(dialog,
                                    "Found " + filename + ". What do you want to do?",
                                    "Royal Mail Click & Drop file detected",
                                    JOptionPane.DEFAULT_OPTION,
                                    JOptionPane.QUESTION_MESSAGE,
                                    null,
                                    includePrintOption ? new String[]{"Ignore", "Create", "Create & Print"} : new String[]{"Ignore","Create"},
                                    null);

                            dialog.dispose();
                        }

                        if (userResponse == 0) {
                            System.out.println("Ignoring file " + filename);
                            continue;
                        }
                    }

                    var proofOfPostages = proofOfPostageCreator.createProofOfPostage(filename);
                    if (proofOfPostages == null) {
                        System.out.println("NOT recognised as Click & Drop file, finished processing early");
                        continue;
                    }
                    for (ProofOfPostage proofOfPostage : proofOfPostages) {
                        if (createQRs) {
                            proofOfPostageCreator.addQRCodesToProofOfPostage(proofOfPostage);
                        }
                        if (!createProofOfPostage) {
                            if (!createQRs) {
                                System.out.println("Deleting pdf " + filename);
                                Files.delete(Paths.get(watchFolder + "\\" + event.context()));
                            } else {
                                proofOfPostageCreator.removeFirstPage(proofOfPostage);
                                System.out.println("Removed proof of postage page from " + filename);
                            }
                        }
                        pdfViewer.openPDF(storeFolder + "\\" + proofOfPostage.getFilename(), userResponse == 1 ? "ViewerExecuteProofOfPostage" : "ViewerExecutePrintProofOfPostage");

                        if (proofOfPostage.getImageIndex() == 0) {
                            if (createPackingSlips) {
                                var packingFilename = FilenameGenerator.generateFilename(filename, "packing");
                                proofOfPostageCreator.createPackingSlips(proofOfPostage, packingFilename);
                                pdfViewer.openPDF(storeFolder + "\\" + packingFilename, userResponse == 1 ? "ViewerExecutePackingSlip" : "ViewerExecutePrintPackingSlip");
                                System.out.println("Created packing slips pdf " + packingFilename);
                            }
                            if (createLabels) {
                                var labelsFilename = FilenameGenerator.generateFilename(filename, "labels", "pdf");
                                proofOfPostageCreator.createLabels(proofOfPostage, labelsFilename);
                                pdfViewer.openPDF(storeFolder + "\\" + labelsFilename, userResponse == 1 ? "ViewerExecuteLabels" : "ViewerExecutePrintLabels");
                                System.out.println("Created labels pdf " + labelsFilename);
                            }
                        }
                        if ("stop".equals(beforeRun)) {
                            System.out.println("Stopping watching folder after first run");
                            System.exit(0);
                        }
                    }
                    System.out.println("Finished processing file " + filename);
                } else {
                    System.out.println("Ignoring file " + event.context());
                }
            }
            poll = key.reset();
        }
    }
}