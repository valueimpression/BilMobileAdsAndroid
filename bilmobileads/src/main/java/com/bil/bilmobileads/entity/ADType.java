package com.bil.bilmobileads.entity;

public enum ADType {
    Banner("banner"),
    Interstitial("interstitial"),
    Rewarded("rewarded");

    private String type;

    ADType(String type) {
        this.type = type;
    }
}
