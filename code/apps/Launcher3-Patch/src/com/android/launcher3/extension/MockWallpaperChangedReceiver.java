package com.android.launcher3.extension;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.android.launcher3.extension.wrapper.BaseWallpaperChangedReceiver;

public class MockWallpaperChangedReceiver extends BaseWallpaperChangedReceiver {
    @Override
    public void onReceive(Context context, Intent data) {
        Log.e("xxx"," MockWallpaperChangedReceiver inner Launcher");
        super.onReceive(context, data);
    }
}
