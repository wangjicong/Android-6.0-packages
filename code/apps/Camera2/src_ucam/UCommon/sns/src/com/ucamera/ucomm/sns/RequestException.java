/**
 *   Copyright (C) 2010,2013 Thundersoft Corporation
 *   All rights Reserved
 */
package com.ucamera.ucomm.sns;

public class RequestException extends Exception {

    private String mRequestUrl;
    private int mCode;

    public RequestException(int code, String url, String message) {
        super(message);
        this.mCode = code;
        this.mRequestUrl = url;
    }

    public String getRequestUrl() {
        return this.mRequestUrl;
    }

    public int getCode() {
        return this.mCode;
    }
}
