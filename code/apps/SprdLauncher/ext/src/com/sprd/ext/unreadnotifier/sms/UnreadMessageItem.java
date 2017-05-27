package com.sprd.ext.unreadnotifier.sms;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemProperties;
import android.provider.BaseColumns;

import com.android.sprdlauncher3.R;
import com.sprd.ext.LogUtils;
import com.sprd.ext.UtilitiesExt;
import com.sprd.ext.unreadnotifier.BaseContentObserver;
import com.sprd.ext.unreadnotifier.MMSAppUtils;
import com.sprd.ext.unreadnotifier.UnreadBaseItem;
import com.sprd.ext.unreadnotifier.UnreadInfoManager;
import com.sprd.ext.unreadnotifier.UnreadSettingsFragment;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by SPRD on 2016/11/22.
 */

public class UnreadMessageItem extends UnreadBaseItem {
    private static final String TAG = "MessageUnreadItem";
    private static final Uri MMS_CONTENT_URI = Uri.parse("content://mms");
    private static final Uri SMS_CONTENT_URI = Uri.parse("content://sms");
    private static final Uri MMSSMS_CONTENT_URI = Uri.parse("content://mms-sms");

    static final ComponentName DEFAULT_CNAME = new ComponentName("com.android.messaging",
            "com.android.messaging.ui.conversationlist.ConversationListActivity");

    private static final String PROP_DEFAULT_SMS = "ro.launcher.unread.sms";

    public UnreadMessageItem(Context context) {
        super(context);
        mContentObserver = new BaseContentObserver(new Handler(), context, MMSSMS_CONTENT_URI, this);
        mPermission = Manifest.permission.READ_SMS;
        mPrefKey = UnreadSettingsFragment.PREF_KEY_UNREAD_SMS;
        mType = UnreadInfoManager.TYPE_SMS;
        mDefaultCn = DEFAULT_CNAME;
        boolean defaultState = mContext.getResources().getBoolean(R.bool.config_default_unread_sms_enable);
        mDefaultState = SystemProperties.getBoolean(PROP_DEFAULT_SMS, defaultState);
    }

    @Override
    public int readUnreadCount() {
        int unreadSms = 0;
        int unreadMms = 0;
        ContentResolver resolver = mContext.getContentResolver();

        boolean result = checkPermission();
        if (!result) {
            LogUtils.w(TAG, "no READ_SMS Permission");
            return 0;
        }

        final Cursor smsCursor = resolver.query(SMS_CONTENT_URI, new String[]{BaseColumns._ID},
                "type =1 AND read = 0", null, null);
        if (smsCursor != null) {
            try {
                unreadSms = smsCursor.getCount();
            } catch (Exception e) {
                // TODO: handle exception
                LogUtils.d(TAG, "readUnreadCount SMS Exception: "+ e);
            } finally {
                UtilitiesExt.closeCursorSilently(smsCursor);
            }
        }

        final Cursor mmsCursor = resolver.query(MMS_CONTENT_URI, new String[]{BaseColumns._ID},
                "msg_box = 1 AND read = 0 AND ( m_type =130 OR m_type = 132 ) AND thread_id > 0",
                null, null);
        if (mmsCursor != null) {
            try {
                unreadMms = mmsCursor.getCount();
            } catch (Exception e) {
                // TODO: handle exception
                LogUtils.d(TAG, "readUnreadCount MMS Exception: "+ e);
            } finally {
                UtilitiesExt.closeCursorSilently(mmsCursor);
            }
        }

        if(LogUtils.DEBUG_UNREAD) {
            LogUtils.d(TAG, "readUnreadCount: unread [sms : mms] = ["
                    + unreadSms + " : " + unreadMms + "]");
        }

        return unreadMms + unreadSms;
    }

    @Override
    public String getUnreadHintString() {
        String name = mContext.getString(R.string.unread_sms);
        return mContext.getString(R.string.unread_hint, name);
    }

    @Override
    public ArrayList<String> loadApps(Context context) {
        Collection<MMSAppUtils.SmsApplicationData> smsApplications =
                MMSAppUtils.getApplicationCollection(context);

        ArrayList<String> installedMsgList = new ArrayList<>();

        for (MMSAppUtils.SmsApplicationData smsApplicationData : smsApplications) {
            ComponentName componentName = new ComponentName(smsApplicationData.mPackageName, smsApplicationData.smsClassName);
            String scName = componentName.flattenToShortString();
            installedMsgList.add(scName);
        }

        return installedMsgList;
    }
}



