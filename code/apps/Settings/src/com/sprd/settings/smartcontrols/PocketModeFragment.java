package com.sprd.settings.smartcontrols;

import com.android.settings.SettingsActivity;
import com.android.settings.widget.SwitchBar;
import com.android.settings.SettingsPreferenceFragment;
import android.widget.Switch;
import com.android.settings.R;
import android.provider.Settings;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.preference.PreferenceScreen;
import com.android.internal.logging.MetricsLogger;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.content.res.Resources;
import static android.provider.Settings.Global.POCKET_MODE_ENABLED;
import static android.provider.Settings.Global.TOUCH_DISABLE;
import static android.provider.Settings.Global.SMART_BELL;
import static android.provider.Settings.Global.POWER_SAVING;

public class PocketModeFragment extends SettingsPreferenceFragment implements
        SwitchBar.OnSwitchChangeListener, Preference.OnPreferenceChangeListener {

    private static final String KEY_TOUCH_DISABLE = "touch_disable";
    private static final String KEY_SMART_BELL = "smart_bell";
    private static final String KEY_POWER_SAVING = "power_saving";
    private static final int DEFAULT_ENABLED = 0;
    private int mControlSwitchBar = 0;

    private SwitchBar mSwitchBar;
    private boolean mValidListener = false;
    private SwitchPreference mTouchDisablePreference;
    private SwitchPreference mSmartBellPreference;
    private SwitchPreference mPowerSavingPreference;

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.POCKET_MODE;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    addPreferencesFromResource(R.xml.pocket_mode);
    initializeAllPreferences();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final SettingsActivity activity = (SettingsActivity) getActivity();
        mSwitchBar = activity.getSwitchBar();
        mSwitchBar.show();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!mValidListener) {
            mSwitchBar.addOnSwitchChangeListener(this);
            mValidListener = true;
        }
        updateState(isPocketModeEnabled());
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mValidListener) {
            mSwitchBar.removeOnSwitchChangeListener(this);
            mValidListener = false;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mSwitchBar.hide();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mTouchDisablePreference) {
            boolean checked = (Boolean) objValue;
            Settings.Global.putInt(getContentResolver(), TOUCH_DISABLE, checked ? 1 : 0);
            mTouchDisablePreference.setChecked(checked);
            updateSwitchBar();
        }

        if (preference == mSmartBellPreference) {
            boolean checked = (Boolean) objValue;
            Settings.Global.putInt(getContentResolver(), SMART_BELL, checked ? 1 : 0);
            mSmartBellPreference.setChecked(checked);
            updateSwitchBar();
        }

        if (preference == mPowerSavingPreference) {
            boolean checked = (Boolean) objValue;
            Settings.Global.putInt(getContentResolver(), POWER_SAVING, checked ? 1 : 0);
            mPowerSavingPreference.setChecked(checked);
            updateSwitchBar();
        }

        return false;
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        Settings.Global.putInt(getContentResolver(), POCKET_MODE_ENABLED, isChecked ? 1 : 0);
        getPreferenceScreen().setEnabled(isChecked);
        if(!isChecked) {
            resetPreference();
        }
    }

    private static boolean isTouchDisableAvailable(Resources res) {
        return res.getBoolean(com.android.internal.R.bool.config_supportTouchDisable);
    }

    private static boolean isSmartBellAvailable(Resources res) {
        return res.getBoolean(com.android.internal.R.bool.config_supportSmartBell);
    }

    private static boolean isPowerSavingAvailable(Resources res) {
        return res.getBoolean(com.android.internal.R.bool.config_supportPowerSaving);
    }

    public final boolean isPocketModeEnabled() {
        return Settings.Global.getInt(getContentResolver(),
                Settings.Global.POCKET_MODE_ENABLED, DEFAULT_ENABLED) == 1;
    }

    private void resetPreference() {
        Settings.Global.putInt(getContentResolver(), TOUCH_DISABLE, 0);
        mTouchDisablePreference.setChecked(false);

        Settings.Global.putInt(getContentResolver(), SMART_BELL, 0);
        mSmartBellPreference.setChecked(false);

        Settings.Global.putInt(getContentResolver(), POWER_SAVING, 0);
        mPowerSavingPreference.setChecked(false);
    }

    private void updateState(boolean isChecked) {
        mSwitchBar.setChecked(isChecked);
        getPreferenceScreen().setEnabled(isChecked);

        if(isChecked) {
            if (mTouchDisablePreference != null) {
                int value = Settings.Global.getInt(getContentResolver(), TOUCH_DISABLE, 0);
                mTouchDisablePreference.setChecked(value != 0);
            }

            if (mSmartBellPreference != null) {
                int value = Settings.Global.getInt(getContentResolver(), SMART_BELL, 0);
                mSmartBellPreference.setChecked(value != 0);
            }

            if (mPowerSavingPreference != null) {
                int value = Settings.Global.getInt(getContentResolver(), POWER_SAVING, 0);
                mPowerSavingPreference.setChecked(value != 0);
            }
        }
    }

    private void updateSwitchBar() {
        if (mSwitchBar != null) {
            int preferenceCount = getPreferenceScreen().getPreferenceCount();
            for (int i = 0; i < preferenceCount; ++i) {
                Preference pref = getPreferenceScreen().getPreference(i);
                if (pref instanceof SwitchPreference) {
                    mControlSwitchBar += ((SwitchPreference)pref).isChecked() ? 1:0;
                }
            }
            mSwitchBar.setChecked(mControlSwitchBar != 0);
            mControlSwitchBar = 0;
        }
    }

    private void initializeAllPreferences() {
        if (isTouchDisableAvailable(getResources())) {
            mTouchDisablePreference = (SwitchPreference) findPreference(KEY_TOUCH_DISABLE);
            mTouchDisablePreference.setOnPreferenceChangeListener(this);
        } else {
            removePreference(KEY_TOUCH_DISABLE);
        }

        if (isSmartBellAvailable(getResources())) {
            mSmartBellPreference = (SwitchPreference) findPreference(KEY_SMART_BELL);
            mSmartBellPreference.setOnPreferenceChangeListener(this);
        } else {
            removePreference(KEY_SMART_BELL);
        }

        if (isPowerSavingAvailable(getResources())) {
            mPowerSavingPreference = (SwitchPreference) findPreference(KEY_POWER_SAVING);
            mPowerSavingPreference.setOnPreferenceChangeListener(this);
        } else {
            removePreference(KEY_POWER_SAVING);
        }
    }
}
