package com.crown.onspotdelivery.utils;

import com.crown.library.onspotlibrary.model.business.BusinessOSD;
import com.crown.library.onspotlibrary.utils.OSListUtils;
import com.crown.library.onspotlibrary.utils.emun.BusinessRequestStatus;

import java.util.ArrayList;
import java.util.List;

public class BusinessUtils {
    public static BusinessRequestStatus getBusinessStatus(List<BusinessOSD> osdList, String bussId) {
        if (osdList == null || osdList.isEmpty()) return null;
        for (BusinessOSD b : osdList) {
            if (b.getBusinessRefId().equals(bussId)) {
                return BusinessRequestStatus.valueOf(b.getStatus());
            }
        }
        return null;
    }

    public static List<String> getAcceptedBusinessIds(List<BusinessOSD> businessOSDList) {
        if (OSListUtils.isEmpty(businessOSDList)) return new ArrayList<>();
        List<String> bussList = new ArrayList<>();
        for (BusinessOSD b : businessOSDList) {
            if (b.getStatus().equals(BusinessRequestStatus.ACCEPTED.name())) {
                bussList.add(b.getBusinessRefId());
            }
        }
        return bussList;
    }
}
