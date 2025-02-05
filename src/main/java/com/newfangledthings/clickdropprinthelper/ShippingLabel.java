package com.newfangledthings.clickdropprinthelper;

public class ShippingLabel {
    private String name;
    private String address;
    private String trackingNumber;

    public ShippingLabel(String name, String address) {
        this.name = name;
        this.address = address;
    }

    public ShippingLabel(String name, String address, String trackingNumber) {
        this.name = name;
        this.address = address;
        this.trackingNumber = trackingNumber;
    }

    public void SetTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }
}