/**
 *   Copyright (C) 2010,2013 Thundersoft Corporation
 *   All rights Reserved
 */
package com.ucamera.ucomm.sns;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import com.ucamera.ucomm.sns.services.ShareService;


public class AuthDialog extends Dialog {
    private static final String TAG = "AuthDialog";

    private static final LayoutParams MATCH_PARENT =
            new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

    private RequestListener mRequestListener;
    private String mUrl;

    private ProgressDialog mSpinner;
    private FrameLayout mContent;
    private WebView mWebView;
    private Context mContext;

    public AuthDialog(Context context, String url, RequestListener listener) {
        super(context, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        mContext = context;
        mUrl = url;
        mRequestListener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setUpWebView();
        this.setOnCancelListener(new OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                mRequestListener.onCancel();
            }
        });
    }

    private void setUpWebView() {
        mSpinner = new ProgressDialog(getContext());
        mSpinner.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mSpinner.setOnCancelListener(new OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                if (mWebView != null) {
                    mWebView.stopLoading();
                    AuthDialog.this.cancel();
                }
            }
        });

        mWebView = new WebView(getContext()) {
            @Override
            public void onWindowFocusChanged(boolean arg0) {
                /*
                 * BUG FIX: 663
                 * FIX COMMENT: see the following page for more infomation.
                 *  http://www.zubha-labs.com/workaround-for-null-pointer-excpetion-in-webv
                 * Date: 2012-08-16
                 */
                try{
                    super.onWindowFocusChanged(arg0);
                }catch (NullPointerException e) {
                    // EAT IT
                }
            }
        };
        mWebView.setVerticalScrollBarEnabled(false);
        mWebView.setHorizontalScrollBarEnabled(false);
        mWebView.setWebViewClient(new AuthDialog.AuthWebViewClient());
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.loadUrl(mUrl);
        mWebView.setLayoutParams(MATCH_PARENT);
        mWebView.setVisibility(View.INVISIBLE);
        mContent = new FrameLayout(getContext());
        mContent.addView(mWebView);

        setContentView(mContent, MATCH_PARENT);
    }

    private class AuthWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.d(TAG,"Accessing: " + url);
            // a little wired. some site need to be a valid url
            if (isCallbackUrl(url)) {
                Bundle params = Util.parseUrl(url);
                String error = params.getString("error");
                if (error == null) {
                    error = params.getString("error_type");
                }
                if (error == null) {
                    mRequestListener.onComplete(params);
                } else {
                    mRequestListener.onException(new RequestException(-1, url, error));
                }
                AuthDialog.this.dismiss();
                return true;
            } else if (url.startsWith("fbconnect://cancel")) {
                mRequestListener.onCancel();
                AuthDialog.this.dismiss();
                return true;
            } else if (url.contains("touch") ){
                return false;
            }
            return super.shouldOverrideUrlLoading(view, url);
        }

        private boolean isCallbackUrl(String url) {
            //sina weibo will return: http://android//callback.ucam?code=xxx
            final String sina_weibo = "//callback.ucam";
            return url.startsWith(ShareService.CALLBACK_URL)
                    || url.startsWith(ShareService.CALLBACK_URL_ALTERNATE)
                    || url.startsWith(ShareService.CALLBACK_URL_FACEBOOK)
                    || url.startsWith(ShareService.CALLBACK_URL_GOOGLE)
                    || url.contains(sina_weibo);
        }

        @Override
        public void onReceivedError(WebView view, int errorCode,
                String description, String failingUrl) {
            Log.w(TAG, String.format("Error[%d] %s, while retrive: %s", errorCode, description,
                    failingUrl));
            super.onReceivedError(view, errorCode, description, failingUrl);
            mRequestListener.onException(new RequestException(errorCode, failingUrl, description));
            AuthDialog.this.dismiss();
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            Log.d(TAG, "start url:" + url);
            mSpinner.show();
            mSpinner.setMessage(getContext().getString(R.string.sns_msg_loading));

            if(isNotSupportOverrideUrl()) {
                //call back url
                if(url.contains("//callback.ucam")) {
                    mWebView.stopLoading();
                    Bundle params = Util.parseUrl(url);
                    String error = params.getString("error");
                    if (error == null) {
                        error = params.getString("error_type");
                    }
                    if (error == null) {
                        mRequestListener.onComplete(params);
                    } else {
                        mRequestListener.onException(new RequestException(-1, url, error));
                    }
                    AuthDialog.this.dismiss();
                }
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            /*
             * FIX BUG: 6170
             * BUG COMMENT: View not attached to window manager
             * DATE: 2014-03-26
             */
            if(mContext instanceof Activity && ((Activity)mContext).isFinishing()) {
                return;
            }
            mSpinner.dismiss();
            mWebView.setVisibility(View.VISIBLE);
        }
    }
    private boolean isNotSupportOverrideUrl() {
        if(Build.VERSION.SDK_INT < 11 && mUrl.contains("https://api.weibo.com")) {
            return true;
        }
        return false;
    }
}
