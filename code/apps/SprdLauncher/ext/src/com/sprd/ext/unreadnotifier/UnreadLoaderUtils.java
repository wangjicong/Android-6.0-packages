package com.sprd.ext.unreadnotifier;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.NinePatchDrawable;
import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;
import android.view.View;

import com.android.sprdlauncher3.AppInfo;
import com.android.sprdlauncher3.AppsCustomizeCellLayout;
import com.android.sprdlauncher3.AppsCustomizePagedView;
import com.android.sprdlauncher3.BubbleTextView;
import com.android.sprdlauncher3.Folder;
import com.android.sprdlauncher3.FolderIcon;
import com.android.sprdlauncher3.FolderInfo;
import com.android.sprdlauncher3.ItemInfo;
import com.android.sprdlauncher3.Launcher;
import com.android.sprdlauncher3.LauncherSettings;
import com.android.sprdlauncher3.R;
import com.android.sprdlauncher3.ShortcutAndWidgetContainer;
import com.android.sprdlauncher3.ShortcutInfo;
import com.android.sprdlauncher3.Workspace;
import com.sprd.ext.LogUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Created by SPRD on 2016/9/27.
 */
class UnreadSupportShortcut {
    UnreadSupportShortcut(String pkgName, String clsName, String keyString, int type) {
        mComponent = new ComponentName(pkgName, clsName);
        mKey = keyString;
        mShortcutType = type;
        mUnreadNum = 0;
    }

    ComponentName mComponent;
    String mKey;
    int mShortcutType;
    int mUnreadNum;

    @Override
    public String toString() {
        return "{UnreadSupportShortcut[" + mComponent + "], key = " + mKey + ",type = "
                + mShortcutType + ",unreadNum = " + mUnreadNum + "}";
    }
}

/**
 * This class is a util class, implemented to do the following two things,:
 *
 * 1.Read config xml to get the shortcuts which support displaying unread number,
 * then get the initial value of the unread number of each component and update
 * shortcuts and folders through callbacks implemented in Launcher.
 *
 * 2. Receive unread broadcast sent by application, update shortcuts and folders in
 * workspace, hot seat and update application icons in app customize paged view.
 */
public class UnreadLoaderUtils extends BroadcastReceiver {
    private static final String TAG = "UnreadLoaderUtils";

    private static final String PREFS_FILE_NAME = TAG + "_Pref";

    private static final int UNREAD_TYPE_INTERNAL = 0;
    private static final int UNREAD_TYPE_EXTERNAL = 1;

    static final int INVALID_NUM = -1;

    public static final String ACTION_UNREAD_CHANGED = "com.sprd.action.UNREAD_CHANGED";
    private static final String EXTRA_UNREAD_COMPONENT = "com.sprd.intent.extra.UNREAD_COMPONENT";
    private static final String EXTRA_UNREAD_NUMBER = "com.sprd.intent.extra.UNREAD_NUMBER";

    private static final int MAX_UNREAD_COUNT = 99;

    private static final ArrayList<UnreadSupportShortcut> UNREAD_SUPPORT_SHORTCUTS =
            new ArrayList<>();

    private static int sUnreadSupportShortcutsNum = 0;
    private static final Object LOG_LOCK = new Object();

    private Context mContext;

    private SharedPreferences mSharePrefs;

    private static UnreadLoaderUtils INSTANCE;

    private Launcher mLauncher;
    private Workspace mWorkspace;
    private AppsCustomizePagedView mAppsCustomizeContent;

    private WeakReference<UnreadCallbacks> mCallbacks;

    private UnreadLoaderUtils(Context context) {
        mContext = context;
        mSharePrefs = mContext.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE);
    }

    public static UnreadLoaderUtils getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new UnreadLoaderUtils(context);
        }
        return INSTANCE;
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        final String action = intent.getAction();
        if (ACTION_UNREAD_CHANGED.equals(action)) {
            final ComponentName componentName = intent.getParcelableExtra(EXTRA_UNREAD_COMPONENT);
            final int unreadNum = intent.getIntExtra(EXTRA_UNREAD_NUMBER, INVALID_NUM);

            updateComponentUnreadInfo(unreadNum , componentName);
        }
    }

    public void updateComponentUnreadInfo(int unreadNum, ComponentName componentName) {
        if (LogUtils.DEBUG_UNREAD) {
            LogUtils.d(TAG, "[[[ updateComponentUnreadInfo ]]]: componentName = " + componentName
                    + ", unreadNum = " + unreadNum + ", mCallbacks = " + mCallbacks);
        }

        if (componentName != null && unreadNum != INVALID_NUM) {
            saveAndUpdateUI(componentName, unreadNum);
        }
    }

    /**
     * Set this as the current Launcher activity object for the loader.
     */
    public void initialize(Launcher launcher) {
        mLauncher = launcher;
        mWorkspace = mLauncher.getWorkspace();
        mAppsCustomizeContent = mLauncher.getAppsCustomizeContent();

        mCallbacks = new WeakReference<>((UnreadCallbacks)launcher);
        if (LogUtils.DEBUG_UNREAD) {
            LogUtils.d(TAG, "initialize: launcher = " + launcher
                    + ", mCallbacks = " + mCallbacks);
        }

        UnreadInfoManager.getInstance(mContext).createItemIfNeeded();
        UnreadInfoManager.getInstance(mContext).initAppsAndPermissionList();

        loadAndInitUnreadShortcuts();
    }

    /**
     * Load and initialize unread shortcuts.
     */
    public void loadAndInitUnreadShortcuts() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... unused) {
                loadUnreadSupportShortcuts();
                return null;
            }

            @Override
            protected void onPostExecute(final Void result) {
                //Make sure initialization of UNREAD_SUPPORT_SHORTCUTS has completed before initUnreadInfo.
                UnreadInfoManager.getInstance(mContext).initUnreadInfo(mLauncher);

                if (mCallbacks != null) {
                    UnreadCallbacks callbacks = mCallbacks.get();
                    if (callbacks != null) {
                        callbacks.bindUnreadInfoIfNeeded();
                    }
                }
            }
        }.execute();
    }

    private void saveAndUpdateUI(final ComponentName component, final int unReadNum) {
        final String key = component.flattenToShortString();
        final int index = supportUnreadFeature(component);
        boolean needUpdate = false;
        if (index != INVALID_NUM) {
            if (UNREAD_SUPPORT_SHORTCUTS.get(index).mUnreadNum != unReadNum) {
                saveUnreadNum(key, unReadNum);
                needUpdate = true;
            }
        } else {
            //add new info
            if (unReadNum > 0) {
                saveUnreadNum(key, unReadNum);
                needUpdate = true;
            }
        }

        if (needUpdate) {
            if (index != INVALID_NUM) {
                UNREAD_SUPPORT_SHORTCUTS.get(index).mUnreadNum = unReadNum;
                if (LogUtils.DEBUG_UNREAD) {
                    LogUtils.d(TAG, "saveAndUpdateUI,update SupportList, key:" + key + " success.");
                }
            } else {
                UnreadSupportShortcut usShortcut = new UnreadSupportShortcut(
                        component.getPackageName(),component.getClassName(),
                        key,UNREAD_TYPE_EXTERNAL);
                usShortcut.mUnreadNum = unReadNum;
                UNREAD_SUPPORT_SHORTCUTS.add(usShortcut);
                sUnreadSupportShortcutsNum = UNREAD_SUPPORT_SHORTCUTS.size();
                if (LogUtils.DEBUG_UNREAD) {
                    LogUtils.d(TAG, "saveAndUpdateUI, add To SupportList, key:" + key + " success."
                            + getUnreadSupportShortcutInfo());
                }
            }

            if(mCallbacks != null) {
                final UnreadCallbacks callbacks = mCallbacks.get();
                if (callbacks != null) {
                    callbacks.bindComponentUnreadChanged(component, unReadNum);
                }
            }
        }
    }

    private int readUnreadNum(final String key) {
        return mSharePrefs.getInt(key, INVALID_NUM);
    }

    private boolean saveUnreadNum(final String key, final int unReadNum) {
        SharedPreferences.Editor editor = mSharePrefs.edit();
        editor.putInt(key, unReadNum).apply();
        return true;
    }

    private boolean deleteUnreadNum(final String key) {
        SharedPreferences.Editor editor = mSharePrefs.edit();
        editor.remove(key).apply();
        return true;
    }

    private void loadUnreadSupportShortcuts() {
        long start = System.currentTimeMillis();
        if (LogUtils.DEBUG_UNREAD) {
            LogUtils.d(TAG, "loadUnreadSupportShortcuts begin: start = " + start);
        }

        // Clear all previous parsed unread shortcuts.
        UNREAD_SUPPORT_SHORTCUTS.clear();

        for (String key : mSharePrefs.getAll().keySet()) {
            boolean needCreateShortCut = false;
            int loadNum = 0;
            if (!UnreadInfoManager.getInstance(mContext).isDeniedPermissionItem(key)) {
                loadNum = readUnreadNum(key);
                needCreateShortCut = loadNum > 0;
            }

            if (needCreateShortCut) {
                ComponentName cmpName = ComponentName.unflattenFromString(key);

                UnreadSupportShortcut usShortcut = new UnreadSupportShortcut(
                        cmpName.getPackageName(), cmpName.getClassName(),
                        key, UNREAD_TYPE_INTERNAL );
                usShortcut.mUnreadNum = loadNum;
                if (!UNREAD_SUPPORT_SHORTCUTS.contains( usShortcut )) {
                    UNREAD_SUPPORT_SHORTCUTS.add(usShortcut);
                }
            } else {
                deleteUnreadNum(key);
            }
        }

        sUnreadSupportShortcutsNum = UNREAD_SUPPORT_SHORTCUTS.size();

        if (LogUtils.DEBUG_UNREAD) {
            LogUtils.d(TAG, "loadUnreadSupportShortcuts end: time used = "
                    + (System.currentTimeMillis() - start) + ",sUnreadSupportShortcutsNum = "
                    + sUnreadSupportShortcutsNum + getUnreadSupportShortcutInfo());
        }
    }

    /**
     * Get unread support shortcut information, since the information are stored
     * in an array list, we may query it and modify it at the same time, a lock
     * is needed.
     *
     * @return SupportShortString
     */
    private static String getUnreadSupportShortcutInfo() {
        String info = " Unread support shortcuts are ";
        ArrayList<UnreadSupportShortcut> logList = new ArrayList<>(UNREAD_SUPPORT_SHORTCUTS);
        synchronized (LOG_LOCK) {
            info += logList.toString();
        }
        return info;
    }

    /**
     * Whether the given component support unread feature.
     *
     * @param component component
     * @return array index, find fail return INVALID_NUM
     */
    static int supportUnreadFeature(ComponentName component) {
        if (component == null) {
            return INVALID_NUM;
        }

        final int size = UNREAD_SUPPORT_SHORTCUTS.size();
        for (int i = 0; i < size; i++) {
            if (UNREAD_SUPPORT_SHORTCUTS.get(i).mComponent.equals(component)) {
                return i;
            }
        }

        return INVALID_NUM;
    }

    /**
     * Get unread number of application at the given position in the supported
     * shortcut list.
     *
     * @param index
     * @return
     */
    static synchronized int getUnreadNumberAt(int index) {
        if (index < 0 || index >= sUnreadSupportShortcutsNum) {
            return 0;
        }
        return UNREAD_SUPPORT_SHORTCUTS.get(index).mUnreadNum;
    }

    /**
     * Get unread number for the given component.
     *
     * @param component
     * @return
     */
    public static int getUnreadNumberOfComponent(ComponentName component) {
        final int index = supportUnreadFeature(component);
        return getUnreadNumberAt(index);
    }

    public void bindComponentUnreadChanged(final ComponentName component, final int unreadNum) {
        if (mWorkspace != null) {
            updateComponentUnreadChanged(component, unreadNum);
        }

        if (mAppsCustomizeContent != null) {
            updateAppsUnreadChanged(component, unreadNum);
        }
    }

    /**
     * SPRD: Update unread number of shortcuts and folders in workspace and hotseat
     * with the given component.
     *
     * @param component app component
     * @param unreadNum app unreadNum
     */
    public void updateComponentUnreadChanged(ComponentName component, int unreadNum) {
        if (LogUtils.DEBUG_UNREAD) {
            LogUtils.d(TAG, "updateComponentUnreadChanged: component = " + component
                    + ", unreadNum = " + unreadNum);
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
                    if (LogUtils.DEBUG_UNREAD) {
                        LogUtils.d(TAG, "updateComponentUnreadChanged: view is null pointer");
                    }
                    continue;
                }

                if (tag instanceof ShortcutInfo) {
                    final ShortcutInfo info = (ShortcutInfo) tag;
                    final Intent intent = info.getIntent();
                    final ComponentName componentName = intent.getComponent();
                    if (componentName != null && componentName.equals(component)) {
                        if (info.unreadNum != unreadNum) {
                            info.unreadNum = unreadNum;
                            if (LogUtils.DEBUG_UNREAD) {
                                LogUtils.d(TAG, "updateComponentUnreadChanged is ShortcutInfo: find componentName = "
                                        + componentName + "cellX = " + info.cellX +
                                        ",cellY = " + info.cellY + ", Screen:" + info.screenId);
                            }
                            view.invalidate();
                        }

                    }
                } else if (tag instanceof FolderInfo) {
                    if (updateFolderUnreadNum((FolderIcon) view, component, unreadNum)) {
                        final FolderInfo info = (FolderInfo) tag;
                        if (LogUtils.DEBUG_UNREAD) {
                            LogUtils.d(TAG, "updateComponentUnreadChanged is FolderInfo: " +
                                    "component = " + component + "cellX = " + info.cellX +
                                    ",cellY = " + info.cellY + ", Screen:" + info.screenId);
                        }
                        view.invalidate();
                    }

                }
            }
        }

        /// SPRD: Update shortcut within folder if open folder exists.
        Folder openFolder = mWorkspace.getOpenFolder();
        updateContentUnreadNum(openFolder);
    }

    /**
     * SPRD: Update the unread message of the shortcut with the given information.
     *
     * @param unreadNum the number of the unread message.
     */
    public static boolean updateFolderUnreadNum(FolderIcon folderIcon, ComponentName component, int unreadNum) {
        if (folderIcon == null) {
            return false;
        }
        final ArrayList<ShortcutInfo> contents = folderIcon.getFolderInfo().contents;
        final int contentsCount = contents.size();
        int unreadNumTotal = 0;
        ShortcutInfo shortcutInfo;
        ComponentName name;
        final ArrayList<ComponentName> components = new ArrayList<>();
        for (int i = 0; i < contentsCount; i++) {
            shortcutInfo = contents.get(i);
            name = shortcutInfo.getIntent().getComponent();
            if (name != null && name.equals(component)) {
                shortcutInfo.unreadNum = unreadNum;
            }
            if (shortcutInfo.unreadNum > 0) {
                int j;
                for (j = 0; j < components.size(); j++) {
                    if (name != null && name.equals(components.get(j))) {
                        break;
                    }
                }

                if (j >= components.size()) {
                    components.add(name);
                    if (LogUtils.DEBUG_UNREAD) {
                        LogUtils.d(TAG, "updateFolderUnreadNum, shortcutInfo.unreadNum = " +shortcutInfo.unreadNum+ ", cellX = " + shortcutInfo.cellX +
                                ", cellY = " + shortcutInfo.cellY + ", Screen:" + shortcutInfo.screenId + ", container: "+shortcutInfo.container);
                    }
                    unreadNumTotal += shortcutInfo.unreadNum;
                }
            }
        }
        if (LogUtils.DEBUG_UNREAD) {
            LogUtils.d(TAG, "updateFolderUnreadNum, end: unreadNumTotal = " + unreadNumTotal);
        }

        return setFolderUnreadNum(folderIcon, unreadNumTotal);
    }

    /**
     * SPRD: Update the unread message number of the shortcut with the given value.
     *
     * @param unreadNum the number of the unread message.
     */
    public static boolean setFolderUnreadNum(FolderIcon folderIcon, int unreadNum) {
        if (folderIcon == null) {
            return false;
        }

        FolderInfo info = folderIcon.getFolderInfo();
        if (LogUtils.DEBUG_UNREAD) {
            LogUtils.d(TAG, "setFolderUnreadNum: unreadNum = " + unreadNum + ", info = " + info);
        }

        if (unreadNum <= 0) {
            unreadNum = 0;
        }

        if (unreadNum != info.unreadNum) {
            info.unreadNum = unreadNum;
            return true;
        }
        return false;
    }

    /**
     * SPRD: Update unread number of the content shortcut.
     */
    public void updateContentUnreadNum(Folder folder) {
        if (folder == null) {
            return;
        }
        if (LogUtils.DEBUG_UNREAD) {
            LogUtils.d(TAG, "Folder updateContentUnreadNum: folder.getInfo() = " + folder.getInfo());
        }
        final ArrayList<ShortcutAndWidgetContainer> childrenLayouts =
                folder.getAllShortcutContainersInFolder();
        int childCount;
        View view;
        Object tag;

        for (ShortcutAndWidgetContainer layout : childrenLayouts) {
            childCount = layout.getChildCount();
            for (int j = 0; j < childCount; j++) {
                view = layout.getChildAt(j);
                tag = view.getTag();
                if (LogUtils.DEBUG_UNREAD) {
                    LogUtils.d(TAG, "updateShortcutsAndFoldersUnread: tag = " + tag + ", j = "
                            + j + ", view = " + view);
                }
                if (tag instanceof ShortcutInfo) {
                    final ShortcutInfo info = (ShortcutInfo) tag;
                    if (LogUtils.DEBUG_UNREAD) {
                        LogUtils.d(TAG, "updateShortcutsAndFoldersUnread:info =" + info.toString());
                    }
                    view.invalidate();
                }
            }
        }
    }

    /**
     * SPRD: Update unread number of shortcuts and folders in workspace and hotseat.
     */
    public void updateShortcutsAndFoldersUnread() {
        if (mWorkspace == null) {
            return;
        }

        if (LogUtils.DEBUG_UNREAD) {
            LogUtils.d(TAG, "updateShortcutsAndFolderUnread: this = " + this);
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
                    final int num = getUnreadNumberOfComponent(componentName);
                    if (info.unreadNum != num) {
                        if (LogUtils.DEBUG_UNREAD) {
                            LogUtils.d(TAG, "updateShortcutsAndFoldersUnread is ShortcutInfo: " +
                                    ", component = " + componentName + "cellX = " + info.cellX +
                                    ",cellY = " + info.cellY + ", Screen:" + info.screenId);
                        }
                        info.unreadNum = num;
                        view.invalidate();
                    }
                } else if (tag instanceof FolderInfo) {
                    if (updateFolderUnreadNum((FolderIcon) view)) {
                        if (LogUtils.DEBUG_UNREAD) {
                            final FolderInfo info = (FolderInfo) tag;
                            LogUtils.d(TAG, "updateComponentUnreadChanged is FolderInfo: " +
                                    " cellX = " + info.cellX +
                                    ",cellY = " + info.cellY + ", Screen:" + info.screenId);
                        }
                        view.invalidate();
                    }
                }
            }
        }
    }

    /**
     * SPRD: Update unread number of the folder, the number is the total unread number
     * of all shortcuts in folder, duplicate shortcut will be only count once.
     */
    public boolean updateFolderUnreadNum(FolderIcon folderIcon) {
        if (folderIcon == null) {
            return false;
        }
        final ArrayList<ShortcutInfo> contents = folderIcon.getFolderInfo().contents;
        final int contentsCount = contents.size();
        int unreadNumTotal = 0;
        final ArrayList<ComponentName> components = new ArrayList<>();
        ShortcutInfo shortcutInfo;
        ComponentName componentName;
        int unreadNum;
        for (int i = 0; i < contentsCount; i++) {
            shortcutInfo = contents.get(i);
            componentName = shortcutInfo.getIntent().getComponent();
            unreadNum = getUnreadNumberOfComponent(componentName);
            if (unreadNum > 0) {
                shortcutInfo.unreadNum = unreadNum;
                int j;
                for (j = 0; j < components.size(); j++) {
                    if (componentName != null && componentName.equals(components.get(j))) {
                        break;
                    }
                }
                if (j >= components.size()) {
                    components.add(componentName);
                    if (LogUtils.DEBUG_UNREAD) {
                        LogUtils.d(TAG, "updateFolderUnreadNum, shortcutInfo.unreadNum = " +shortcutInfo.unreadNum+ ", cellX = " + shortcutInfo.cellX +
                                ", cellY = " + shortcutInfo.cellY + ", Screen:" + shortcutInfo.screenId + ", container: "+shortcutInfo.container);
                    }
                    unreadNumTotal += unreadNum;
                }
            }
        }
        if (LogUtils.DEBUG_UNREAD) {
            LogUtils.d(TAG, "updateFolderUnreadNum end: unreadNumTotal = " + unreadNumTotal);
        }
        return setFolderUnreadNum(folderIcon, unreadNumTotal);
    }

    /**
     * SPRD: Update unread number of the given component in app customize paged view
     * with the given value, first find the icon, and then update the number.
     *
     * @param component
     * @param unreadNum
     */
    public void updateAppsUnreadChanged(ComponentName component, int unreadNum) {
        if(mAppsCustomizeContent == null) {
            return;
        }
        int numAppsPages = mAppsCustomizeContent.getNumAppPages();
        if (LogUtils.DEBUG_UNREAD) {
            LogUtils.d(TAG, "updateAppsUnreadChanged: component = " + component
                    + ",unreadNum = " + unreadNum + ",numAppsPages = " + numAppsPages);
        }
        updateUnreadNumInAppInfo(component, unreadNum);
        for (int i = 0; i < numAppsPages; i++) {
            View view = mAppsCustomizeContent.getPageAt(i);
            if(view != null && view instanceof AppsCustomizeCellLayout){
                AppsCustomizeCellLayout cl = (AppsCustomizeCellLayout)view;
                if(cl == null){
                    LogUtils.d(TAG, "updateAppsUnreadChanged: cl == null");
                    continue;
                }
                ShortcutAndWidgetContainer container = cl.getShortcutsAndWidgets();
                if(container == null){
                    LogUtils.d(TAG, "updateAppsUnreadChanged: container == null");
                    continue;
                }
                final int count = container.getChildCount();
                if (LogUtils.DEBUG_UNREAD) {
                    LogUtils.d(TAG, "updateAppsUnreadChanged: getPageChildCount() == " + count);
                }
                BubbleTextView appIcon;
                AppInfo appInfo;
                for (int j = 0; j < count; j++) {
                    appIcon = (BubbleTextView) container.getChildAt(j);
                    if (appIcon == null) {
                        continue;
                    }
                    appInfo = (AppInfo) appIcon.getTag();
                    if (LogUtils.DEBUG_UNREAD) {
                        LogUtils.d(TAG, "updateAppsUnreadChanged: component = " + component
                                        + ", appInfo = " + appInfo.componentName + ", appIcon = "
                                        + appIcon);
                    }
                    if (appInfo != null && appInfo.componentName.equals(component)) {
                        appInfo.unreadNum = unreadNum;
                        appIcon.invalidate();
                    }
                }
            }
        }
    }

    /**
     * SPRD: Update the unread number of the app info with given component.
     *
     */
    private void updateUnreadNumInAppInfo(ComponentName component, int unreadNum) {
        final int size = mAppsCustomizeContent.mApps.size();
        AppInfo appInfo;
        for (int i = 0; i < size; i++) {
            appInfo = mAppsCustomizeContent.mApps.get(i);
            if (appInfo.getIntent().getComponent().equals(component)) {
                appInfo.unreadNum = unreadNum;
            }
        }
    }

    /**
     * SPRD: Update unread number of all application info.
     */
    public void updateAppsUnread() {
        if(mAppsCustomizeContent == null) {
            return;
        }
        int numAppsPages = mAppsCustomizeContent.getNumAppPages();
        if (LogUtils.DEBUG_UNREAD) {
            LogUtils.d(TAG, "updateAppsUnreadChanged: numAppsPages = " + numAppsPages);
        }

        updateUnreadNumInAppInfo(mAppsCustomizeContent.mApps);
        // Update apps which already shown in the customized pane.
        for (int i = 0; i < numAppsPages; i++) {
            View view = mAppsCustomizeContent.getPageAt(i);
            if (view != null && view instanceof AppsCustomizeCellLayout) {
                AppsCustomizeCellLayout cl = (AppsCustomizeCellLayout)view;
                if(cl == null){
                    LogUtils.d(TAG, "updateAppsUnread: cl == null");
                    continue;
                }
            }
        }
    }

    /**
     * SPRD: Update the unread number of the app info with given component.
     *
     */
	public static void updateUnreadNumInAppInfo(final ArrayList<AppInfo> apps) {
        if(apps == null){
            if (LogUtils.DEBUG_UNREAD) {
                LogUtils.d(TAG, "updateUnreadNumInAppInfo: apps == null");
            }
            return;
        }
        final int size = apps.size();
        AppInfo appInfo;
        for (int i = 0; i < size; i++) {
            appInfo = apps.get(i);
            appInfo.unreadNum = getUnreadNumberOfComponent(appInfo.componentName);
        }
    }

    /**
     * Draw unread number for the given icon.
     *
     * @param canvas
     * @param icon
     * @return
     */
    public static void drawUnreadEventIfNeed(Canvas canvas, View icon) {
        ItemInfo info = (ItemInfo) icon.getTag();

        if (info != null && info.unreadNum > 0) {
            Resources res = icon.getContext().getResources();

            /// SPRD: Meature sufficent width for unread text and background image
            Paint unreadTextNumberPaint = new Paint();
            unreadTextNumberPaint.setTextSize(res.getDimension(R.dimen.unread_text_number_size));
            unreadTextNumberPaint.setTypeface(Typeface.DEFAULT_BOLD);
            unreadTextNumberPaint.setColor(0xffffffff);
            unreadTextNumberPaint.setTextAlign(Paint.Align.CENTER);

            Paint unreadTextPlusPaint = new Paint(unreadTextNumberPaint);
            unreadTextPlusPaint.setTextSize(res.getDimension(R.dimen.unread_text_plus_size));

            String unreadTextNumber;
            String unreadTextPlus = "+";
            Rect unreadTextNumberBounds = new Rect(0, 0, 0, 0);
            Rect unreadTextPlusBounds = new Rect(0, 0, 0, 0);
            if (info.unreadNum > MAX_UNREAD_COUNT) {
                unreadTextNumber = String.valueOf(MAX_UNREAD_COUNT);
                unreadTextPlusPaint.getTextBounds(unreadTextPlus, 0,
                        unreadTextPlus.length(), unreadTextPlusBounds);
            } else {
                unreadTextNumber = String.valueOf(info.unreadNum);
            }
            unreadTextNumberPaint.getTextBounds(unreadTextNumber, 0,
                    unreadTextNumber.length(), unreadTextNumberBounds);
            int textHeight = unreadTextNumberBounds.height();
            int textWidth = unreadTextNumberBounds.width() + unreadTextPlusBounds.width();

            /// SPRD: Draw unread background image.
            NinePatchDrawable unreadBgNinePatchDrawable =
                    (NinePatchDrawable) ContextCompat.getDrawable(icon.getContext(),
                            R.drawable.ic_newevent_bg );
            int unreadBgWidth = unreadBgNinePatchDrawable.getIntrinsicWidth();
            int unreadBgHeight = unreadBgNinePatchDrawable.getIntrinsicHeight();

            int unreadMinWidth = (int) res.getDimension(R.dimen.unread_minWidth);
            if (unreadBgWidth < unreadMinWidth) {
                unreadBgWidth = unreadMinWidth;
            }
            int unreadTextMargin = (int) res.getDimension(R.dimen.unread_text_margin);
            if (unreadBgWidth < textWidth + unreadTextMargin) {
                unreadBgWidth = textWidth + unreadTextMargin;
            }
            if (unreadBgHeight < textHeight) {
                unreadBgHeight = textHeight;
            }
            Rect unreadBgBounds = new Rect(0, 0, unreadBgWidth, unreadBgHeight);
            unreadBgNinePatchDrawable.setBounds(unreadBgBounds);

            int unreadMarginTop = 0;
            int unreadMarginRight = 0;
            if (info instanceof ShortcutInfo) {
                if (info.container == (long) LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
                    unreadMarginTop = (int) res.getDimension(R.dimen.hotseat_unread_margin_top);
                    unreadMarginRight = (int) res.getDimension(R.dimen.hotseat_unread_margin_right);
                } else if (info.container == (long) LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                    unreadMarginTop = (int) res.getDimension(R.dimen.workspace_unread_margin_top);
                    unreadMarginRight = (int) res.getDimension(
                            R.dimen.workspace_unread_margin_right);
                } else {
                    unreadMarginTop = (int) res.getDimension(R.dimen.folder_unread_margin_top);
                    unreadMarginRight = (int) res.getDimension(R.dimen.folder_unread_margin_right);
                }
            } else if (info instanceof FolderInfo) {
                if (info.container == (long) LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
                    unreadMarginTop = (int) res.getDimension(R.dimen.hotseat_unread_margin_top);
                    unreadMarginRight = (int) res.getDimension(R.dimen.hotseat_unread_margin_right);
                } else if (info.container == (long) LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                    unreadMarginTop = (int) res.getDimension(R.dimen.workspace_unread_margin_top);
                    unreadMarginRight = (int) res.getDimension(
                            R.dimen.workspace_unread_margin_right);
                }
            } else if (info instanceof AppInfo) {
                unreadMarginTop = (int) res.getDimension(R.dimen.app_list_unread_margin_top);
                unreadMarginRight = (int) res.getDimension(R.dimen.app_list_unread_margin_right);
            }

            int unreadBgPosX = icon.getScrollX() + icon.getWidth()
                    - unreadBgWidth - unreadMarginRight;
            int unreadBgPosY = icon.getScrollY() + unreadMarginTop;

            canvas.save();
            canvas.translate(unreadBgPosX, unreadBgPosY);

            unreadBgNinePatchDrawable.draw(canvas);

            /// SPRD: Draw unread text.
            Paint.FontMetrics fontMetrics = unreadTextNumberPaint.getFontMetrics();
            if (info.unreadNum > MAX_UNREAD_COUNT) {
                canvas.drawText(unreadTextNumber,
                        (unreadBgWidth - unreadTextPlusBounds.width()) / 2,
                        (unreadBgHeight + textHeight) / 2,
                        unreadTextNumberPaint);
                canvas.drawText(unreadTextPlus,
                        (unreadBgWidth + unreadTextNumberBounds.width()) / 2,
                        (unreadBgHeight + textHeight) / 2 + fontMetrics.ascent / 2,
                        unreadTextPlusPaint);
            } else {
                canvas.drawText(unreadTextNumber,
                        unreadBgWidth / 2,
                        (unreadBgHeight + textHeight) / 2,
                        unreadTextNumberPaint);
            }

            canvas.restore();
        }
    }

    public static boolean shouldDrawUnreadInfo(final View v) {
        boolean result = false;
        if(v instanceof BubbleTextView) {
            ItemInfo info = (ItemInfo) v.getTag();
            if(info!= null && info.unreadNum > 0) {
                result = true;
            }
        }
        return result;
    }

    public interface UnreadCallbacks {
        /**
         * Bind shortcuts and application icons with the given component, and
         * update folders unread which contains the given component.
         *
         * @param component
         * @param unreadNum
         */
        void bindComponentUnreadChanged(ComponentName component, int unreadNum);

        /**
         * Bind unread shortcut information if needed, this call back is used to
         * update shortcuts and folders when launcher first created.
         */
        void bindUnreadInfoIfNeeded();
    }
}

