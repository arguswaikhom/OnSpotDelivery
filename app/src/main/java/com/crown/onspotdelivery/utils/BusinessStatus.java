package com.crown.onspotdelivery.utils;

import com.crown.library.onspotlibrary.model.business.BusinessOSD;
import com.crown.library.onspotlibrary.utils.emun.BusinessRequestStatus;

import java.util.List;

public class BusinessStatus {
    private static BusinessStatus instance;

    public static BusinessStatus getInstance() {
        if (instance == null) return new BusinessStatus();
        return instance;
    }

    public BusinessRequestStatus get(List<BusinessOSD> osdList, String bussId) {
        if (osdList == null || osdList.isEmpty()) return null;
        for (BusinessOSD b :osdList) {
            if (b.getBusinessRefId().equals(bussId)) {
                return BusinessRequestStatus.valueOf(b.getStatus());
            }
        }
        return null;
    }
}
