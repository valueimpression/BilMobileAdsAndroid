package com.bil.bilmobileads.entity;

import java.util.ArrayList;

public class AdUnitObj {
    public String placement;
    public String typel;
    public ADFormat defaultType; // Kieu hien thi mac dinh
    public boolean isActive; // -> cho hien thi
    public ArrayList<AdInfor> adInfor;

    public AdUnitObj(String placement, String typel, ADFormat defaultType, boolean isActive, ArrayList<AdInfor> adInfor) {
        this.placement = placement;
        this.typel = typel;
        this.defaultType = defaultType;
        this.isActive = isActive;
        this.adInfor = adInfor;
    }
}
