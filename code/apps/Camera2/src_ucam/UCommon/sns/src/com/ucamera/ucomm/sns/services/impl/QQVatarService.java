/*
 * Copyright (C) 2014,2015 Thundersoft Corporation
 * All rights Reserved
 */
package com.ucamera.ucomm.sns.services.impl;

import org.scribe.oauth.OAuthService;

import android.app.Activity;
import android.content.Context;

import com.ucamera.ucomm.sns.services.AbstractService;
import com.ucamera.ucomm.sns.services.LoginListener;
import com.ucamera.ucomm.sns.services.LogoutListener;
import com.ucamera.ucomm.sns.services.ShareContent;
import com.ucamera.ucomm.sns.services.ShareFile;
import com.ucamera.ucomm.sns.tencent.QQVatar;

public class QQVatarService extends AbstractService {
    @Override
    public boolean share(ShareContent share, ShareFile file) {
        return false;
    }

    @Override
    public String getServiceName() {
        return "QQVatar";
    }

    @Override
    protected OAuthService createOAuthService() {
        return null;
    }

    @Override
    protected ResponseChecker getResponseChecker(String action) {
        return null;
    }

    @Override
    public void login(Context context, LoginListener listener) {
        QQVatar.getInstance((Activity)context).login(listener, (Activity)context);
    }

    @Override
    public void logout(Context context, LogoutListener listener) {
        QQVatar.getInstance((Activity)context).doLogout();
        listener.onSuccess();
    }

    @Override
    public boolean isAuthorized(Activity activity) {
        return QQVatar.getInstance(activity).isAuthorezed();
    }

    @Override
    public void saveSession(Context context) {
    }

    @Override
    public void loadSession(Context context) {
    }
}
