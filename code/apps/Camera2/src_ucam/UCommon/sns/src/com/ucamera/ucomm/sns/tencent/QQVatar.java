/*
 * Copyright (C) 2014,2015 Thundersoft Corporation
 * All rights Reserved
 */
package com.ucamera.ucomm.sns.tencent;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;

import org.apache.http.conn.ConnectTimeoutException;
import org.json.JSONException;
import org.json.JSONObject;
import org.scribe.model.Token;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;

import com.tencent.open.HttpStatusException;
import com.tencent.open.NetworkUnavailableException;
import com.tencent.tauth.Constants;
import com.tencent.tauth.IRequestListener;
import com.tencent.tauth.IUiListener;
import com.tencent.tauth.Tencent;
import com.tencent.tauth.UiError;
import com.ucamera.ucomm.sns.R;
import com.ucamera.ucomm.sns.Util;
import com.ucamera.ucomm.sns.services.LoginListener;
import com.ucamera.ucomm.stat.StatApi;

public class QQVatar {
    private static final String APP_KEY = "100247210";
    private Tencent tencent;
    private Activity mActivity;
    private Uri mUri;
//    private QQVatar(Activity activity) {
//        this.mActivity = activity;
//    }
    private QQVatar() {}
    private static QQVatar mInstance;
//    public static QQVatar getInstance(Activity activity) {
//        if(mInstance == null) {
//            mInstance = new QQVatar(activity);
//        }
//        return mInstance;
//    }
    public void initActivity(Activity activity) {
        this.mActivity = activity;
    }
    public static QQVatar getInstance(Activity activity) {
        if(mInstance == null) {
            mInstance = new QQVatar();
        }
        return mInstance;
    }
    public void share(Uri uri, Activity activity) {
        mUri = uri;
        if(tencent == null || !tencent.isSessionValid()) {
            login(activity);
        } else {
            set(uri);
        }
    }

    private void set(Uri uri) {
        if (!Util.checkNetworkShowAlert(mActivity)){
            return;
        }
        Bundle params = new Bundle();
        params.putString(Constants.PARAM_AVATAR_URI, uri.toString());
        tencent.setAvatar(mActivity, params, new ShareUiListener(), R.anim.zoomin, R.anim.zoomout);
        StatApi.onEvent(mActivity, StatApi.EVENT_SNS_SHARE, "qqvatar");
    }
    public boolean isAuthorezed() {
        if(tencent != null) {
            return tencent.isSessionValid();
        }
        return false;
    }
    public void login(Activity activity) {
        if (!Util.checkNetworkShowAlert(activity)){
            return;
        }
        if(tencent == null) {
            tencent = Tencent.createInstance(APP_KEY, mActivity.getApplicationContext());
        }
        if(!tencent.isSessionValid()) {
            tencent.login(activity, "all", new LogInUiListener(null));
        }
    }
    public void login(LoginListener listener, Activity activity) {
        if (!Util.checkNetworkShowAlert(activity)){
            return;
        }
        if(tencent == null) {
            tencent = Tencent.createInstance(APP_KEY, mActivity.getApplicationContext());
        }
        if(!tencent.isSessionValid()) {
            tencent.login(activity, "all", new LogInUiListener(listener));
        }
    }
    public boolean doLogout() {
        tencent.logout(mActivity);
        return true;
    }

    private class LogInUiListener implements IUiListener {
        private LoginListener mListener;
        public LogInUiListener(LoginListener listener) {
            this.mListener = listener;
        }
        public void onComplete(JSONObject response) {
            if(tencent.getAccessToken() != null) {
                if(mListener != null) {
                    mListener.onSuccess(new Token(tencent.getAccessToken(), tencent.getAccessToken()));
                    updateUserInfo();
                } else {
                    updateUserInfo();
                    set(mUri);
                }
            }
        }
        @Override
        public void onError(UiError e) {
            if(mListener != null) {
                mListener.onFail(e.toString());
            }
        }
        @Override
        public void onCancel() {
            if(mListener != null) {
                mListener.onCancel();
            }
        }
    }
    private class ShareUiListener implements IUiListener {
        public ShareUiListener() {
            super();
        }
        public void onCancel() {
        }
        public void onComplete(JSONObject arg0) {
        }
        public void onError(UiError arg0) {
        }
    }

    private void updateUserInfo() {
        if (tencent != null && tencent.isSessionValid()) {
            IRequestListener requestListener = new IRequestListener() {
                public void onUnknowException(Exception e, Object state) {}
                public void onSocketTimeoutException(SocketTimeoutException e,Object state) {}
                public void onNetworkUnavailableException(NetworkUnavailableException e, Object state) {}
                public void onMalformedURLException(MalformedURLException e,Object state) {}
                public void onJSONException(JSONException e, Object state) {}
                public void onIOException(IOException e, Object state) {}
                public void onHttpStatusException(HttpStatusException e,Object state) {}
                public void onConnectTimeoutException(ConnectTimeoutException e, Object state) {}
                public void onComplete(final JSONObject response, Object state) {}
            };
            tencent.requestAsync(Constants.GRAPH_SIMPLE_USER_INFO, null,Constants.HTTP_GET, requestListener, null);
        }
    }
}
