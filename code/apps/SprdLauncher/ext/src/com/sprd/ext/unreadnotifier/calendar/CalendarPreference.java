package com.sprd.ext.unreadnotifier.calendar;

import android.content.ComponentName;
import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.sprd.ext.unreadnotifier.AppListPreference;
import com.sprd.ext.unreadnotifier.UnreadInfoManager;

import java.util.ArrayList;

/**
 * Created by SPRD on 2/8/17.
 */

public class CalendarPreference extends AppListPreference{
    public CalendarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        item = UnreadInfoManager.getInstance(context).getItemByType(UnreadInfoManager.TYPE_CALENDAR);

        if (item != null) {
            isPermissionGranted = item.checkPermission();
            initState = item.isPersistChecked();

            //get initValue before verifyDefaultCN
            initValue = item.readSavedValues();

            ArrayList<String> listValues = item.loadApps(item.mContext);
            item.setInstalledList(listValues);
            item.verifyDefaultCN(listValues, UnreadCalendarItem.DEFAULT_CNAME);

            String pkgName = TextUtils.isEmpty(item.mCurrentCn) ? null
                    : ComponentName.unflattenFromString(item.mCurrentCn).getPackageName();
            setListValues(listValues, pkgName);
        }
    }

}
