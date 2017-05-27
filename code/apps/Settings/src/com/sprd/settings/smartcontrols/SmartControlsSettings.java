package com.sprd.settings.smartcontrols;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.SearchIndexableResource;
import com.sprd.settings.smartcontrols.SmartWakeAnimation;
import static android.provider.Settings.Global.SMART_WAKE;
import com.android.settings.widget.SmartSwitchPreference;
import com.android.settings.widget.SmartSwitchPreference.OnPreferenceSwitchChangeListener;
import com.android.settings.widget.SmartSwitchPreference.OnViewClickedListener;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.Indexable.SearchIndexProvider;
import com.android.settings.search.SearchIndexableRaw;
import android.os.Bundle;
import android.content.Context;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ContentResolver;
import android.provider.Settings;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.content.Intent;
import android.view.LayoutInflater;
import android.content.Context;
import java.util.ArrayList;
import java.util.List;

public class SmartControlsSettings extends SettingsPreferenceFragment implements Indexable {

    private static final String KEY_SMART_WAKE = "smart_wake";
    private static final String KEY_SMART_MOTION = "smart_motion";
    private static final String KEY_POCKET_MODE = "pocket_mode";
    private static final String DIALOG_TAG = "smart_wake_dialog";

    private static final String TAG = "SmartControlsSettings";
    private SmartSwitchPreference mSmartWakePreference;
    private Preference mSmartMotionPreference;
    private Preference mPocketModePreference;

    public SmartControlsSettings() {
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.SMART_CONTROLS;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.smart_controls_settings);

        if (isSmartWakeAvailable(getResources())) {
            mSmartWakePreference = (SmartSwitchPreference) findPreference(KEY_SMART_WAKE);

            mSmartWakePreference.setOnViewClickedListener(new OnViewClickedListener() {
                @Override
                public void OnViewClicked(View v) {
                    showSmartWakeAnimation();
                }
            });

            mSmartWakePreference.setOnPreferenceSwitchCheckedListener(new OnPreferenceSwitchChangeListener() {
                @Override
                public void onPreferenceSwitchChanged(boolean checked) {
                    Settings.Global.putInt(getContentResolver(), SMART_WAKE, checked ? 1 : 0);
                }
            });
       } else {
            removePreference(KEY_SMART_WAKE);
       }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mSmartWakePreference != null) {
            int value = Settings.Global.getInt(getContentResolver(), SMART_WAKE, 0);
            mSmartWakePreference.setChecked(value != 0);
        }
    }

    private static boolean isSmartWakeAvailable(Resources res) {
        return res.getBoolean(com.android.internal.R.bool.config_supportSmartWake);
    }

    private void showSmartWakeAnimation() {
        final FragmentTransaction ft = getFragmentManager().beginTransaction();
        final Fragment prev = getFragmentManager().findFragmentByTag(DIALOG_TAG);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        final SmartWakeAnimation newFragment = SmartWakeAnimation.newInstance(mSmartWakePreference);
        if (newFragment != null && getActivity().isResumed() && !newFragment.isAdded()) {
            newFragment.show(ft, DIALOG_TAG);
        }
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new  BaseSearchIndexProvider() {

        @Override
        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
            final List<SearchIndexableRaw> result = new ArrayList<SearchIndexableRaw>();
            final Resources res = context.getResources();

            /* SPRD: Bug 537482 device not support smart controls, but can be search @{  */
            if (SettingsActivity.fileIsExists()) {
                SearchIndexableRaw data = new SearchIndexableRaw(context);
                final String screenTitle = res.getString(R.string.smart_controls);
                data.title = screenTitle;
                data.screenTitle = screenTitle;
                result.add(data);
            }
            /* @} */
            return result;
        }
    };
}
