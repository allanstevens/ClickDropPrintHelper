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
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProofOfPostageCreator {

    private final Config config;
    private final String storeFolder;
    private final String watchFolder;

    public ProofOfPostageCreator(Config config) {
        this.config = config;
        this.storeFolder = config.getProperty("StoreFolder");
        this.watchFolder = config.getProperty("WatchFolder");
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
        try (PDDocument pdfShippingLabels = PDDocument.load(new File(watchFolder + "\\" + filename))) {
            System.out.println("Attempting to process " + filename);
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
            if (shippingLabels.isEmpty()) {
                System.out.println("Has not found any postage details");
                return null;
            }

            // Now create the Proof Of Postage PDF, if there is more than 30, then create additional pdfs
            for (int p = 0; p < shippingLabels.size(); p += 30) {
                InputStream file = getClass().getClassLoader().getResourceAsStream("Royal Mail Proof Of Postage.pdf");
                //File file = new File(workingFolder + "\\Royal Mail Proof Of Postage.pdf");
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
                    System.out.println("Found postage [" + label.getName() + "," + label.getAddress() + "," + label.getTrackingNumber() + "]");
                    countForNumberofItems++;
                }
                acroForm.getField("Text57").setValue(countForNumberofItems + " items");
                acroForm.getField("Text58").setValue(new SimpleDateFormat("dd/MM/yyyy").format(new Date()));

                String uniqueFilename = FilenameGenerator.generateFilename(filename,"proof");
                pdfRoyalMailTemplate.save(storeFolder + "\\" + uniqueFilename);
                pdfRoyalMailTemplate.close();

                proofOfPostageArrayList.add(new ProofOfPostage(
                        uniqueFilename,
                        shippingLabels.subList(p, Math.min(p + 30, shippingLabels.size())),
                        filename,
                        p));
                System.out.println("Created proof of postage file " + uniqueFilename);
            }
        } catch (IOException e) {
            System.err.println("Error creating proof of postage: " + e.getMessage());
            e.printStackTrace(System.err);
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
            System.out.println("Attempting to process " + storeFolder + "\\" + proofOfPostage.getFilename() + " for images");

            PDDocument docLoad = PDDocument.load(new File(watchFolder + "\\" + proofOfPostage.getSourcePDF()));
            PDDocument docProofPostage = PDDocument.load(new File(storeFolder + "\\" + proofOfPostage.getFilename()));
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
                            //System.out.println("Found QR code");

                            if (count >= proofOfPostage.getImageIndex() && count < proofOfPostage.getImageIndex() + 30) {

                                // Add the tracking number and name
                                ShippingLabel label = proofOfPostage.getShippingLabels().get(count - proofOfPostage.getImageIndex());
                                String trackingNumber = label.getTrackingNumber().trim();
                                String name = label.getName();

                                contents.beginText();
                                contents.setFont(PDType1Font.COURIER, 9);
                                contents.newLineAtOffset(x-5, y - 15);
                                contents.showText(trackingNumber.substring(10));
                                contents.endText();

                                contents.beginText();
                                contents.setFont(PDType1Font.COURIER, 9);
                                contents.newLineAtOffset(x, y - 30);
                                contents.showText(name);
                                contents.endText();

                                // Add the QR image
                                if (((PDImageXObject) o).getWidth() == 1050) {
                                    PDImageXObject pdi = LosslessFactory.createFromImage(docLoad, ((PDImageXObject) o).getImage().getSubimage(70, 450, 280, 280));
                                    contents.drawImage(pdi, x, y, (float) pdi.getWidth() / 5, (float) pdi.getHeight() / 5);
                                } else {
                                    contents.drawImage((PDImageXObject) o, x, y, 60, 60);
                                }

                                x += 110;
                                if (x > 500) {
                                    x = 40;
                                    y -= 135;
                                }
                            }
                            count++;
                        }
                    }
                }
            }
            contents.close();
            docProofPostage.save(storeFolder + "\\" + proofOfPostage.getFilename());
            docProofPostage.close();
            docLoad.close();
            System.out.println("Added "+count+" QR codes to " +proofOfPostage.getFilename());

        } catch (IOException e) {
            System.err.println("Error adding QR codes: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    public void removeFirstPage(ProofOfPostage proofOfPostage) throws IOException {
        PDDocument doc = PDDocument.load(new File(storeFolder + "\\" + proofOfPostage.getFilename()));
        doc.removePage(0);
        doc.save(storeFolder + "\\" + proofOfPostage.getFilename());
        doc.close();
    }

    public List<Integer> findPackingSlips(Boolean isPackingSlips, PDDocument doc) throws IOException {
        List<Integer> packingSlipPages = new ArrayList<>();

        // Regex pattern to match "Shipping Address" on the first or second line followed by an empty line
        String regexPattern = "(?i)^(.*\\r?\\n)?Shipping Address";
        Pattern pattern = Pattern.compile(regexPattern);

        // Prepare text extractor
        PDFTextStripper textStripper = new PDFTextStripper();

        // Loop through all pages and find matches
        for (int i = 0; i < doc.getNumberOfPages(); i++) {
            textStripper.setStartPage(i + 1);
            textStripper.setEndPage(i + 1);
            String pageText = textStripper.getText(doc).trim();

            Matcher matcher = pattern.matcher(pageText);
            if (isPackingSlips == matcher.find()) { // If isPackingSlips is true, keep pages with the pattern; else, keep pages without it
                packingSlipPages.add(i);
            }
        }

        return packingSlipPages;
    }


    public void createPackingSlips(ProofOfPostage proofOfPostage, String packingFilename) throws IOException {
        FileUtils.copyFile(new File(watchFolder + "\\" + proofOfPostage.getSourcePDF()), new File(storeFolder + "\\" + packingFilename));
        PDDocument doc = PDDocument.load(new File(storeFolder + "\\" + packingFilename));

        // Remove pages in reverse order
        List<Integer> pagesToRemove = findPackingSlips(false, doc); // Get pages that do NOT match
        for (int i = pagesToRemove.size() - 1; i >= 0; i--) {
            doc.removePage(pagesToRemove.get(i));
        }

        // now we want to add a custom header and footer to each page of the pdf
        PDPageTree list = doc.getPages();
        var pageWidth = doc.getPage(0).getMediaBox().getWidth();
        var pageHeight = doc.getPage(0).getMediaBox().getHeight();
        for (PDPage page : list) {
            PDPageContentStream contents = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true);
            // Check if packing slip header and footer exists
            String headerImage = config.getProperty("PackingSlipHeaderImage");
            if (new File( headerImage ).exists()) {
                PDImageXObject image = PDImageXObject.createFromFile(headerImage, doc);
                var height = (image.getHeight() * pageWidth) / image.getWidth();
                contents.drawImage(image, 0, pageHeight - height, pageWidth, height);
            }
            String footerImage = config.getProperty("PackingSlipFooterImage");
            if (new File(footerImage).exists()) {
                PDImageXObject image = PDImageXObject.createFromFile(footerImage, doc);
                var height = (image.getHeight() * pageWidth) / image.getWidth();
                contents.drawImage(image, 0, 0, pageWidth, height);
            }
            contents.close();
        }
        doc.save(storeFolder + "\\" + packingFilename);
        doc.close();
    }

    public void createLabels(ProofOfPostage proofOfPostage, String labelsFilename) throws IOException {
        FileUtils.copyFile(new File(watchFolder + "\\" + proofOfPostage.getSourcePDF()), new File(storeFolder + "\\" + labelsFilename));
        PDDocument doc = PDDocument.load(new File(storeFolder + "\\" + labelsFilename));

        // Remove pages in reverse order
        List<Integer> pagesToRemove = findPackingSlips(true, doc); // Get pages that do NOT match
        for (int i = pagesToRemove.size() - 1; i >= 0; i--) {
            doc.removePage(pagesToRemove.get(i));
        }

        doc.save(storeFolder + "\\" + labelsFilename);
        doc.close();
    }
}