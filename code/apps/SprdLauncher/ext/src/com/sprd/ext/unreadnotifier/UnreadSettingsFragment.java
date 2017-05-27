package com.sprd.ext.unreadnotifier;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.text.TextUtils;
import android.widget.Toast;

import com.android.sprdlauncher3.R;
import com.sprd.ext.LogUtils;
import com.sprd.ext.unreadnotifier.calendar.CalendarPreference;
import com.sprd.ext.unreadnotifier.call.DefaultPhonePreference;
import com.sprd.ext.unreadnotifier.email.EmailPreference;
import com.sprd.ext.unreadnotifier.sms.DefaultSmsPreference;

/**
 * Created by SPRD on 10/21/16.
 */

/**
 * This fragment shows the unread settings preferences.
 */
public class UnreadSettingsFragment extends PreferenceFragment
        implements Preference.OnPreferenceChangeListener,
        AppListPreference.OnPreferenceCheckBoxClickListener{
    private static final String TAG = "UnreadSettingsFragment";
    private Context mContext;
    private PackageManager mPm;

    public static final String PREF_KEY_MISS_CALL = "pref_missed_call_count";
    public static final String PREF_KEY_UNREAD_SMS = "pref_unread_sms_count";
    public static final String PREF_KEY_UNREAD_EMAIL = "pref_unread_email_count";
    public static final String PREF_KEY_UNREAD_CALENDAR = "pref_unread_calendar_count";

    private DefaultPhonePreference mDefaultPhonePref;
    private DefaultSmsPreference mDefaultSmsPref;
    private EmailPreference mEmailPref;
    private CalendarPreference mCalendarPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = getActivity();
        mPm = mContext.getPackageManager();

        UnreadInfoManager.getInstance(mContext).createItemIfNeeded();
        addPreferencesFromResource(R.xml.unread_settings_preferences);

        init();
    }

    @Override
    public void onStop() {
        super.onStop();
        if(LogUtils.DEBUG_UNREAD) {
            LogUtils.d(TAG, "onStop");
        }

        updateUnreadItemInfo(mDefaultPhonePref);
        updateUnreadItemInfo(mDefaultSmsPref);
        updateUnreadItemInfo(mEmailPref);
        updateUnreadItemInfo(mCalendarPref);
    }

    public void updateUnreadItemInfo(AppListPreference preference) {
        if(preference == null) {
            return;
        }
        UnreadBaseItem item = preference.item;
        if(item == null) {
            return;
        }
        //get the initial value and state
        boolean oldState = preference.initState;
        String oldValue = preference.initValue;
        boolean isOldGranted = preference.isPermissionGranted;

        //get the current value and state
        boolean currentState = item.isPersistChecked();
        boolean isCurrentGranted = item.checkPermission();
        ComponentName currentCn = item.getCurrentComponentName();
        String currentValue = null;
        if(currentCn != null) {
            currentValue = currentCn.flattenToShortString();
        }

        boolean isOldEmpty = TextUtils.isEmpty(oldValue);
        boolean isCurrentEmpty = TextUtils.isEmpty(currentValue);

        if(!isCurrentGranted) {
            return;
        }

        if(LogUtils.DEBUG_UNREAD) {
            LogUtils.d(TAG, "item = "+item
                    + ", oldState = "+oldState + ", oldValue = "+oldValue + ", isOldGranted = "+isOldGranted
                    + ",currentState = "+ currentState + ",currentValue = "+ currentValue + ", isCurrentGranted = true.");
        }

        if(currentState != oldState) {
            //checkbox state changed
            if(currentState ) {
                item.mContentObserver.registerContentObserver();
                item.updateUIFromDatabase();
            } else {
                item.mContentObserver.unregisterContentObserver();
                item.setUnreadCount(0);
                UnreadInfoManager.updateUI(mContext, oldValue);
            }
        } else {
            //checkbox state unchanged
            if(currentState) {
                //if oldValue is not empty, clear the unread info on the old icon.
                if(!isOldEmpty && !oldValue.equals(currentValue)) {
                    UnreadInfoManager.updateUI(mContext, oldValue);
                }

                //update the unread info on the new icon.
                if(!isOldGranted) {
                    //from permission denied to permission granted, need to re-register observer
                    item.mContentObserver.registerContentObserver();
                    item.updateUIFromDatabase();
                } else {
                    if((isOldEmpty && !isCurrentEmpty) || (!isOldEmpty && !oldValue.equals(currentValue))) {
                        UnreadInfoManager.updateUI(mContext, currentValue);
                    }
                }
            }
        }

    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference instanceof AppListPreference) {
            ((AppListPreference) preference).item.mCurrentCn = (String)newValue;
            ((AppListPreference) preference).setValue((String)newValue);
            preference.setSummary(((AppListPreference) preference).getEntry());
            return false;
        }
        return true;
    }

    @Override
    public void onPreferenceCheckboxClick(Preference preference) {
        String key = preference.getKey();
        if(LogUtils.DEBUG_UNREAD) {
            LogUtils.d(TAG, "onPreferenceCheckboxClick, key is: "+ key);
        }

        UnreadBaseItem item = UnreadInfoManager.getInstance(mContext).getItemByKey(key);

        if (item != null) {
            if (item.isPersistChecked() && !item.checkPermission()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[] {item.mPermission}, item.mType);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(LogUtils.DEBUG_UNREAD) {
            LogUtils.d(TAG, "onRequestPermissionsResult, requestCode: " + requestCode + ", permissions:" + permissions+ "grantResults: "+grantResults.length);
        }

        UnreadBaseItem item = UnreadInfoManager.getInstance(mContext).getItemByType(requestCode);
        if(item != null) {
            if (grantResults.length == 1) {
                if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(mContext, item.getUnreadHintString(), Toast.LENGTH_LONG).show();
                }
            } else {
                LogUtils.e(TAG, "grantResult length error.");
            }
        }

    }

    private void init() {
        mDefaultPhonePref = (DefaultPhonePreference) findPreference(PREF_KEY_MISS_CALL);
        initPref(mDefaultPhonePref, R.string.pref_missed_call_count_summary);

        mDefaultSmsPref = (DefaultSmsPreference) findPreference(PREF_KEY_UNREAD_SMS);
        initPref(mDefaultSmsPref, R.string.pref_unread_sms_count_summary);

        mEmailPref = (EmailPreference) findPreference(PREF_KEY_UNREAD_EMAIL);
        initPref(mEmailPref, R.string.pref_unread_email_count_summary);

        mCalendarPref = (CalendarPreference) findPreference(PREF_KEY_UNREAD_CALENDAR);
        initPref(mCalendarPref, R.string.pref_unread_calendar_count_summary);
    }

    private boolean hasValidSelectItem(AppListPreference pref) {
        return pref != null && pref.item != null && pref.item.mInstalledList != null
                && !pref.item.mInstalledList.isEmpty();
    }

    private void initPref(Preference pref, int defaultSummaryID) {
        if (pref != null) {
            if (pref instanceof AppListPreference) {
                AppListPreference appListPref = (AppListPreference)pref;
                if (hasValidSelectItem(appListPref)) {
                    appListPref.setOnPreferenceCheckBoxClickListener(this);
                    appListPref.setPreferenceChecked(appListPref.item.isPersistChecked());
                    loadPrefsSetting(appListPref, defaultSummaryID);
                } else {
                    removePref(appListPref);
                    return;
                }
            }
            pref.setOnPreferenceChangeListener(this);
        }
    }

    private void loadPrefsSetting(AppListPreference preference, int defaultSummaryID) {
        if (preference == null) {
            return;
        }

        boolean ret = false;
        ApplicationInfo info = null;
        try {
            UnreadBaseItem item = preference.item;
            String pkgName = ComponentName.unflattenFromString(item.mCurrentCn).getPackageName();
            LogUtils.d(TAG, "loadPrefsSetting preference.mCurrentCn: "+item.mCurrentCn);
            LogUtils.d(TAG, "loadPrefsSetting pkgName: "+pkgName);
            info = mPm.getApplicationInfo(pkgName, 0);
            ret = info != null;
        } catch (Exception e) {
            LogUtils.e(TAG, "loadPrefsSetting failed, e:" + e);
        }

        preference.setSummary(ret ? info.loadLabel(mPm) : getString(defaultSummaryID));
    }

    private void removePref(Preference pref) {
        if (pref != null) {
            getPreferenceScreen().removePreference(pref);
            LogUtils.e(TAG, "preference: " +pref.getTitle()+ " is null, remove it.");
        }
    }
}