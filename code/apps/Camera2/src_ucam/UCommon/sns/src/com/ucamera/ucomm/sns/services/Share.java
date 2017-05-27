/*
 * Copyright (C) 2010,2013 Thundersoft Corporation
 * All rights Reserved
 */
package com.ucamera.ucomm.sns.services;

public class Share {
    private String mMessage;
    private String mTitle;

    public Share(String title, String message) {
        this.mTitle   = title;
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
}
