package com.newfangledthings.clickdropprinthelper;

import java.util.List;

public class ProofOfPostage {
    private final List<ShippingLabel> shippingLabels;
    private final String filename;
    private final String sourcePDF;
    private final int imageIndex;

    public ProofOfPostage(String filename, List<ShippingLabel> content, String sourcePDF, int imageIndex) {
        this.shippingLabels = content;
        this.filename = filename;
        this.sourcePDF = sourcePDF;
        this.imageIndex = imageIndex;
    }

    public List<ShippingLabel> getShippingLabels() {
        return shippingLabels;
    }

    public String getFilename() {
        return filename;
    }

    public String getSourcePDF() {
        return sourcePDF;
    }

    public int getImageIndex() {
        return imageIndex;
    }

}