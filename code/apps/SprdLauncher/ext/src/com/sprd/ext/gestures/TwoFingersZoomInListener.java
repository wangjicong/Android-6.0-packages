package com.sprd.ext.gestures;

import android.view.HapticFeedbackConstants;

import com.android.sprdlauncher3.Launcher;
import com.sprd.ext.gestures.LauncherRootViewGestures.Gesture;
/**
 * Created by SPREADTRUM on 17-4-5.
 */

public class TwoFingersZoomInListener implements LauncherRootViewGestures.OnGestureListener {

    private Launcher mLauncher;

    public TwoFingersZoomInListener(Launcher mLauncher) {
        this.mLauncher = mLauncher;
    }

    @Override
    public boolean onGesture(Gesture gesture) {
        if(gesture == Gesture.TWO_FINGER_ZOOM_IN){
            if (!mLauncher.getWorkspace().isInOverviewMode()) {
                if (mLauncher.getWorkspace().enterOverviewMode()) {
                    mLauncher.getWorkspace().performHapticFeedback(HapticFeedbackConstants.LONG_PRESS,
                            HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
                    return true;
                }
            }
        }
        return false;
    }
}
