package com.sprd.ext.dynamicIcon;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.sprd.ext.LogUtils;
import com.sprd.ext.UtilitiesExt;
import com.sprd.ext.dynamicIcon.DynamicIconUtils.DynamicAppChangedCallbacks;

/**
 * Created by SPRD on 10/18/16.
 */
public abstract class DynamicIcon extends BroadcastReceiver {

    private static final String TAG = "DynamicIcon";

    protected Context mContext;
    protected ComponentName mComponent;
    protected boolean mIsChecked;

    private boolean mIsRegisted;
    private int mType;
    private DynamicAppChangedCallbacks mCallbacks;

    protected abstract void init();
    protected abstract boolean hasChanged();
    protected abstract IntentFilter getReceiverFilter();
    public abstract DynamicIconDrawCallback getDynamicIconDrawCallback();
    public abstract Drawable getStableBackground();
    protected abstract void draw(Canvas canvas, View icon, float scale, int[] center);

    public class DynamicIconDrawCallback {
        public void drawDynamicIcon(Canvas canvas, View icon, float scale, int[] center) {}
    }

    protected DynamicIconDrawCallback mDrawCallback = new DynamicIconDrawCallback() {
        @Override
        public void drawDynamicIcon(Canvas canvas, View icon, float scale, int[] center) {
                draw(canvas, icon, scale, center);
        }
    };

    public DynamicIcon(Context context, int type) {
        mContext = context;
        mType = type;
        init();
    }

    public void setDynamicIconDrawCallback(DynamicAppChangedCallbacks callbacks) {
        mCallbacks = callbacks;
    }

    public void setCheckedState(boolean isChecked) {
        mIsChecked = isChecked;
    }

    public void setComponentName(ComponentName component) {
        mComponent = component;
    }

    public ComponentName getComponentName() {
        return mComponent;
    }

    public CharSequence getAppLabel() {
        CharSequence ch = null;
        if (mComponent != null) {
            String pkgName = mComponent.getPackageName();
            ch = UtilitiesExt.getAppLabelByPackageName(mContext, pkgName);
        }
        return ch;
    }

    public int getType() {
        return mType;
    }

    public boolean isCheckedState() {
        return mIsChecked;
    }

    protected void forceUpdateView(boolean force) {
        if (force || hasChanged()) {
            if (mCallbacks != null) {
                mCallbacks.bindComponentDynamicIconChanged(mComponent);
            }
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (mIsChecked) {
            if (LogUtils.DEBUG_DYNAMIC_ICON) {
                LogUtils.d(TAG, "Receive broadcast: " + action + ", this = " + this);
            }
            forceUpdateView(false);
        }
    }

    public boolean registerReceiver() {
        if (!mIsRegisted) {
            IntentFilter filter = getReceiverFilter();
            mContext.registerReceiver(this, filter);
            mIsRegisted = true;
            return true;
        }
        return false;
    }

    public boolean unRegisterReceiver() {
        if (mIsRegisted) {
            mContext.unregisterReceiver(this);
            mIsRegisted = false;
            return true;
        }
        return false;
    }
}
