/**
 *   Copyright (C) 2010,2013 Thundersoft Corporation
 *   All rights Reserved
 */
package com.ucamera.ucomm.sns.services;

import android.app.Activity;
import android.content.Context;

public interface ShareService {

    public static final String CALLBACK_URL = "android://callback.ucam";
    public static final String CALLBACK_URL_ALTERNATE = "http://www.u-camera.com/api/callback";
    public static final String CALLBACK_URL_FACEBOOK = "fbconnect://success";
    public static final String CALLBACK_URL_GOOGLE  = "http://localhost/";

    public boolean isAuthorized();
    public boolean isAuthorized(Activity activity);

    public void saveSession(Context context);

    public void loadSession(Context context);

    /*
     * Used to login
     */
    public void login(Context context, LoginListener listener);

    public void logout(Context context, LogoutListener listener);

    /*
     * Used to share
     */
    public boolean share(ShareContent share, ShareFile file);
    public ShareError getShareError();
    public String getServiceName();
}
