package com.masterlibs.commonlibs;

import com.common.control.MyApplication;
import com.common.control.model.PurchaseModel;

import java.util.Collections;
import java.util.List;

public class App extends MyApplication {
    public static final String PRODUCT_LIFETIME = "android.test.purchased";

    @Override
    protected void onApplicationCreate() {

    }

    @Override
    protected boolean hasAdjust() {
        return false;
    }

    @Override
    protected String getAdjustAppToken() {
        return null;
    }

    @Override
    protected boolean hasAds() {
        return true;
    }

    @Override
    protected boolean isShowDialogLoadingAd() {
        return true;
    }

    @Override
    protected boolean isShowAdsTest() {
        return false;
    }

    @Override
    public boolean enableAdsResume() {
        return true;
    }

    @Override
    public String getOpenAppAdId() {
        return "ca-app-pub-3940256099942544/3419835294";
    }

    @Override
    public String getPolicyUrl() {
        return null;
    }

    @Override
    protected String getSubjectSupport() {
        return null;
    }

    @Override
    protected String getEmailSupport() {
        return null;
    }

    @Override
    public boolean isInitBilling() {
        return false;
    }

    @Override
    public List<PurchaseModel> getPurchaseList() {
        return Collections.singletonList(new PurchaseModel(PRODUCT_LIFETIME, PurchaseModel.ProductType.INAPP));
    }
}
