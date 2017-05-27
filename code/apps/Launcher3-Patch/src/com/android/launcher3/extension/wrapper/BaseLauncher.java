package com.android.launcher3.extension.wrapper;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.android.launcher3.AppInfo;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.bridge.BridgeVersion;
import com.android.launcher3.bridge.ExtensionCallbacks;
import com.android.launcher3.compat.UserHandleCompat;

import java.util.ArrayList;

public class BaseLauncher extends Launcher{
    public static final String TAG = "BaseLauncher";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @TargetApi(BridgeVersion.VERSION_B)
    public void setExtensionCallbacks(ExtensionCallbacks callbacks){
        setLauncherCallbacks(new LauncherCallbacksWrapper(callbacks));

    }

    @TargetApi(BridgeVersion.VERSION_B)
    public boolean onTouchCellLayout(View v, MotionEvent ev) {
        Log.w(TAG, " Calling 'onTouchCellLayout' in BaseLauncher ");
        return false;
    }

    @TargetApi(BridgeVersion.VERSION_B)
    public DeviceProfile getDeviceProfile(){
        return super.getDeviceProfile();
    }

    @TargetApi(BridgeVersion.VERSION_B)
    public void addPopMenuToView(View v){
        Log.w(TAG, " Calling 'addPopMenuToView' in BaseLauncher ");
    }

    @TargetApi(BridgeVersion.VERSION_B)
    protected void addToAllApps(ArrayList<AppInfo> apps){
        Log.w(TAG, " Calling 'addToAllApps' in BaseLauncher ");
    }

    @TargetApi(BridgeVersion.VERSION_B_a)
    protected void addShortcuts(ArrayList<ItemInfo> shortcuts) {
        Log.w(TAG, " Calling 'addShortcuts' in BaseLauncher ");
    }

    @TargetApi(BridgeVersion.VERSION_B)
    public static Bitmap getIconByInfo(ItemInfo itemInfo){
        if (itemInfo instanceof ShortcutInfo){
            return ((ShortcutInfo) itemInfo).getIcon(LauncherAppState.getInstance().getIconCache());
        }
        if(itemInfo instanceof AppInfo){
            LauncherAppState.getInstance().getIconCache().getTitleAndIcon((AppInfo) itemInfo, null, false);
            return ((AppInfo) itemInfo).iconBitmap;
        }
        return null;
    }

    @Override
    public View createShortcut(ViewGroup parent, ItemInfo info) {
        View favorite = super.createShortcut(parent, info);
        addPopMenuToView(favorite);
        return favorite;
    }

    @Override
    public void bindAppsAdded(ArrayList<Long> newScreens, ArrayList<ItemInfo> addNotAnimated, ArrayList<ItemInfo> addAnimated, ArrayList<AppInfo> addedApps) {
        super.bindAppsAdded(newScreens, addNotAnimated, addAnimated, addedApps);
        addToAllApps(addedApps);
    }

    @Override
    public void bindAllApplications(ArrayList<AppInfo> apps) {
        super.bindAllApplications(apps);
        addToAllApps(apps);
    }

    @Override
    public void bindAppsUpdated(ArrayList<AppInfo> apps) {
        super.bindAppsUpdated(apps);
        addToAllApps(apps);
    }

    @Override
    public void bindItems(ArrayList<ItemInfo> shortcuts, int start, int end, boolean forceAnimateIcons) {
        super.bindItems(shortcuts, start, end, forceAnimateIcons);
        addShortcuts(shortcuts);
    }

    @Override
    public void bindShortcutsChanged(ArrayList<ShortcutInfo> updated, ArrayList<ShortcutInfo> removed, UserHandleCompat user) {
        super.bindShortcutsChanged(updated, removed, user);
    }

}
