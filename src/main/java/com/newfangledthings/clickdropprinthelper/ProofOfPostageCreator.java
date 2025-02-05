// src/main/java/com/newfangledthings/clickAndDropPrintHelper/ProofOfPostageCreator.java
package com.newfangledthings.clickdropprinthelper;

import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class ProofOfPostageCreator {

    private final Config config;
    private final String workingFolder;

    public ProofOfPostageCreator(Config config) {
        this.config = config;
        this.workingFolder = config.getProperty("WorkingFolder");
    }

    /**
     * Uses pdfBox to open a Royal Mail PDF and extract text from the shipping label.
     * Once this is done it will then open the Royal Mail proof of postage pdf and
     * populate the boxes with the text stripped from the shipping labels.
     *
     * @param filename      is the file that it will attempt to process
     */
    public ProofOfPostage[] createProofOfPostage(String filename) {
        ArrayList<ProofOfPostage> proofOfPostageArrayList = new ArrayList<>();
        try {
            System.out.println("Attempting to process " + filename);
            PDDocument pdfShippingLabels = PDDocument.load(new File(filename));
            String text = new PDFTextStripper().getText(pdfShippingLabels);
            List<ShippingLabel> shippingLabels = new ArrayList<>();
            int countTrackingNumbers = 0;
            String[] lines = text.split("\\r\\n");
            for (int i = 0; i <= lines.length - 1; i++) {
                if (lines[i].contains("Shipping Address")) {
                    shippingLabels.add(new ShippingLabel(
                            lines[i + 1],
                            lines[i + 2] + " " + lines[i + 3] + " " + lines[i + 4])
                    );
                }
                if (lines[i].contains("Postage Paid GB")) {
                    if (lines[i - 2].startsWith("Tracked")) {
                        shippingLabels.get(countTrackingNumbers).SetTrackingNumber(lines[i - 2].replace("No Signature", "") + " " + lines[i + 3].replace(" ", "").replace("-", ""));
                    } else {
                        shippingLabels.get(countTrackingNumbers).SetTrackingNumber(lines[i - 2] + " " + lines[i + 3].replace(" ", "").replace("-", ""));
                    }
                    countTrackingNumbers++;
                }
            }
            if (shippingLabels.size() == 0) {
                System.out.println("Output file is empty, exiting early");
                return null;
            }
            for (int p = 0; p < shippingLabels.size(); p += 30) {
                File file = new File(workingFolder + "\\Royal Mail Proof Of Postage.pdf");
                PDDocument pdfRoyalMailTemplate = PDDocument.load(file);
                PDDocumentCatalog docCatalog = pdfRoyalMailTemplate.getDocumentCatalog();
                PDAcroForm acroForm = docCatalog.getAcroForm();
                int countForNumberofItems = 0;
                for (int i = p; i < shippingLabels.size() && i < p + 30; i++) {
                    ShippingLabel label = shippingLabels.get(i);
                    if (i == p) {
                        acroForm.getField("1").setValue(label.getName());
                        acroForm.getField("my text here").setValue(label.getAddress());
                        acroForm.getField("service used 1").setValue(label.getTrackingNumber());
                    } else {
                        acroForm.getField("" + (i - p + 1)).setValue(label.getName());
                        acroForm.getField("address and postcode " + (i - p + 1)).setValue(label.getAddress());
                        acroForm.getField("service used " + (i - p + 1)).setValue(label.getTrackingNumber());
                    }
                    System.out.println("Content added [" + label.getName() + "," + label.getAddress() + "," + label.getTrackingNumber() + "]");
                    countForNumberofItems++;
                }
                acroForm.getField("Text57").setValue(countForNumberofItems + " items");
                acroForm.getField("Text58").setValue(new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
                String uniqueFilename = "proof_" + UUID.randomUUID() + ".pdf";
                pdfRoyalMailTemplate.save(workingFolder + "\\" + uniqueFilename);
                pdfRoyalMailTemplate.close();
                proofOfPostageArrayList.add(new ProofOfPostage(
                        uniqueFilename,
                        shippingLabels.subList(p, Math.min(p + 30, shippingLabels.size())),
                        filename,
                        p));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return proofOfPostageArrayList.toArray(new ProofOfPostage[0]);
    }

    /**
     * Uses pdfBox to open a Royal Mail PDF and extract QR codes from the shipping label.
     *
     * @param proofOfPostage   is the proof of postage document object that hold all the data required to create the pdf
     */
    public void addQRCodesToProofOfPostage(ProofOfPostage proofOfPostage) {
        try {
            System.out.println("Attempting to process " + workingFolder + "\\" + proofOfPostage.getFilename() + " for images");

            PDDocument docLoad = PDDocument.load(new File(proofOfPostage.getSourcePDF()));
            PDDocument docProofPostage = PDDocument.load(new File(workingFolder + "\\" + proofOfPostage.getFilename()));
            PDPage newPage = new PDPage(PDRectangle.A4);
            PDPageContentStream contents = new PDPageContentStream(docProofPostage, newPage);
            docProofPostage.addPage(newPage);

            PDPageTree list = docLoad.getPages();
            int x = 40, y = 750, count = 0;
            for (PDPage page : list) {
                PDResources pdResources = page.getResources();

                for (COSName c : pdResources.getXObjectNames()) {
                    PDXObject o = pdResources.getXObject(c);
                    if (o instanceof PDImageXObject) {
                        if (((PDImageXObject) o).getWidth() == 128 || ((PDImageXObject) o).getWidth() == 1050) {
                            System.out.println("Found QR code");

                            if (count >= proofOfPostage.getImageIndex() && count < proofOfPostage.getImageIndex() + 30) {
                                ShippingLabel label = proofOfPostage.getShippingLabels().get(count);
                                String trackingNumber = label.getTrackingNumber().trim();
                                String name = label.getName();

                                if (trackingNumber.length() > 10) {
                                    contents.beginText();
                                    contents.setFont(PDType1Font.COURIER, 9);
                                    contents.newLineAtOffset(x, y - 15);
                                    contents.showText(trackingNumber.substring(10));
                                    contents.endText();

                                    contents.beginText();
                                    contents.setFont(PDType1Font.COURIER, 9);
                                    contents.newLineAtOffset(x, y - 30);
                                    contents.showText(name);
                                    contents.endText();
                                } else {
                                    System.out.println("Tracking number is too short, skipping");
                                }

                                if (((PDImageXObject) o).getWidth() == 1050) {
                                    PDImageXObject pdi = LosslessFactory.createFromImage(docLoad, ((PDImageXObject) o).getImage().getSubimage(70, 450, 280, 280));
                                    contents.drawImage(pdi, x, y, pdi.getWidth() / 5, pdi.getHeight() / 5);
                                } else {
                                    contents.drawImage((PDImageXObject) o, x, y, 60, 60);
                                }

                                x += 110;
                                if (x > 500) {
                                    x = 40;
                                    y -= 135;
                                }
                            } else {
                                System.out.println("Image already added to previous pdf, skipping");
                            }

                            count++;
                        } else {
                            System.out.println("Output image not QR, skipping");
                        }
                    }
                }
            }

            contents.close();
            docProofPostage.save(workingFolder + "\\" + proofOfPostage.getFilename());
            docProofPostage.close();
            docLoad.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void removeFirstPage(ProofOfPostage proofOfPostage) throws IOException {
        PDDocument doc = PDDocument.load(new File(workingFolder + "\\" + proofOfPostage.getFilename()));
        doc.removePage(0);
        doc.save(workingFolder + "\\" + proofOfPostage.getFilename());
        doc.close();
    }

    public void createPackingSlips(ProofOfPostage proofOfPostage, String packingFilename) throws IOException {
        FileUtils.copyFile(new File(config.getProperty("WatchFolder") + "\\" + proofOfPostage.getSourcePDF()), new File(workingFolder + "\\" + packingFilename));
        PDDocument doc = PDDocument.load(new File(workingFolder + "\\" + packingFilename));
        // Find out the number of pages then remove every 5th page
        int pageCount = doc.getNumberOfPages();
        List<Integer> pagesToRemove = new ArrayList<>();
        // Collect indices of every fifth page
        for (int i = 0; i < pageCount; i++) {
            if ((i + 1) % 5 == 0) {
                pagesToRemove.add(i);
            }
        }
        // Remove pages in reverse order
        for (int i = pagesToRemove.size() - 1; i >= 0; i--) {
            doc.removePage(pagesToRemove.get(i));
        }
        // now we want to add a custom header and footer to each page of the pdf
        PDPageTree list = doc.getPages();
        var pageWidth = doc.getPage(0).getMediaBox().getWidth();
        var pageHeight = doc.getPage(0).getMediaBox().getHeight();
        for (PDPage page : list) {
            PDResources pdResources = page.getResources();
            PDPageContentStream contents = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true);
            // Check if packing slip header and footer exists
            String headerImage = config.getProperty("PackingSlipHeaderImage");
            if (new File(workingFolder + "\\" + headerImage ).exists()) {
                PDImageXObject image = PDImageXObject.createFromFile(workingFolder + "\\" + headerImage, doc);
                var height = (image.getHeight() * pageWidth) / image.getWidth();
                contents.drawImage(image, 0, pageHeight - height, pageWidth, height);
            }
            String footerImage = config.getProperty("PackingSlipFooterImage");
            if (new File(workingFolder + "\\" + footerImage).exists()) {
                PDImageXObject image = PDImageXObject.createFromFile(workingFolder + "\\" + footerImage, doc);
                var height = (image.getHeight() * pageWidth) / image.getWidth();
                contents.drawImage(image, 0, 0, pageWidth, height);
            }
            contents.close();
        }
        doc.save(workingFolder + "\\" + packingFilename);
        doc.close();
    }

    public void createLabels(ProofOfPostage proofOfPostage, String labelsFilename) throws IOException {
        FileUtils.copyFile(new File(config.getProperty("WatchFolder") + "\\" + proofOfPostage.getSourcePDF()), new File(workingFolder + "\\" + labelsFilename));
        PDDocument doc = PDDocument.load(new File(workingFolder + "\\" + labelsFilename));
        // Find out the number of pages then remove every 5th page
        int pageCount = doc.getNumberOfPages();
        List<Integer> pagesToRemove = new ArrayList<>();
        // Collect indices of every fifth page
        for (int i = 0; i < pageCount; i++) {
            if (!((i + 1) % 5 == 0)) {
                pagesToRemove.add(i);
            }
        }
        // Remove pages in reverse order
        for (int i = pagesToRemove.size() - 1; i >= 0; i--) {
            doc.removePage(pagesToRemove.get(i));
        }
        doc.save(workingFolder + "\\" + labelsFilename);
        doc.close();
    }
}