
package com.android.settings.accessibility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.view.Gravity;
import android.util.Log;
import android.util.SparseArray;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.widget.SwitchBar;
import com.android.settings.widget.ToggleSwitch;
import com.android.settings.widget.ToggleSwitch.OnBeforeCheckedChangeListener;
import com.android.settings.R;

public class ToggleAssistantFragment extends SettingsPreferenceFragment {

    private static final String TAG = "ToggleAssistantFragment";

    private static final String KEY_SHORTCUT_TOP = "key_shortcut_top";
    private static final String KEY_SHORTCUT_BOTTOM = "key_shortcut_bottom";
    private static final String KEY_SHORTCUT_LEFT = "key_shortcut_left";
    private static final String KEY_SHORTCUT_RIGHT = "key_shortcut_right";

    private static final String KEY_APP_TOP = "key_app_top";
    private static final String KEY_APP_BOTTOM = "key_app_bottom";
    private static final String KEY_APP_LEFT = "key_app_left";
    private static final String KEY_APP_RIGHT = "key_app_right";

    private ListPreference mShortcutTop;
    private ListPreference mShortcutBottom;
    private ListPreference mShortcutLeft;
    private ListPreference mShortcutRight;

    private ListPreference mAppTop;
    private ListPreference mAppBottom;
    private ListPreference mAppLeft;
    private ListPreference mAppRight;

    private PackageManager mPackagemanager;
    private List<ResolveInfo> mApps;
    private AssistantSecure mAssistantSecure;

    private String[] mAppNames;
    private String[] mAppComponentInfo;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPackagemanager = this.getActivity().getPackageManager();

        mAssistantSecure = new AssistantSecure(this.getActivity());

        //installActionBarToggleSwitch();

        addPreferencesFromResource(R.xml.assistant_settings);

        mShortcutTop = (ListPreference) this.findPreference(KEY_SHORTCUT_TOP);
        mShortcutBottom = (ListPreference) this.findPreference(KEY_SHORTCUT_BOTTOM);
        mShortcutLeft = (ListPreference) this.findPreference(KEY_SHORTCUT_LEFT);
        mShortcutRight = (ListPreference) this.findPreference(KEY_SHORTCUT_RIGHT);

        mAppTop = (ListPreference) this.findPreference(KEY_APP_TOP);
        mAppBottom = (ListPreference) this.findPreference(KEY_APP_BOTTOM);
        mAppLeft = (ListPreference) this.findPreference(KEY_APP_LEFT);
        mAppRight = (ListPreference) this.findPreference(KEY_APP_RIGHT);

        mShortcutTop.setOnPreferenceChangeListener(mShortcutChangeListener);
        mShortcutBottom.setOnPreferenceChangeListener(mShortcutChangeListener);
        mShortcutLeft.setOnPreferenceChangeListener(mShortcutChangeListener);
        mShortcutRight.setOnPreferenceChangeListener(mShortcutChangeListener);

        mAppTop.setOnPreferenceChangeListener(mAppChangeListener);
        mAppBottom.setOnPreferenceChangeListener(mAppChangeListener);
        mAppLeft.setOnPreferenceChangeListener(mAppChangeListener);
        mAppRight.setOnPreferenceChangeListener(mAppChangeListener);

    }

    public void onResume() {
        super.onResume();
        installActionBarToggleSwitch();
        updateView();
    }

    public void onPause() {
        super.onPause();
        if (mShortcutTop.getDialog() != null && mShortcutTop.getDialog().isShowing()) {
            mShortcutTop.getDialog().cancel();
        }
        if (mShortcutBottom.getDialog() != null && mShortcutBottom.getDialog().isShowing()) {
            mShortcutBottom.getDialog().cancel();
        }
        if (mShortcutLeft.getDialog() != null && mShortcutLeft.getDialog().isShowing()) {
            mShortcutLeft.getDialog().cancel();
        }
        if (mShortcutRight.getDialog() != null && mShortcutRight.getDialog().isShowing()) {
            mShortcutRight.getDialog().cancel();
        }

        if (mAppTop.getDialog() != null && mAppTop.getDialog().isShowing()) {
            mAppTop.getDialog().cancel();
        }
        if (mAppBottom.getDialog() != null && mAppBottom.getDialog().isShowing()) {
            mAppBottom.getDialog().cancel();
        }
        if (mAppLeft.getDialog() != null && mAppLeft.getDialog().isShowing()) {
            mAppLeft.getDialog().cancel();
        }
        if (mAppRight.getDialog() != null && mAppRight.getDialog().isShowing()) {
            mAppRight.getDialog().cancel();
        }
    }

    private void updateView() {

        mShortcutTop.setValue(mAssistantSecure.getShortcutTopValue() + "");
        mShortcutBottom.setValue(mAssistantSecure.getShortcutBottomValue() + "");
        mShortcutLeft.setValue(mAssistantSecure.getShortcutLeftValue() + "");
        mShortcutRight.setValue(mAssistantSecure.getShortcutRightValue() + "");

        mShortcutTop.setTitle(mShortcutTop.getEntry());
        mShortcutBottom.setTitle(mShortcutBottom.getEntry());
        mShortcutLeft.setTitle(mShortcutLeft.getEntry());
        mShortcutRight.setTitle(mShortcutRight.getEntry());

        final Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        mApps = mPackagemanager.queryIntentActivities(intent, 0);

        mAppNames = new String[mApps.size()];
        mAppComponentInfo = new String[mApps.size()];

        boolean isAppTopExist = false;
        boolean isAppBottomExist = false;
        boolean isAppLeftExist = false;
        boolean isAppRightExist = false;

        int appTopIndex = 0;
        int appBottomIndex = 0;
        int appLeftIndex = 0;
        int appRightIndex = 0;

        for (int i = 0; i < mApps.size(); i++) {

            mAppNames[i] = mApps.get(i).loadLabel(mPackagemanager).toString();
            mAppComponentInfo[i] = mApps.get(i).activityInfo.packageName + ":"
                    + mApps.get(i).activityInfo.name;

            if (mAssistantSecure.getAppTopValue().equals(mAppComponentInfo[i])) {
                isAppTopExist = true;
                appTopIndex = i;
            }
            if (mAssistantSecure.getAppBottomValue().equals(mAppComponentInfo[i])) {
                isAppBottomExist = true;
                appBottomIndex = i;
            }
            if (mAssistantSecure.getAppLeftValue().equals(mAppComponentInfo[i])) {
                isAppLeftExist = true;
                appLeftIndex = i;
            }
            if (mAssistantSecure.getAppRightValue().equals(mAppComponentInfo[i])) {
                isAppRightExist = true;
                appRightIndex = i;
            }

            Log.d(TAG, "app name:" + mAppNames[i] + " info:" + mAppComponentInfo[i]);

        }

        Log.d(TAG, "isAppTopExist:" + isAppTopExist + " isAppBottomExist:" + isAppBottomExist
                + " isAppLeftExist:" + isAppLeftExist + " isAppRightExist:" + isAppRightExist);

        if (!isAppTopExist) {
            mAssistantSecure.setAppTopValue(AssistantSecure.DEFAULT_APP_TOP);
            appTopIndex = getArrayIndex(mAppComponentInfo, AssistantSecure.DEFAULT_APP_TOP);
        }
        if (!isAppBottomExist) {
            mAssistantSecure.setAppBottomValue(AssistantSecure.DEFAULT_APP_BOTTOM);
            appBottomIndex = getArrayIndex(mAppComponentInfo, AssistantSecure.DEFAULT_APP_BOTTOM);
        }
        if (!isAppLeftExist) {
            mAssistantSecure.setAppLeftValue(AssistantSecure.DEFAULT_APP_LEFT);
            appLeftIndex = getArrayIndex(mAppComponentInfo, AssistantSecure.DEFAULT_APP_LEFT);
        }
        if (!isAppRightExist) {
            mAssistantSecure.setAppRightValue(AssistantSecure.DEFAULT_APP_RIGHT);
            appRightIndex = getArrayIndex(mAppComponentInfo, AssistantSecure.DEFAULT_APP_RIGHT);
        }

        mAppTop.setEntries(mAppNames);
        mAppTop.setEntryValues(mAppComponentInfo);
        mAppBottom.setEntries(mAppNames);
        mAppBottom.setEntryValues(mAppComponentInfo);
        mAppLeft.setEntries(mAppNames);
        mAppLeft.setEntryValues(mAppComponentInfo);
        mAppRight.setEntries(mAppNames);
        mAppRight.setEntryValues(mAppComponentInfo);

        mAppTop.setValueIndex(appTopIndex);
        mAppBottom.setValueIndex(appBottomIndex);
        mAppLeft.setValueIndex(appLeftIndex);
        mAppRight.setValueIndex(appRightIndex);

        mAppTop.setTitle(mAppTop.getEntry());
        mAppBottom.setTitle(mAppBottom.getEntry());
        mAppLeft.setTitle(mAppLeft.getEntry());
        mAppRight.setTitle(mAppRight.getEntry());

    }

    private int getArrayIndex(CharSequence[] array, CharSequence ch) {
        for (int i = 0; i < array.length; i++) {
            if (ch.equals(array[i])) {
                return i;
            }
        }
        return 0;
    }

    private void installActionBarToggleSwitch() {
        final boolean enabled = mAssistantSecure.getAssistantStatus() != 0;
        SettingsActivity activity = (SettingsActivity) getActivity();
        SwitchBar switchBar = activity.getSwitchBar();
        switchBar.setCheckedInternal(enabled);
        ToggleSwitch toggleSwitch = switchBar.getSwitch();

        switchBar.show();

        toggleSwitch.setOnBeforeCheckedChangeListener(new OnBeforeCheckedChangeListener() {

            @Override
            public boolean onBeforeCheckedChanged(ToggleSwitch toggleSwitch, boolean checked) {
                toggleSwitch.setCheckedInternal(checked);
                mAssistantSecure.setAssistantStatus(checked ? 1 : 0);
                if (checked) {
                    getActivity().sendBroadcastAsUser(
                            new Intent("com.android.systemui.FLOATKEY_ACTION_START"), UserHandle.ALL);
                } else {
                    getActivity().sendBroadcastAsUser(
                            new Intent("com.android.systemui.FLOATKEY_ACTION_STOP"), UserHandle.ALL);
                }

                return false;
            }
        });
    }

    private OnPreferenceChangeListener mShortcutChangeListener = new OnPreferenceChangeListener() {

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            // TODO Auto-generated method stub
            ListPreference p = (ListPreference) preference;
            String value = (String)newValue;
            CharSequence[] entries = mShortcutTop.getEntries();
            boolean ret = false;
            if (preference == mShortcutTop) {
                mShortcutTop.setTitle(entries[p.findIndexOfValue(value)]);
                mAssistantSecure.setShortcutTopValue(Integer.parseInt(value));
                ret = true;
            }
            if (preference == mShortcutBottom) {
                mShortcutBottom.setTitle(entries[p.findIndexOfValue(value)]);
                mAssistantSecure.setShortcutBottomValue(Integer.parseInt(value));
                ret = true;
            }
            if (preference == mShortcutLeft) {
                mShortcutLeft.setTitle(entries[p.findIndexOfValue(value)]);
                mAssistantSecure.setShortcutLeftValue(Integer.parseInt(value));
                ret = true;
            }
            if (preference == mShortcutRight) {
                mShortcutRight.setTitle(entries[p.findIndexOfValue(value)]);
                mAssistantSecure.setShortcutRightValue(Integer.parseInt(value));
                ret = true;
            }
            if (ret && mAssistantSecure.getAssistantStatus() == 1) {
                getActivity().sendBroadcastAsUser(
                        new Intent("com.android.systemui.FLOATKEY_ACTION_RESTART"), UserHandle.ALL);
            }
            return ret;
        }
    };

    private OnPreferenceChangeListener mAppChangeListener = new OnPreferenceChangeListener() {

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            // TODO Auto-generated method stub
            // CharSequence[] entries = mShortcutTop.getEntries();
            int index = getArrayIndex(mAppComponentInfo, (String) newValue);
            boolean ret = false;

            if (preference == mAppTop) {
                mAppTop.setTitle(mAppNames[index]);
                mAssistantSecure.setAppTopValue(newValue.toString().trim());
                ret = true;
            }
            if (preference == mAppBottom) {
                mAppBottom.setTitle(mAppNames[index]);
                mAssistantSecure.setAppBottomValue(newValue.toString().trim());
                ret = true;
            }
            if (preference == mAppLeft) {
                mAppLeft.setTitle(mAppNames[index]);
                mAssistantSecure.setAppLeftValue(newValue.toString().trim());
                ret = true;
            }
            if (preference == mAppRight) {
                mAppRight.setTitle(mAppNames[index]);
                mAssistantSecure.setAppRightValue(newValue.toString().trim());
                ret = true;
            }
            if (ret && mAssistantSecure.getAssistantStatus() == 1) {
                getActivity().sendBroadcastAsUser(
                        new Intent("com.android.systemui.FLOATKEY_ACTION_RESTART"), UserHandle.ALL);
            }
            return ret;
        }
    };

    protected int getMetricsCategory() {
        return MetricsLogger.TOUCH_ASSISTANT;
    }
}
