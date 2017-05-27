/**
 *   Copyright (C) 2010,2013 Thundersoft Corporation
 *   All rights Reserved
 */

package com.ucamera.ucomm.sns.services;

public class ShareContent {
    private String mMessage;
    private String mTitle;
    private String mUCamShare;

    private String mLatitude;
    private String mLongitude;

    public ShareContent(String title, String message) {
        this.mTitle = title;
        this.mMessage = message;
    }

    public String getMessage() {
        return this.mMessage;
    }

    public void setMessage(String message) {
        this.mMessage = message;
    }

    public void setTitle(String title) {
        this.mTitle = title;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setUCamShare(String share) {
        mUCamShare = share;
    }

    public String getUCamShare() {
        return mUCamShare;
    }

    public void setLocation(String latitude, String longitude) {
        this.mLatitude = latitude;
        this.mLongitude = longitude;
    }

    public String getLatitude() {
        return this.mLatitude;
    }

    public String getLongitude() {
        return this.mLongitude;
    }
}
