package com.newfangledthings.clickdropprinthelper;

import java.util.List;

public class ProofOfPostage {
    private List<ShippingLabel> shippingLabels;
    private String filename;
    private String sourcePDF;
    private int imageIndex;

    public ProofOfPostage(String filename, List<ShippingLabel> content, String sourcePDF) {
        this.shippingLabels = content;
        this.filename = filename;
        this.sourcePDF = sourcePDF;
        this.imageIndex = 0;
    }

    public ProofOfPostage(String filename, List<ShippingLabel> content, String sourcePDF, int imageIndex) {
        this.shippingLabels = content;
        this.filename = filename;
        this.sourcePDF = sourcePDF;
        this.imageIndex = imageIndex;
    }

    public List<ShippingLabel> getShippingLabels() {
        return shippingLabels;
    }

    public void setShippingLabels(List<ShippingLabel> shippingLabels) {
        this.shippingLabels = shippingLabels;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getSourcePDF() {
        return sourcePDF;
    }

    public void setSourcePDF(String sourcePDF) {
        this.sourcePDF = sourcePDF;
    }

    public int getImageIndex() {
        return imageIndex;
    }

    public void setImageIndex(int imageIndex) {
        this.imageIndex = imageIndex;
    }
}