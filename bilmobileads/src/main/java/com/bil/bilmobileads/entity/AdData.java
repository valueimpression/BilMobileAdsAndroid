package com.bil.bilmobileads.entity;

public class AdData {
    private final String currencyCode;
    private final int precision;
    private final double microsValue;

    public AdData(String currencyCode, int precision, double microsValue) {
        this.currencyCode = currencyCode;
        this.precision = precision;
        this.microsValue = microsValue;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public int getPrecision() {
        return precision;
    }

    public double getMicrosValue() {
        return microsValue;
    }
}
