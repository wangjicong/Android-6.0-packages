/**
 * Add for navigation tab
 *@{
 */

package com.android.browser;

import java.util.Map;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.FrameLayout;

public class DefaultHomeTab extends Tab {

    private Controller mController;
    private boolean mIsHome;
    private WebViewFactory mWebViewFactory;
    private static String LOGTAG = "DefaultHomeTab";

    DefaultHomeTab(Controller controller) {
        super(controller, null, null);
        mController = controller;
        mWebViewFactory = mController.getWebViewFactory();
        WebView web = mWebViewFactory.createWebView(false);
        setWebView(web);
        mIsHome = true;
        setHomePageState();
    }

    private void setHomePageState(){
        if(mCurrentState == null){
            mCurrentState = new PageState(mContext, false);
        }
        mCurrentState.mFavicon = null;
        mCurrentState.mIncognito = false;
        mCurrentState.mIsBookmarkedSite = false;
        mCurrentState.mOriginalUrl = mCurrentState.mUrl = Controller.HOME_URL;
        mCurrentState.mSecurityState = SecurityState.SECURITY_STATE_NOT_SECURE;
        mCurrentState.mSslCertificateError = null;
        mCurrentState.mTitle = mContext.getResources().getString(R.string.home_view_title);
    }

    @Override
    void putInForeground() {
        if (getWebView() == null) {
            WebView web = mWebViewFactory.createWebView(false);
            setWebView(web);
        }
        if(mIsHome){
            mController.getDefaultHomeView().putInForeground();
        }

        super.putInForeground();
    }

    @Override
    void putInBackground() {
        if(mIsHome){
            mController.getDefaultHomeView().putInBackground();
        }
        super.putInBackgroundNoCapture();
    }

    @Override
    public View getAttachedView(){
        if(mIsHome){
            return mController.getDefaultHomeView();
        }else{
            return super.getAttachedView();
        }
    }

    @Override
    public boolean canGoBack() {
        return super.canGoBack() || !mIsHome;
    }

    @Override
    public boolean canGoForward() {
        return super.canGoForward() && !mIsHome;
    }

    @Override
    public void goBack() {
        if(super.canGoBack()){
            super.goBack();
            return;
        }
        if(!mIsHome){
            mController.getUi().detachTab(this);
            final WebView web = getWebView();
            if(web != null){
                destroy();
                Log.d(LOGTAG, "goBack()    destroy");
            }
            resetSavedState();
            WebView newWeb = mWebViewFactory.createWebView(false);
            setWebView(newWeb);
            mIsHome = true;
            setHomePageState();
            newWeb.setOnCreateContextMenuListener(mWebViewController.getActivity());
            mController.setActiveTab(this);
            mController.goBacktoHomePage(this);
        }
    }

    @Override
    public void goForward() {
        super.goForward();
    }

    @Override
    protected void capture(){
        if(!mIsHome){
            super.capture();
        }else{
            mController.getDefaultHomeView().capture();
        }
    }

    public Bitmap getScreenshot() {
        if(mIsHome){
            return mController.getDefaultHomeView().getScreenshot();
        }else{
            return super.getScreenshot();
        }
    }

    @Override
    String getUrl() {
        if(mIsHome){
            return Controller.HOME_URL;
        }else{
            return super.getUrl();
        }
    }

    @Override
    String getOriginalUrl() {
        if(mIsHome){
            return Controller.HOME_URL;
        }else{
            return super.getOriginalUrl();
        }
    }
    @Override
    Bitmap getFavicon() {
        if (mIsHome) {
            mCurrentState.mFavicon = null;
        }
        return super.getFavicon();
    }

    @Override
    String getTitle() {
        if(mIsHome){
            return mContext.getResources().getString(R.string.home_view_title);
        }else{
            return super.getTitle();
        }
    }

    @Override
    public Bundle saveState() {
        if (!mIsHome) {
            return super.saveState();
        }
        return null;
    }

    public boolean isDefaultHome(){
        return mIsHome;
    }

    @Override
    public ContentValues createSnapshotValues() {
        if (!mIsHome) {
            return super.createSnapshotValues();
        }
        return null;
    }

    @Override
    public void loadUrl(String url, Map<String, String> headers) {
        if (mIsHome && url.equals(Controller.HOME_URL)) {
            //do nothing
            Log.d(LOGTAG,"no need to load browser:home from DefaultHomeTab");
            return;
        }
        if(!url.equals(Controller.HOME_URL)){
            Log.d(LOGTAG,"load: " + url);
//            mController.openUrlFromHome(url, headers);
            if(mIsHome){
                mController.getUi().detachTab(this);
                mIsHome = false;
                mController.setActiveTab(this);
            }
            super.loadUrl(url, headers);
        } else {
            mController.getUi().detachTab(this);
            final WebView web = getWebView();
            if(web != null){
                destroy();
                Log.d(LOGTAG, "go to homepage    destroy");
            }
            resetSavedState();
            WebView newWeb = mWebViewFactory.createWebView(false);
            setWebView(newWeb);
            mIsHome = true;
            setHomePageState();
            mController.setActiveTab(this);
        }
    }

    protected void postCapture() {
        //do nothing
        Log.d("LOGTAG","postCapture do nothing");
    }
}
