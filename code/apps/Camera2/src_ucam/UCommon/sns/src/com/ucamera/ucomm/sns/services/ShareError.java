/**
 *   Copyright (C) 2010,2013 Thundersoft Corporation
 *   All rights Reserved
 */
package com.ucamera.ucomm.sns.services;

public class ShareError {
    private String mCode;
    private String mMessage;

    public ShareError(String code, String msg){
        this.mCode = code;
        this.mMessage = msg;
    }
    public String getCode() { return mCode;}
    public void setCode(String code) {this.mCode = code;}
    public String getMessage() {return mMessage;}
    public void setMessage(String message) {this.mMessage = message;}
}
