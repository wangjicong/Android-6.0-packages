package com.android.sprdlauncher3;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.sprd.ext.FeatureOption;
import com.sprd.ext.gestures.LauncherRootViewGestures;
import com.sprd.ext.gestures.TwoFingersZoomInListener;

public class LauncherRootView extends InsettableFrameLayout {

    private LauncherRootViewGestures mGestures;

    public LauncherRootView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if(FeatureOption.SPRD_FINGER_GESTURE_SUPPORT) {
            mGestures = new LauncherRootViewGestures((Launcher) context);
            //SPRD Add for two-finger scale reduction into the OverView Mode
            if(FeatureOption.SPRD_FINGER_SCALE_TO_EDIT_SUPPORT) {
                mGestures.registerOnGestureListener(new TwoFingersZoomInListener((Launcher) context));
            }
        }
    }

    @Override
    protected boolean fitSystemWindows(Rect insets) {
        setInsets(insets);
        return true; // I'll take it from here
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if(mGestures != null) {
            return mGestures.onTouchEvent(ev);
        }

        return super.onInterceptTouchEvent(ev);
    }
}