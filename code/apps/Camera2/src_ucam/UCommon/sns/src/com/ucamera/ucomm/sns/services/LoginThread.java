/*
 * Copyright (C) 2014,2015 Thundersoft Corporation
 * All rights Reserved
 */
package com.ucamera.ucomm.sns.services;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.scribe.model.Token;

import com.ucamera.ucomm.sns.R;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;

public class LoginThread {
    private static final int LOGIN_END = 200;
    private AbstractService mService;
    private Context mContext;
    private LoginListener mListener;
    private ProgressDialog mSpinner;
    private Token mToken;
    private boolean mCancel = false;
    private ExecutorService executorService;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case LOGIN_END:
                /*
                 * FIX BUG: 6180
                 * BUG COMMENT: View not attached to window manager
                 * DATE: 2014-03-28
                 */
                if(mContext instanceof Activity && !((Activity)mContext).isFinishing()) {
                    mSpinner.dismiss();
                }
                if (mToken == null) {
                    mListener.onFail("Fail to fetch request token.");
                } else {
                    mService.showAuthDialog(mContext, mToken, mListener);
                }
                break;
            default:
                break;
            }
        }
    };
    public LoginThread(AbstractService service, Context context, LoginListener listener) {
        this.mService = service;
        this.mContext = context;
        this.mListener = listener;
        executorService = Executors.newFixedThreadPool(1);
    }
    public void logIn(){
        initShowDialog();
        executorService.submit(new OAuthRunnable());
    }
    private void initShowDialog() {
        mSpinner = new ProgressDialog(mContext);
        mSpinner.setMessage(mContext.getString(R.string.sns_msg_connecting));
        mSpinner.show();
        mSpinner.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                mCancel = true;
                mListener.onCancel();
                executorService.shutdownNow();
            }
        });
    }
    class OAuthRunnable implements Runnable {
        public void run() {
            if(mCancel) return;
            mToken = mService.prepareRequestToken();
            if(mCancel) return;
            mHandler.sendEmptyMessage(LOGIN_END);
        }
    }
}
