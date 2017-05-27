package com.android.launcher3.extension;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import com.android.launcher3.extension.wrapper.BaseLauncher;

public class MockLauncher extends BaseLauncher {

    public static final String TAG = "BaseLauncher";

    public boolean onTouchCellLayout(View v, MotionEvent ev) {
        super.onTouchCellLayout(v, ev);
        Log.w(TAG, " Calling 'onTouchCellLayout' in BaseLauncher ");
        return false;
    }

}
