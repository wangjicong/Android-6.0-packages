package com.sprd.ext.unreadnotifier.sms;

import android.content.ComponentName;
import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.sprd.ext.unreadnotifier.AppListPreference;
import com.sprd.ext.unreadnotifier.UnreadInfoManager;

import java.util.ArrayList;

/**
 * Created by SPRD on 10/20/16.
 */
public class DefaultSmsPreference extends AppListPreference {
    public DefaultSmsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        item = UnreadInfoManager.getInstance(context).getItemByType(UnreadInfoManager.TYPE_SMS);
        if (item != null) {
            isPermissionGranted = item.checkPermission();
            initState = item.isPersistChecked();

            //get initValue before verifyDefaultCN
            initValue = item.readSavedValues();

            ArrayList<String> listValues = item.loadApps(item.mContext);
            item.setInstalledList(listValues);
            item.verifyDefaultCN(listValues, UnreadMessageItem.DEFAULT_CNAME);

            String pkgName = TextUtils.isEmpty(item.mCurrentCn) ? null
                    : ComponentName.unflattenFromString(item.mCurrentCn).getPackageName();
            setListValues(listValues, pkgName);
        }
    }
}