package com.sprd.ext.unreadnotifier.email;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemProperties;

import com.android.sprdlauncher3.R;
import com.sprd.ext.LogUtils;
import com.sprd.ext.UtilitiesExt;
import com.sprd.ext.unreadnotifier.BaseContentObserver;
import com.sprd.ext.unreadnotifier.UnreadBaseItem;
import com.sprd.ext.unreadnotifier.UnreadInfoManager;
import com.sprd.ext.unreadnotifier.UnreadSettingsFragment;

import java.util.ArrayList;

/**
 * Created by SPRD on 1/25/17.
 */

public class UnreadEmailItem extends UnreadBaseItem{
    private static final String TAG = "UnreadEmailItem";
    private static final Uri EMAILS_CONTENT_URI = Uri.parse("content://com.android.email.provider/mailbox");
    private static final Uri EMAILS_NOTIFY_URI = Uri.parse("content://com.android.email.notifier");

    static final ComponentName DEFAULT_CNAME = new ComponentName("com.android.email",
            "com.android.email.activity.Welcome");

    private static final String PROP_DEFAULT_EMAIL = "ro.launcher.unread.email";

    public UnreadEmailItem(Context context) {
        super(context);
        mContentObserver = new BaseContentObserver(new Handler(), context, EMAILS_NOTIFY_URI, this);
        mPermission = "com.android.email.permission.ACCESS_PROVIDER";
        mPrefKey = UnreadSettingsFragment.PREF_KEY_UNREAD_EMAIL;
        mType = UnreadInfoManager.TYPE_EMAIL;
        mDefaultCn = DEFAULT_CNAME;
        boolean defaultState = mContext.getResources().getBoolean(R.bool.config_default_unread_email_enable);
        mDefaultState = SystemProperties.getBoolean(PROP_DEFAULT_EMAIL, defaultState);
    }

    @Override
    public int readUnreadCount() {
        int unreadEmail = 0;
        int unRead;

        ContentResolver resolver = mContext.getContentResolver();

        boolean result = checkPermission();
        if (!result) {
            LogUtils.w(TAG, "no READ EMAIL Permission");
            return 0;
        }

        final Cursor cursor = resolver.query(EMAILS_CONTENT_URI, new String[] {"unreadCount"},
                "type = ?", new String[] {"0"}, null);
        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    unRead = cursor.getInt(0);
                    if (unRead > 0) {
                        unreadEmail += unRead;
                    }
                }

            } catch (Exception e) {
                // TODO: handle exception
                LogUtils.d(TAG, "getUnreadEmailCount: updateUnreadEmailCount Exception: "+ e);
            } finally {
                UtilitiesExt.closeCursorSilently(cursor);
            }
        }

        if(LogUtils.DEBUG_UNREAD) LogUtils.d(TAG, "readUnreadCount: unreadEmail = " + unreadEmail);

        return unreadEmail;
    }

    @Override
    public String getUnreadHintString() {
        String name = mContext.getString(R.string.unread_email);
        return mContext.getString(R.string.unread_hint, name);
    }

    @Override
    public ArrayList<String> loadApps(Context context) {
        String[] emailLists = context.getResources().getStringArray(R.array.support_email_component_array);

        ArrayList<String> installEmailList = new ArrayList<>();
        for (String emailList : emailLists) {
            ComponentName componentName = ComponentName.unflattenFromString( emailList );
            boolean isInstalled = UtilitiesExt.isAppInstalled(context, componentName);
            if (isInstalled) {
                installEmailList.add(componentName.flattenToShortString());
            }
        }

        return installEmailList;
    }
}
