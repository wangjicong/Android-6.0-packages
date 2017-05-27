package com.sprd.ext.dynamicIcon;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.view.View;

import com.android.sprdlauncher3.AppInfo;
import com.android.sprdlauncher3.AppsCustomizeCellLayout;
import com.android.sprdlauncher3.AppsCustomizePagedView;
import com.android.sprdlauncher3.BubbleTextView;
import com.android.sprdlauncher3.CellLayout;
import com.android.sprdlauncher3.Folder;
import com.android.sprdlauncher3.FolderIcon;
import com.android.sprdlauncher3.FolderInfo;
import com.android.sprdlauncher3.ItemInfo;
import com.android.sprdlauncher3.Launcher;
import com.android.sprdlauncher3.R;
import com.android.sprdlauncher3.ShortcutAndWidgetContainer;
import com.android.sprdlauncher3.ShortcutInfo;
import com.android.sprdlauncher3.Workspace;
import com.sprd.ext.FeatureOption;
import com.sprd.ext.LogUtils;
import com.sprd.ext.UtilitiesExt;
import com.sprd.ext.dynamicIcon.DynamicIcon.DynamicIconDrawCallback;
import com.sprd.ext.dynamicIcon.calendar.DynamicCalendar;
import com.sprd.ext.dynamicIcon.calendar.OriginalDynamicCalendar;
import com.sprd.ext.dynamicIcon.deskclock.DynamicDeskclock;
import com.sprd.ext.dynamicIcon.deskclock.OriginalDynamicDeskclock;

import java.util.ArrayList;

/**
 * Created by SPRD on 10/19/16.
 */
public class DynamicIconUtils {

    private static final String TAG = "DynamicIconUtils";

    public static final float STABLE_SCALE = 1.0f;

    public static final int DYNAMIC_CALENDAR_TYPE = 1001;
    public static final int DYNAMIC_CLOCK_TYPE = 1002;
    public static final ComponentName sCalendarComponentName =
            new ComponentName("com.android.calendar", "com.android.calendar.AllInOneActivity");
    public static final ComponentName sDeskClockComponentName =
            new ComponentName("com.android.deskclock", "com.android.deskclock.DeskClock");

    // Ture if the dynamic icon is the customize icon from sprd,
    // otherwise, it is similar to the AOSP icon.
    private  static final boolean CUSTOMIZE_DYNAMIC_ICON =
            SystemProperties.getBoolean("ro.launcher.dyicon.customize", true);
    private static final String GOOGLE = "google";
    private volatile static DynamicIconUtils INSTANCE;
    private static final int INVALID_NUM = -1;
    private ArrayList<DynamicIcon> DYNAMIC_INFOS = new ArrayList<>();

    private Launcher mLauncher;
    private Workspace mWorkspace;
    private AppsCustomizePagedView mAppsCustomizeContent;
    private ComponentName mDynamicCalCn;
    private ComponentName mDynamicClockCn;
    private Context mContext;

    private final Object mLock = new Object();

    private boolean mResumed;
    private boolean mNeedRegister;

    public DynamicIconUtils(Context context) {
        mContext = context;
        if (FeatureOption.SPRD_DYNAMIC_CALENDAR_SUPPORT) {
            mDynamicCalCn = getDynamicIconCnByType(DYNAMIC_CALENDAR_TYPE);
        }
        if (FeatureOption.SPRD_DYNAMIC_CLOCK_SUPPORT) {
            mDynamicClockCn = getDynamicIconCnByType(DYNAMIC_CLOCK_TYPE);
        }
        loadDynamicIconInfo();
    }

    public static DynamicIconUtils getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized(DynamicIconUtils.class){
                if (INSTANCE == null) {
                    INSTANCE = new DynamicIconUtils(context);
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Set this as the current Launcher activity object for the loader.
     */
    public void initialize(Launcher launcher) {
        synchronized (mLock) {
            if (launcher != null) {
                if (mLauncher != null) {
                    LogUtils.w(TAG, "initialize called twice! old=" + mLauncher + " new=" + launcher);
                }

                //step1: init object
                mLauncher = launcher;
                mWorkspace = launcher.getWorkspace();
                mAppsCustomizeContent = launcher.getAppsCustomizeContent();
                mResumed = true;

                //step2: bind dynamic icon
                initDynamicIconDrawCallback();
                updateDynamicIconRegisterState();
                if (mLauncher != null) {
                    mLauncher.bindDynamicIconIfNeeded();
                }
            }
        }
    }

    private void initDynamicIconDrawCallback() {
        for (int i = 0; i < DYNAMIC_INFOS.size(); i++) {
            DynamicIcon dynamicIcon = DYNAMIC_INFOS.get(i);
            if (mLauncher != null) {
                dynamicIcon.setDynamicIconDrawCallback(mLauncher);
            }
        }
    }

    /**
     * Reset all objects when onDestroy is called in the launcher activity.
     */
    public void destroy() {

        mLauncher = null;
        mWorkspace = null;
        mAppsCustomizeContent = null;

        mResumed = false;
        mNeedRegister = false;

        resetDynamicIconInfo(false);
    }

    /**
     * Get the stable part of the dynamic icon for the ComponentName
     */
    public Drawable getStableBGForComponent(ComponentName componentName) {
        return getStableBGAt(supportDynamicIcon(componentName));
    }

    /**
     * Get DynamicIconDrawCallback according to componentName
     */
    public DynamicIconDrawCallback getDIDCForComponent(ComponentName componentName) {
        return getDynamicIconCallbackAt(supportDynamicIcon(componentName));
    }

    public DynamicIcon getDynamicIconByType(int type) {
        for (int i = 0; i < DYNAMIC_INFOS.size(); i ++) {
            DynamicIcon icon = DYNAMIC_INFOS.get(i);
            if (icon.getType() == type) {
                return icon;
            }
        }
        return null;
    }

    private ComponentName getDynamicIconCnByType(int type) {
        ComponentName cn = getComponentByType(type);

        boolean isInstalled = UtilitiesExt.isAppInstalled(mContext, cn);
        if (!isInstalled) {
            cn = getCustomItemComponentName(type);
            isInstalled = UtilitiesExt.isAppInstalled(mContext, cn);
        }

        if (!isInstalled) {
            LogUtils.d(TAG, "The app (" + cn
                + ") is not installed, so making the variable to be null");
            cn = null;
        }

        if (LogUtils.DEBUG_DYNAMIC_ICON) {
            LogUtils.d(TAG, "getDynamicIconCnByType: type = " + type + ", cn = " + cn);
        }

        return cn;
    }

    private ComponentName getCustomItemComponentName(int type) {
        String componentName = "";
        switch (type) {
            case DYNAMIC_CALENDAR_TYPE:
                componentName = mContext.getResources().getString(R.string.custom_dynamic_calendar);
                break;
            case DYNAMIC_CLOCK_TYPE:
                componentName = mContext.getResources().getString(R.string.custom_dynamic_clock);
                break;
        }
        return ComponentName.unflattenFromString(componentName);
    }

    private ComponentName getComponentByType(int type) {
        ComponentName cn = null;
        switch (type) {
            case DYNAMIC_CALENDAR_TYPE:
                cn = sCalendarComponentName;
                break;
            case DYNAMIC_CLOCK_TYPE:
                cn = sDeskClockComponentName;
                break;
        }
        return cn;
    }

    private int supportDynamicIcon(ComponentName componentName) {
        if (!FeatureOption.SPRD_DYNAMIC_ICON_SUPPORT) {
            return INVALID_NUM;
        }

        final int size = DYNAMIC_INFOS.size();
        for(int i = 0; i < size; i++) {
            if (DYNAMIC_INFOS.get(i).getComponentName().equals(componentName)) {
                return i;
            }
        }

        return INVALID_NUM;
    }

    private DynamicIconDrawCallback getDynamicIconCallbackAt(int index) {
        if (index < 0 || index >= DYNAMIC_INFOS.size()) {
            return null;
        }

        DynamicIcon dynamicIcon = DYNAMIC_INFOS.get(index);
        DynamicIconDrawCallback drawCallback = dynamicIcon == null ?
                null : dynamicIcon.getDynamicIconDrawCallback();
        if (LogUtils.DEBUG_DYNAMIC_ICON) {
            LogUtils.d(TAG, "getDynamicIconCallbackAt: index = " + index
                    + ", component = " + DYNAMIC_INFOS.get(index).getComponentName()
                    + ", drawCallback = " + drawCallback);
        }
        return drawCallback;
    }

    private Drawable getStableBGAt(int index) {
        if (index < 0 || index >= DYNAMIC_INFOS.size()) {
            return null;
        }

        if (LogUtils.DEBUG_DYNAMIC_ICON) {
            LogUtils.d(TAG, "getStableBGAt: index = " + index
                    + ", component = " + DYNAMIC_INFOS.get(index).getComponentName());
        }
        return DYNAMIC_INFOS.get(index).getStableBackground();
    }

    /**
     * Load and initialize dynamic icon.
     */
    private void loadDynamicIconInfo() {
        if (LogUtils.DEBUG_DYNAMIC_ICON) {
            LogUtils.d(TAG, "start to load the dynamic icon info");
        }
        resetDynamicIconInfo(true);

        if (FeatureOption.SPRD_DYNAMIC_CALENDAR_SUPPORT && mDynamicCalCn != null) {
            DynamicIcon dynamicCal;
            if (CUSTOMIZE_DYNAMIC_ICON && !isGoogleApp(mDynamicCalCn)) {
                dynamicCal = new DynamicCalendar(mContext, DYNAMIC_CALENDAR_TYPE);
            } else {
                dynamicCal = new OriginalDynamicCalendar(mContext, DYNAMIC_CALENDAR_TYPE);
            }
            dynamicCal.setComponentName(mDynamicCalCn);
            DYNAMIC_INFOS.add(dynamicCal);
        }

        if (FeatureOption.SPRD_DYNAMIC_CLOCK_SUPPORT && mDynamicClockCn != null) {
            DynamicIcon dynamicClock;
            if (CUSTOMIZE_DYNAMIC_ICON && !isGoogleApp(mDynamicClockCn)) {
                dynamicClock = new DynamicDeskclock(mContext, DYNAMIC_CLOCK_TYPE);
            } else {
                dynamicClock = new OriginalDynamicDeskclock(mContext, DYNAMIC_CLOCK_TYPE);
            }
            dynamicClock.setComponentName(mDynamicClockCn);
            DYNAMIC_INFOS.add(dynamicClock);
        }

        if (LogUtils.DEBUG_DYNAMIC_ICON) {
            LogUtils.d(TAG, "load complete: DYNAMIC_INFOS = " + DYNAMIC_INFOS.toString());
        }
    }

    private void resetDynamicIconInfo(boolean isClear) {
        for (int i = 0; i < DYNAMIC_INFOS.size(); i++) {
            DynamicIcon dynamicIcon = DYNAMIC_INFOS.get(i);
            dynamicIcon.setDynamicIconDrawCallback(null);
            dynamicIcon.unRegisterReceiver();
        }
        if (isClear) {
            DYNAMIC_INFOS.clear();
        }
    }

    private boolean isGoogleApp(ComponentName cn) {
        if (cn != null) {
            String pkgName = cn.getPackageName();
            if (!TextUtils.isEmpty(pkgName)
                    && pkgName.contains(GOOGLE)) {
                return true;
            }
        }
        return false;
    }

    private void registerReceiver() {
        for (int i = 0; i < DYNAMIC_INFOS.size(); i++) {
            DynamicIcon dynamicIcon = DYNAMIC_INFOS.get(i);
            if (dynamicIcon.isCheckedState()) {
                dynamicIcon.registerReceiver();
            }
        }
    }

    private void unRegisterReceiver() {
        for (int i = 0; i < DYNAMIC_INFOS.size(); i++) {
            DynamicIcon dynamicIcon = DYNAMIC_INFOS.get(i);
            dynamicIcon.unRegisterReceiver();
        }
    }

    public void syncResumeState(boolean resumed) {
        mResumed = resumed;
        updateDynamicIconRegisterState();
    }


    private void updateDynamicIconRegisterState() {
        if (LogUtils.DEBUG_DYNAMIC_ICON) {
            LogUtils.d(TAG, "updateDynamicIconRegisterState, mResumed:mNeedRegister->"
                    + mResumed + ":" + mNeedRegister);
        }
        if (mResumed != mNeedRegister) {
            mNeedRegister = mResumed;
            if (mNeedRegister) {
                registerReceiver();
            } else {
                unRegisterReceiver();
            }
        }
    }

    public void updateComponentDynamicIconChanged(final ComponentName component) {
        updateWorkspaceDynamicIconChanged(component);
        if (mLauncher.isAllAppsVisible()) {
            updateAppsDynamicIconChanged(component);
        }
    }

    /**
     * SPRD: Update dynamic icon of shortcuts and folders in workspace and hotseat
     * with the given component.
     */
    private void updateWorkspaceDynamicIconChanged(final ComponentName component) {
        if (mWorkspace == null) {
            return;
        }
        final ArrayList<ShortcutAndWidgetContainer> childrenLayouts =
                mWorkspace.getAllShortcutAndWidgetContainers();
        int childCount;
        View view;
        Object tag;
        for (ShortcutAndWidgetContainer layout : childrenLayouts) {
            childCount = layout.getChildCount();
            for (int j = 0; j < childCount; j++) {
                view = layout.getChildAt(j);

                if (view != null) {
                    tag = view.getTag();
                } else {
                    if (LogUtils.DEBUG_DYNAMIC_ICON) {
                        LogUtils.d(TAG, "updateWorkspaceDynamicIconChanged: view is null pointer");
                    }
                    continue;
                }

                if (tag instanceof ShortcutInfo) {
                    final ShortcutInfo info = (ShortcutInfo) tag;
                    final Intent intent = info.getIntent();
                    final ComponentName componentName = intent.getComponent();
                    if (componentName != null && componentName.equals(component)) {
                        if (LogUtils.DEBUG_DYNAMIC_ICON_ALL) {
                            LogUtils.d(TAG, "updateWorkspaceDynamicIconChanged: component = " + component
                                    + ", cellX = " + info.cellX + ", cellY = " + info.cellY
                                    + ", screenId = " + info.screenId);
                        }
                        view.invalidate();
                    }
                }
            }
        }

        /// SPRD: Update shortcut within folder if open folder exists.
        Folder openFolder = mWorkspace.getOpenFolder();
        updateFolderContentDynamicIcon(openFolder, component, false);
    }

    /**
     * SPRD: Update dynamic icon of the content shortcut.
     */
    private boolean updateFolderContentDynamicIcon(Folder folder, ComponentName component, boolean init) {
        boolean hasDynamicIcon = false;
        if (folder == null) {
            return false;
        }

        CellLayout cellLayout = folder.getContent();
        final ShortcutAndWidgetContainer container = cellLayout.getShortcutsAndWidgets();
        int childCount = container.getChildCount();
        View view;
        Object tag;

        for (int j = 0; j < childCount; j++) {
            view = container.getChildAt(j);
            tag = view.getTag();

            if (tag instanceof ShortcutInfo) {
                final ShortcutInfo info = (ShortcutInfo) tag;
                final Intent intent = info.getIntent();
                final ComponentName componentName = intent.getComponent();

                if (init) {
                    info.dynamicIconDrawCallback = getDIDCForComponent(componentName);
                    if (info.dynamicIconDrawCallback != null) {
                        if (LogUtils.DEBUG_DYNAMIC_ICON) {
                            LogUtils.d(TAG, "updateFolderContentDynamicIcon:find componentName = " + componentName
                                    + ", info.dynamicIconDrawCallback = " + info.dynamicIconDrawCallback
                                    + ", cellX = " + info.cellX + ", cellY = " + info.cellY
                                    + ", screenId = " + info.screenId);
                        }
                        hasDynamicIcon = true;
                    }
                    } else if (componentName != null && componentName.equals(component)) {
                    view.invalidate();
                    if (LogUtils.DEBUG_DYNAMIC_ICON_ALL) {
                        LogUtils.d(TAG, "updateFolderContentDynamicIcon:find componentName = " + componentName
                                + ", cellX = " + info.cellX + ", cellY = " + info.cellY
                                + ", screenId = " + info.screenId);
                    }
                }
            }
        }
        return hasDynamicIcon;
    }

    /**
     * SPRD: Update dynamic icon of the given component in app customize paged view,
     * first find the icon, and then update the icon.
     */
    private void updateAppsDynamicIconChanged(final ComponentName component) {
        if (mAppsCustomizeContent == null) {
            return;
        }

        int numAppsPages = mAppsCustomizeContent.getNumAppPages();
        for (int i = 0; i < numAppsPages; i++) {
            View view = mAppsCustomizeContent.getPageAt(i);
            if(view != null && view instanceof AppsCustomizeCellLayout) {
                AppsCustomizeCellLayout cl = (AppsCustomizeCellLayout)view;
                ShortcutAndWidgetContainer container = cl.getShortcutsAndWidgets();
                if(container == null){
                    LogUtils.d(TAG, "updateAppsDynamicIconChanged: container == null");
                    continue;
                }
                final int count = container.getChildCount();

                BubbleTextView appIcon;
                AppInfo appInfo;
                int j = 0;
                for (; j < count; j++) {
                    appIcon = (BubbleTextView) container.getChildAt(j);
                    if (appIcon == null) {
                        continue;
                    }
                    appInfo = (AppInfo) appIcon.getTag();
                    if (appInfo != null && appInfo.componentName.equals(component)) {
                        if (LogUtils.DEBUG_DYNAMIC_ICON_ALL) {
                            LogUtils.d(TAG, "updateAppsDynamicIconChanged: component = " + component);
                        }
                        appIcon.invalidate();
                        break;
                    }
                }
                if (j < count) {
                    break;
                }
            }
        }
    }

    /**
     * SPRD: Update dynamic icon  of shortcuts in workspace and hotseat.
     */
    public void updateShortcutsAndFoldersDynamicIcon() {
        if (mWorkspace == null) {
            return;
        }

        final ArrayList<ShortcutAndWidgetContainer> childrenLayouts =
                mWorkspace.getAllShortcutAndWidgetContainers();
        int childCount;
        View view;
        Object tag;
        for (ShortcutAndWidgetContainer layout : childrenLayouts) {
            childCount = layout.getChildCount();
            for (int j = 0; j < childCount; j++) {
                view = layout.getChildAt(j);
                tag = view.getTag();

                if (tag instanceof ShortcutInfo) {
                    final ShortcutInfo info = (ShortcutInfo) tag;
                    final Intent intent = info.getIntent();
                    final ComponentName componentName = intent.getComponent();
                    info.dynamicIconDrawCallback = getDIDCForComponent(componentName);
                    if (info.dynamicIconDrawCallback != null) {
                        if (LogUtils.DEBUG_DYNAMIC_ICON) {
                            LogUtils.d(TAG, "updateShortcutsAndFoldersDynamicIcon: componentName = " + componentName
                                    + ", dynamicIconDrawCallback = " + info.dynamicIconDrawCallback
                                    + ", cellX = " + info.cellX + ", cellY = " + info.cellY
                                    + ", screenId = " + info.screenId);
                        }
                        view.invalidate();
                    }
                } else if (tag instanceof FolderInfo) {
                    Folder folder = ((FolderIcon) view).getFolder();
                    boolean hasDynamicIcon = updateFolderContentDynamicIcon(folder, null, true);
                    if (hasDynamicIcon) {
                        final FolderInfo info = (FolderInfo) tag;
                        if (LogUtils.DEBUG_DYNAMIC_ICON) {
                            LogUtils.d(TAG, "updateShortcutsAndFoldersDynamicIcon: cellX = " + info.cellX
                                    + ", cellY = " + info.cellY + ", screenId = " + info.screenId);
                        }
                        view.invalidate();
                    }
                }
            }
        }
    }

     /**
     * SPRD: Get DynamicIconDrawCallback of the app info with given component
     */
    public void updateDIDCInAppInfo(final ArrayList<AppInfo> apps) {
        if(apps == null){
            LogUtils.e(TAG, "updateDIDCInAppInfo: apps == null");
            return;
        }
        final int size = apps.size();
        AppInfo appInfo;
        for (int i = 0; i < size; i++) {
            appInfo = apps.get(i);
            appInfo.dynamicIconDrawCallback = getDIDCForComponent(appInfo.componentName);
        }
    }

    /**
     * SPRD: Draw dynamic part of the dynamic icon if needed.
     * @param canvas the canvas to draw the dynamic icon.
     * @param icon the view on which to draw the dynamic icon.
     * @param scale the scale of the dynamic icon.
     * @param center the center of the dynamic icon.
     */
    public static void drawDynamicIconIfNeed(Canvas canvas, View icon, float scale, int[] center) {
        if (!FeatureOption.SPRD_DYNAMIC_ICON_SUPPORT) {
            return;
        }
        if (icon instanceof BubbleTextView) {
            ItemInfo info = (ItemInfo) icon.getTag();
            if (info != null) {
                DynamicIconDrawCallback callback = info.dynamicIconDrawCallback;
                if (callback != null) {
                    if (LogUtils.DEBUG_DYNAMIC_ICON_ALL) {
                        LogUtils.d(TAG, "drawDynamicIconIfNeed: parent = " + icon.getParent().getParent()
                                + "callback = " + callback);
                    }
                    callback.drawDynamicIcon(canvas, icon, scale, center);
                }
            }
        }
    }

    public interface DynamicAppChangedCallbacks {
        void bindComponentDynamicIconChanged(ComponentName component);
    }

    public static boolean isDynamicIconView(final View v) {
        if (v instanceof BubbleTextView) {
            ItemInfo info = (ItemInfo) v.getTag();
            if (info != null && info.dynamicIconDrawCallback != null) {
                return true;
            }
        }
        return false;
    }
}
