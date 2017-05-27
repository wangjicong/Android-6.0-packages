package com.sprd.ext.unreadnotifier;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;
import android.widget.Toast;

import com.android.sprdlauncher3.Launcher;
import com.sprd.ext.LogUtils;
import com.sprd.ext.unreadnotifier.calendar.UnreadCalendarItem;
import com.sprd.ext.unreadnotifier.call.MissCallItem;
import com.sprd.ext.unreadnotifier.email.UnreadEmailItem;
import com.sprd.ext.unreadnotifier.sms.UnreadMessageItem;

import java.util.ArrayList;

/**
 * Created by SPRD on 11/15/16.
 */

public class UnreadInfoManager {
    private static final String TAG = "UnreadInfoManager";

    private static final int PERMISSIONS_REQUEST_CODE = 1001;

    @SuppressLint("StaticFieldLeak")
    private static UnreadInfoManager INSTANCE;
    public Context mContext;

    public static final int TYPE_CALL_LOG = 101;
    public static final int TYPE_SMS = 102;
    public static final int TYPE_EMAIL = 103;
    public static final int TYPE_CALENDAR = 104;

    private UnreadMessageItem mMessageUnreadItem;
    private MissCallItem mMissCallItem;
    private UnreadEmailItem mUnreadEmailItem;
    private UnreadCalendarItem mUnreadCalendarItem;

    private static final ArrayList<UnreadBaseItem> ALL_ITEMS =
            new ArrayList<>();
    private static final ArrayList<UnreadBaseItem> ALL_GRANTEDPERMISSION_ITEMS =
            new ArrayList<>();
    private static final ArrayList<UnreadBaseItem> ALL_DENIEDPERMISSION_ITEMS =
            new ArrayList<>();

    private UnreadInfoManager(Context context) {
        mContext = context;
    }

    public static UnreadInfoManager getInstance(Context context) {
        synchronized (UnreadInfoManager.class) {
            if (INSTANCE == null) {
                INSTANCE = new UnreadInfoManager(context);
            }
        }
        return INSTANCE;
    }

    void createItemIfNeeded() {
        if(ALL_ITEMS.isEmpty()) {
            if(LogUtils.DEBUG_UNREAD) {
                LogUtils.d(TAG, " ALL_ITEMS is empty, need create items.");
            }
            createItems();
        }

    }

    private void createItems() {
        if(mMessageUnreadItem == null) {
            mMessageUnreadItem = new UnreadMessageItem(mContext);
            ALL_ITEMS.add(mMessageUnreadItem);
        }

        if(mMissCallItem == null) {
            mMissCallItem = new MissCallItem( mContext);
            ALL_ITEMS.add(mMissCallItem);
        }

        if(mUnreadEmailItem == null) {
            mUnreadEmailItem = new UnreadEmailItem(mContext);
            ALL_ITEMS.add(mUnreadEmailItem);
        }

        if(mUnreadCalendarItem == null) {
            mUnreadCalendarItem = new UnreadCalendarItem(mContext);
            ALL_ITEMS.add(mUnreadCalendarItem);
        }

        if(LogUtils.DEBUG_UNREAD) {
            LogUtils.d(TAG, "createItems(), size of ALL_ITEMS is "+ALL_ITEMS.size());
        }
    }

    void initAppsAndPermissionList() {
        int N = ALL_ITEMS.size();

        ALL_GRANTEDPERMISSION_ITEMS.clear();
        ALL_DENIEDPERMISSION_ITEMS.clear();

        for(int i = 0; i<N;i++) {
            UnreadBaseItem item = ALL_ITEMS.get(i);

            //verify ComponentName
            ArrayList<String> listValues = item.loadApps(item.mContext);
            item.verifyDefaultCN(listValues, item.mDefaultCn);
            item.setInstalledList(listValues);

            //init permission List
            if (item.isPersistChecked()) {
                if (item.checkPermission()) {
                    ALL_GRANTEDPERMISSION_ITEMS.add(item);
                } else {
                    ALL_DENIEDPERMISSION_ITEMS.add(item);
                }
            }
        }
    }

    void initUnreadInfo(Context context){
        int N = ALL_DENIEDPERMISSION_ITEMS.size();

        String[] deniedString = new String[N];
        for (int i = 0 ; i < N; i++) {
            deniedString[i] = ALL_DENIEDPERMISSION_ITEMS.get(i).mPermission;
        }
        if(N > 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ((Launcher) context).requestPermissions(deniedString, PERMISSIONS_REQUEST_CODE);
            }
        }

        N = ALL_GRANTEDPERMISSION_ITEMS.size();
        for (int i = 0 ; i < N; i++) {
            UnreadBaseItem item = ALL_GRANTEDPERMISSION_ITEMS.get(i);
            if(item != null) {
                item.mContentObserver.registerContentObserver();
                item.updateUIFromDatabase();
            }
        }
    }

    boolean isDeniedPermissionItem(String key) {
        int N = ALL_DENIEDPERMISSION_ITEMS.size();
        for (int i = 0 ; i < N; i++) {
            UnreadBaseItem item = ALL_DENIEDPERMISSION_ITEMS.get(i);
            if(item != null && item.getCurrentComponentName() != null) {
                String value = item.getCurrentComponentName().flattenToShortString();
                if (value.equals(key)) {
                    return true;
                }
            }
        }
        return false;
    }

    public UnreadBaseItem getItemByType(int type) {
        for (int i = 0; i < ALL_ITEMS.size(); i++) {
            UnreadBaseItem item = ALL_ITEMS.get(i);
            if (item.mType == type) {
                return item;
            }
        }
        return null;
    }

    public UnreadBaseItem getItemByKey(String key) {
        for (int i = 0; i < ALL_ITEMS.size(); i++) {
            UnreadBaseItem item = ALL_ITEMS.get(i);
            if (item.mPrefKey.equals(key)) {
                return item;
            }
        }
        return null;
    }

    public void handleRequestPermissionResult(String[] permissions,
                                              int[] grantResults) {
        if(LogUtils.DEBUG_UNREAD) {
            LogUtils.d(TAG, "handleRequestPermissionResult, onPermissionsResult counts: " + permissions.length + ":" + grantResults.length);
        }

        for (int i = 0; i < ALL_DENIEDPERMISSION_ITEMS.size(); i++) {
            UnreadBaseItem item = ALL_DENIEDPERMISSION_ITEMS.get(i);
            if(grantResults.length > 0 && permissions.length >0 && item.mPermission.equals(permissions[i])) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    if(LogUtils.DEBUG_UNREAD) {
                        LogUtils.d(TAG, "handleRequestPermissionResult, permission granted:" + item.mPermission);
                    }
                    item.mContentObserver.registerContentObserver();
                    item.updateUIFromDatabase();
                } else if (grantResults[i] == PackageManager.PERMISSION_DENIED){
                    Toast.makeText(mContext, item.getUnreadHintString(), Toast.LENGTH_LONG).show();
                    if(LogUtils.DEBUG_UNREAD) {
                        LogUtils.d(TAG, "handleRequestPermissionResult, permission denied:" + item.mPermission);
                    }
                }
            }
        }

    }

    static void updateUI(final Context context, final String desComponentName) {
        if(LogUtils.DEBUG_UNREAD) {
            LogUtils.d(TAG, "updateUI, desComponentName = "+desComponentName);
        }
        if (!TextUtils.isEmpty(desComponentName)) {
            ComponentName cmpName = ComponentName.unflattenFromString(desComponentName);
            int unreadCount = getUnreadCountForDesComponent(cmpName);
            UnreadLoaderUtils.getInstance(context).updateComponentUnreadInfo(unreadCount, cmpName);
        }
    }

    private static int getUnreadCountForDesComponent(final ComponentName desComponentName) {
        int result = 0;
        for(UnreadBaseItem item : ALL_ITEMS) {
            if (!TextUtils.isEmpty(item.mCurrentCn)) {
                ComponentName cmpName = ComponentName.unflattenFromString(item.mCurrentCn);
                if (cmpName.equals(desComponentName)) {
                    result += item.getUnreadCount();
                }
            }
        }
        if(LogUtils.DEBUG_UNREAD) {
            LogUtils.d(TAG, "getUnreadCountForDesComponent, unreadCount of desComponentName: "
                    +desComponentName+" is: "+result);
        }
        return result;
    }

}
