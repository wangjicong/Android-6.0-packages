package com.sprd.drmplugin;

import android.net.Uri;
import com.android.browser.R;
import android.app.AddonManager;

public class BrowserPlugInDrm {

    static BrowserPlugInDrm sInstance;

    public BrowserPlugInDrm(){
    }

    public static BrowserPlugInDrm getInstance(){
        if (sInstance != null)
            return sInstance;
        sInstance = (BrowserPlugInDrm) AddonManager.getDefault().getAddon(R.string.feature_browserplugdrm, BrowserPlugInDrm.class);
        return sInstance;
    }

    public boolean canGetDrmPath(){
        return true;
    }

    public Uri getDrmPath(String url, String mimetype , String filename){
        return null;
    }

    public CharSequence getFilePath(CharSequence path , String str , String filename){
        return path;
    }

    public boolean getMimeType(String mimetype){
        return true;
    }

}
