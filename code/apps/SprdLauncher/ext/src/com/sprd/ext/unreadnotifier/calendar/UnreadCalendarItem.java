package com.sprd.ext.unreadnotifier.calendar;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemProperties;
import android.provider.CalendarContract;
import android.provider.CalendarContract.CalendarAlerts;

import com.android.sprdlauncher3.R;
import com.sprd.ext.LogUtils;
import com.sprd.ext.UtilitiesExt;
import com.sprd.ext.unreadnotifier.BaseContentObserver;
import com.sprd.ext.unreadnotifier.UnreadBaseItem;
import com.sprd.ext.unreadnotifier.UnreadInfoManager;
import com.sprd.ext.unreadnotifier.UnreadSettingsFragment;

import java.util.ArrayList;

/**
 * Created by SPRD on 2/8/17.
 */

public class UnreadCalendarItem extends UnreadBaseItem{
    private static final String TAG = "UnreadCalendarItem";
    private static final Uri CALENDARS_CONTENT_URI = CalendarContract.CalendarAlerts.CONTENT_URI;

    static final ComponentName DEFAULT_CNAME = new ComponentName("com.android.calendar",
            "com.android.calendar.AllInOneActivity");

    private static final String PROP_DEFAULT_CALENDAR = "ro.launcher.unread.calendar";

    public UnreadCalendarItem(Context context) {
        super(context);
        mContentObserver = new BaseContentObserver(new Handler(), context, CALENDARS_CONTENT_URI, this);
        mPermission = Manifest.permission.READ_CALENDAR;
        mPrefKey = UnreadSettingsFragment.PREF_KEY_UNREAD_CALENDAR;
        mDefaultCn = DEFAULT_CNAME;
        mType = UnreadInfoManager.TYPE_CALENDAR;
        boolean defaultState = mContext.getResources().getBoolean(R.bool.config_default_unread_calendar_enable);
        mDefaultState =  SystemProperties.getBoolean(PROP_DEFAULT_CALENDAR, defaultState);
    }

    @Override
    public int readUnreadCount() {
        String[] ALERT_PROJECTION = new String[] { CalendarAlerts._ID, // 0
                CalendarAlerts.EVENT_ID, // 1
                CalendarAlerts.STATE, // 2
                CalendarAlerts.TITLE, // 3
                CalendarAlerts.EVENT_LOCATION, // 4
                CalendarAlerts.SELF_ATTENDEE_STATUS, // 5
                CalendarAlerts.ALL_DAY, // 6
                CalendarAlerts.ALARM_TIME, // 7
                CalendarAlerts.MINUTES, // 8
                CalendarAlerts.BEGIN, // 9
                CalendarAlerts.END, // 10
                CalendarAlerts.DESCRIPTION, // 11
        };
        int unreadEvents = 0;

        boolean result = checkPermission();
        if (!result) {
            LogUtils.w(TAG, "no READ_CALENDAR Permission");
            return 0;
        }
        ContentResolver resolver = mContext.getContentResolver();
        final Cursor alertCursor = resolver
                .query(CALENDARS_CONTENT_URI,
                        ALERT_PROJECTION,
                        ("(" + CalendarAlerts.STATE + "=? OR "
                                + CalendarAlerts.STATE + "=?) AND "
                                + CalendarAlerts.ALARM_TIME + "<=" + System
                                .currentTimeMillis()),
                        new String[] {
                                Integer.toString(CalendarAlerts.STATE_FIRED),
                                Integer.toString(CalendarAlerts.STATE_SCHEDULED) },
                        "begin DESC, end DESC");

        if (alertCursor != null) {
            try {
                unreadEvents = alertCursor.getCount();
            } catch (Exception e) {
                // TODO: handle exception
                LogUtils.d(TAG, "readUnreadCount Exception: "+ e);
            } finally {
                UtilitiesExt.closeCursorSilently(alertCursor);
            }
        }

        if(LogUtils.DEBUG_UNREAD) LogUtils.d(TAG, "readUnreadCount, unread Calendar num = "+unreadEvents);

        return unreadEvents;
    }

    @Override
    public String getUnreadHintString() {
        String name = mContext.getString(R.string.unread_calendar);
        return mContext.getString(R.string.unread_hint, name);
    }

    @Override
    public ArrayList<String> loadApps(Context context) {
        String[] calLists = context.getResources().getStringArray(R.array.support_calendar_component_array);

        ArrayList<String> installedCalendarList = new ArrayList<>();
        for (String calList : calLists) {
            ComponentName componentName = ComponentName.unflattenFromString( calList );
            boolean isInstalled = UtilitiesExt.isAppInstalled(context, componentName );
            if (isInstalled) {
                installedCalendarList.add(componentName.flattenToShortString());
            }
        }

        return installedCalendarList;
    }
}
