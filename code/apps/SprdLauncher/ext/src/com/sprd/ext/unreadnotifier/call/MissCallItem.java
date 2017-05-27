package com.sprd.ext.unreadnotifier.call;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemProperties;
import android.provider.BaseColumns;
import android.provider.CallLog;

import com.android.sprdlauncher3.R;
import com.sprd.ext.LogUtils;
import com.sprd.ext.UtilitiesExt;
import com.sprd.ext.unreadnotifier.BaseContentObserver;
import com.sprd.ext.unreadnotifier.CallAppUtils;
import com.sprd.ext.unreadnotifier.UnreadBaseItem;
import com.sprd.ext.unreadnotifier.UnreadInfoManager;
import com.sprd.ext.unreadnotifier.UnreadSettingsFragment;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by SPRD on 2016/11/22.
 */

public class MissCallItem extends UnreadBaseItem {
    private static final String TAG = "MissCallItem";
    private static final Uri CALLS_CONTENT_URI = CallLog.Calls.CONTENT_URI;
    private static final String MISSED_CALLS_SELECTION =
            CallLog.Calls.TYPE + " = " + CallLog.Calls.MISSED_TYPE + " AND " + CallLog.Calls.NEW + " = 1";

    static final ComponentName DEFAULT_CNAME = new ComponentName("com.android.dialer",
            "com.android.dialer.DialtactsActivity");

    private static final String PROP_DEFAULT_CALL = "ro.launcher.unread.call";

    public MissCallItem(Context context) {
        super(context);
        mContentObserver = new BaseContentObserver(new Handler(), context, CALLS_CONTENT_URI, this);
        mPermission = Manifest.permission.READ_CALL_LOG;
        mPrefKey = UnreadSettingsFragment.PREF_KEY_MISS_CALL;
        mType = UnreadInfoManager.TYPE_CALL_LOG;
        mDefaultCn = DEFAULT_CNAME;
        boolean defaultState = mContext.getResources().getBoolean(R.bool.config_default_unread_call_enable);
        mDefaultState = SystemProperties.getBoolean(PROP_DEFAULT_CALL, defaultState);
    }

    @Override
    public int readUnreadCount() {
        int missedCalls = 0;
        ContentResolver resolver = mContext.getContentResolver();

        boolean result = checkPermission();
        if (!result) {
            LogUtils.w(TAG, "no READ_CALL_LOG Permission");
            return 0;
        }

        final Cursor cursor = resolver.query(CALLS_CONTENT_URI, new String[]{BaseColumns._ID},
                MISSED_CALLS_SELECTION, null, null);
        if (cursor != null) {
            try {
                missedCalls = cursor.getCount();
            } catch (Exception e) {
                // TODO: handle exception
                LogUtils.d(TAG, "readUnreadCount Exception: "+ e);
            } finally {
                UtilitiesExt.closeCursorSilently(cursor);
            }
        }

        if(LogUtils.DEBUG_UNREAD) LogUtils.d(TAG, "readUnreadCount, missedCalls = "+missedCalls);

        return missedCalls;
    }

    @Override
    public String getUnreadHintString() {
        String name = mContext.getString(R.string.unread_call);
        return mContext.getString(R.string.unread_hint, name);
    }

    @Override
    public ArrayList<String> loadApps(Context context) {
        Collection<CallAppUtils.CallApplicationData> callApplications =
                CallAppUtils.getInstalledDialerApplications(context);

        ArrayList<String> installedPhoneList = new ArrayList<>();
        for (CallAppUtils.CallApplicationData callApplication : callApplications) {
            ComponentName componentName = new ComponentName(callApplication.mPackageName, callApplication.callClassName);
            String scName = componentName.flattenToShortString();
            installedPhoneList.add(scName);
        }

        return installedPhoneList;
    }
}