package com.android.launcher3.extension.wrapper;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;

import com.android.launcher3.AppInfo;
import com.android.launcher3.InsettableFrameLayout;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherCallbacks;
import com.android.launcher3.allapps.AllAppsSearchBarController;
import com.android.launcher3.bridge.ExtensionCallbacks;
import com.android.launcher3.bridge.ExtensionOverlay;
import com.android.launcher3.bridge.ExtensionOverlayCallbacks;
import com.android.launcher3.util.ComponentKey;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class LauncherCallbacksWrapper implements LauncherCallbacks {

    private ExtensionCallbacks mCallbacks;
    private ExtensionOverlayCallbacksImpl mExtensionOverlayCallbacksImpl;
    private LauncherOverlayImpl mLauncherOverlayImpl;

    public LauncherCallbacksWrapper(ExtensionCallbacks callbacks) {
        mCallbacks = callbacks;
    }

    @Override
    public void preOnCreate() {
        mCallbacks.preOnCreate();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mCallbacks.onCreate(savedInstanceState);
    }

    @Override
    public void preOnResume() {
        mCallbacks.preOnResume();
    }

    @Override
    public void onResume() {
        mCallbacks.onResume();
    }

    @Override
    public void onStart() {
        mCallbacks.onStart();
    }

    @Override
    public void onStop() {
        mCallbacks.onStop();
    }

    @Override
    public void onPause() {
        mCallbacks.onPause();
    }

    @Override
    public void onDestroy() {
        mCallbacks.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        mCallbacks.onSaveInstanceState(outState);
    }

    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        mCallbacks.onPostCreate(savedInstanceState);
    }

    @Override
    public void onNewIntent(Intent intent) {
        mCallbacks.onNewIntent(intent);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mCallbacks.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        mCallbacks.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        mCallbacks.onWindowFocusChanged(hasFocus);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return mCallbacks.onPrepareOptionsMenu(menu);
    }

    @Override
    public void dump(String prefix, FileDescriptor fd, PrintWriter w, String[] args) {
        mCallbacks.dump(prefix, fd, w, args);
    }

    @Override
    public void onHomeIntent() {
        mCallbacks.onHomeIntent();
    }

    @Override
    public boolean handleBackPressed() {
        return mCallbacks.handleBackPressed();
    }

    @Override
    public void onTrimMemory(int level) {
        mCallbacks.onTrimMemory(level);
    }

    @Override
    public void onLauncherProviderChange() {
        mCallbacks.onLauncherProviderChange();
    }

    @Override
    public void finishBindingItems(boolean upgradePath) {
        mCallbacks.finishBindingItems(upgradePath);
    }

    @Override
    public void onClickAllAppsButton(View v) {
        mCallbacks.onClickAllAppsButton(v);
    }

    @Override
    public void bindAllApplications(ArrayList<AppInfo> apps) {
        mCallbacks.bindAllApplications(apps);
    }

    @Override
    public void onClickFolderIcon(View v) {
        mCallbacks.onClickFolderIcon(v);
    }

    @Override
    public void onClickAppShortcut(View v) {
        mCallbacks.onClickAppShortcut(v);
    }

    @Override
    public void onClickPagedViewIcon(View v) {
        mCallbacks.onClickPagedViewIcon(v);
    }

    @Override
    public void onClickWallpaperPicker(View v) {
        mCallbacks.onClickWallpaperPicker(v);
    }

    @Override
    public void onClickSettingsButton(View v) {
        mCallbacks.onClickSettingsButton(v);
    }

    @Override
    public void onClickAddWidgetButton(View v) {
        mCallbacks.onClickAddWidgetButton(v);
    }

    @Override
    public void onPageSwitch(View newPage, int newPageIndex) {
        mCallbacks.onPageSwitch(newPage, newPageIndex);
    }

    @Override
    public void onWorkspaceLockedChanged() {
        mCallbacks.onWorkspaceLockedChanged();
    }

    @Override
    public void onDragStarted(View view) {
        mCallbacks.onDragStarted(view);
    }

    @Override
    public void onInteractionBegin() {
        mCallbacks.onInteractionBegin();
    }

    @Override
    public void onInteractionEnd() {
        mCallbacks.onInteractionEnd();
    }

    @Override
    public boolean forceDisableVoiceButtonProxy() {
        return mCallbacks.forceDisableVoiceButtonProxy();
    }

    @Override
    public boolean providesSearch() {
        return mCallbacks.providesSearch();
    }

    @Override
    public boolean startSearch(String initialQuery, boolean selectInitialQuery,
                               Bundle appSearchData, Rect sourceBounds) {
        return mCallbacks.startSearch(initialQuery, selectInitialQuery, appSearchData, sourceBounds);
    }

    @Override
    public void startVoice() {
        mCallbacks.startVoice();
    }

    @Override
    public boolean hasCustomContentToLeft() {
        return mCallbacks.hasCustomContentToLeft();
    }

    @Override
    public void populateCustomContentContainer() {
        mCallbacks.populateCustomContentContainer();
    }

    @Override
    public View getQsbBar() {
        return mCallbacks.getQsbBar();
    }

    @Override
    public Intent getFirstRunActivity() {
        return mCallbacks.getFirstRunActivity();
    }

    @Override
    public boolean hasFirstRunActivity() {
        return mCallbacks.hasFirstRunActivity();
    }

    @Override
    public boolean hasDismissableIntroScreen() {
        return mCallbacks.hasDismissableIntroScreen();
    }

    @Override
    public View getIntroScreen() {
        return mCallbacks.getIntroScreen();
    }

    @Override
    public boolean shouldMoveToDefaultScreenOnHomeIntent() {
        return mCallbacks.shouldMoveToDefaultScreenOnHomeIntent();
    }

    @Override
    public boolean hasSettings() {
        return mCallbacks.hasSettings();
    }

    public ComponentName getWallpaperPickerComponent() {
        return mCallbacks.getWallpaperPickerComponent();
    }

    @Override
    public boolean overrideWallpaperDimensions() {
        return mCallbacks.overrideWallpaperDimensions();
    }

    @Override
    public boolean isLauncherPreinstalled() {
        return mCallbacks.isLauncherPreinstalled();
    }

    @Override
    public AllAppsSearchBarController getAllAppsSearchBarController() {
        return null;
    }

    @Override
    public List<ComponentKey> getPredictedApps() {
        return null;
    }

    @Override
    public boolean hasLauncherOverlay() {
        return mCallbacks.hasLauncherOverlay();
    }

    @Override
    public Launcher.LauncherOverlay setLauncherOverlayView(InsettableFrameLayout container,
                                                           final Launcher.LauncherOverlayCallbacks overlayCallbacks) {
        if (mExtensionOverlayCallbacksImpl == null){
            mExtensionOverlayCallbacksImpl = new ExtensionOverlayCallbacksImpl();
        }
        if (mLauncherOverlayImpl == null){
            mLauncherOverlayImpl = new LauncherOverlayImpl();
        }
        mExtensionOverlayCallbacksImpl.setOverlayCallbacks(overlayCallbacks);
        ExtensionOverlay extOverlay = mCallbacks.setLauncherOverlayView(container,
                mExtensionOverlayCallbacksImpl);
        mLauncherOverlayImpl.setExtOverlay(extOverlay);
        return mLauncherOverlayImpl;
    }

    public boolean startSearchFromAllApps(String query){
        return false;
    }

    @Override
    public void setLauncherSearchCallback(Object callbacks) {
        mCallbacks.setLauncherSearchCallback(callbacks);
    }

    class LauncherOverlayImpl implements Launcher.LauncherOverlay{
        ExtensionOverlay extOverlay;

        public void setExtOverlay(ExtensionOverlay overlay){
            extOverlay = overlay;
        }

        @Override
        public void onScrollInteractionBegin() {
            extOverlay.onScrollInteractionBegin();
        }

        @Override
        public void onScrollInteractionEnd() {
            extOverlay.onScrollInteractionBegin();
        }

        @Override
        public void onScrollChange(int i, boolean b) {
            extOverlay.onScrollChange(i, b);
        }

        @Override
        public void onScrollSettled() {
            extOverlay.onScrollSettled();
        }

        @Override
        public void forceExitFullImmersion() {
            extOverlay.forceExitFullImmersion();
        }
    }

    class ExtensionOverlayCallbacksImpl implements ExtensionOverlayCallbacks {
        Launcher.LauncherOverlayCallbacks overlayCallbacks;

        public void setOverlayCallbacks(Launcher.LauncherOverlayCallbacks callbacks){
            overlayCallbacks = callbacks;
        }

        @Override
        public boolean canEnterFullImmersion() {
            return overlayCallbacks.canEnterFullImmersion();
        }

        @Override
        public boolean enterFullImmersion() {
            return overlayCallbacks.enterFullImmersion();
        }

        @Override
        public void exitFullImmersion() {
            overlayCallbacks.exitFullImmersion();
        }
    }

}
