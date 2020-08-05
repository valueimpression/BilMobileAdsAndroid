package com.bil.bilmobileads.entity;

import com.google.android.gms.ads.AdSize;

import java.util.ArrayList;

public class AdUnitObj {
    public String placement;
    public String type;
    public ADFormat defaultType; // Kieu hien thi mac dinh
    public boolean isActive; // -> cho hien thi
    public ArrayList<AdInfor> adInfor;
    public AdSize adSize;

    public AdUnitObj(String placement, String type, ADFormat defaultType, boolean isActive, ArrayList<AdInfor> adInfor, AdSize adSize) {
        this.placement = placement;
        this.type = type;
        this.defaultType = defaultType;
        this.isActive = isActive;
        this.adInfor = adInfor;
        this.adSize = adSize;
    }

    public AdUnitObj(String placement, String type, ADFormat defaultType, boolean isActive, ArrayList<AdInfor> adInfor) {
        this.placement = placement;
        this.type = type;
        this.defaultType = defaultType;
        this.isActive = isActive;
        this.adInfor = adInfor;
    }
}
