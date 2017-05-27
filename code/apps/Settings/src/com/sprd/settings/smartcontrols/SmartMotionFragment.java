package com.sprd.settings.smartcontrols;

import com.android.settings.SettingsActivity;
import com.android.settings.widget.SwitchBar;
import com.android.settings.SettingsPreferenceFragment;
import android.widget.Switch;
import com.android.settings.R;
import android.provider.Settings;
import com.android.internal.logging.MetricsLogger;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.Preference.OnPreferenceClickListener;
import com.android.settings.widget.SmartSwitchPreference;
import com.android.settings.widget.SmartSwitchPreference.OnPreferenceSwitchChangeListener;
import com.android.settings.widget.SmartSwitchPreference.OnViewClickedListener;

import static android.provider.Settings.Global.SMART_MOTION_ENABLED;
import static android.provider.Settings.Global.EASY_DIAL;
import static android.provider.Settings.Global.EASY_ANSWER;
import static android.provider.Settings.Global.HANDSFREE_SWITCH;
import static android.provider.Settings.Global.SMART_CALL_RECORDER;
import static android.provider.Settings.Global.EASY_BELL;
import static android.provider.Settings.Global.MUTE_INCOMING_CALLS;
import static android.provider.Settings.Global.PLAY_CONTROL;
import static android.provider.Settings.Global.MUSIC_SWITCH;
import static android.provider.Settings.Global.LOCK_MUSIC_SWITCH;
import static android.provider.Settings.Global.EASY_START;
import static android.provider.Settings.Global.MUTE_ALARMS;
import static android.provider.Settings.Global.SHAKE_TO_SWITCH;
import static android.provider.Settings.Global.QUICK_BROWSE;
import static android.provider.Settings.Global.EASY_CLEAR_MEMORY;

public class SmartMotionFragment extends SettingsPreferenceFragment implements
        SwitchBar.OnSwitchChangeListener {

    private static final String KEY_EASY_DIAL = "easy_dial";
    private static final String KEY_EASY_ANSWER = "easy_answer";
    private static final String KEY_HANDSFREE_SWITCH = "handsfree_switch";
    private static final String KEY_SMART_CALL_RECORDER = "smart_call_recorder";
    private static final String KEY_EASY_BELL = "easy_bell";
    private static final String KEY_MUTE_INCOMING_CALLS = "mute_incoming_calls";
    private static final String KEY_PLAY_CONTROL = "play_control";
    private static final String KEY_MUSIC_SWITCH = "music_switch";
    private static final String KEY_LOCK_MUSIC_SWITCH = "lock_music_switch";
    private static final String KEY_EASY_START = "easy_start";
    private static final String KEY_MUTE＿ALARMS = "mute_alarms";
    private static final String KEY_SHAKE_TO_SWITCH = "shake_to_switch";
    private static final String KEY_QUICK_BROWSE = "quick_browse";
    private static final String KEY_EASY_CLEAR_MEMORY = "easy_clear_memory";
    private static final String KEY_SMART_CALL = "smart_call";
    private static final String KEY_SMART_PLAY = "smart_play";
    private static final String KEY_MORE = "more";
    private static final int DEFAULT_ENABLED = 0;

    private SwitchBar mSwitchBar;
    private Switch mSwitch;
    private boolean mValidListener = false;
    private int mControlSwitchBar = 0;
    private SmartSwitchPreference mEasyDialPreference;
    private SmartSwitchPreference mEasyAnswerPreference;
    private SmartSwitchPreference mHandsfreeSwitchPreference;
    private SmartSwitchPreference mSmartCallRecorderPreference;
    private SmartSwitchPreference mEasyBellPreference;
    private SmartSwitchPreference mMuteIncomingCallsPreference;
    private SmartSwitchPreference mPlayControlPreference;
    private SmartSwitchPreference mMusicSwitchPreference;
    private SmartSwitchPreference mLockMusicSwitchPreference;
    private SmartSwitchPreference mEasyStartPreference;
    private SmartSwitchPreference mMuteAlarmsPreference;
    private SmartSwitchPreference mShakeToSwitchPreference;
    private SmartSwitchPreference mQuickBrowsePreference;
    private SmartSwitchPreference mEasyClearMemoryPreference;
    private PreferenceCategory mSmartCallCategory;
    private PreferenceCategory mSmartPlayCategory;
    private PreferenceCategory mMoreCategory;


    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.SMART_MOTION;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.smart_motion);
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
        updateState(isSmartMotionEnabled());
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
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        Settings.Global.putInt(getContentResolver(),
                Settings.Global.SMART_MOTION_ENABLED, isChecked ? 1 : 0);
        getPreferenceScreen().setEnabled(isChecked);
        if(!isChecked) {
            resetPreference();
        }
    }

    private void updateSwitchBar() {
        if (mSwitchBar != null) {
            int countSmartCall = mSmartCallCategory.getPreferenceCount();
            int countSmartPlay = mSmartPlayCategory.getPreferenceCount();
            int countMore = mMoreCategory.getPreferenceCount();

            for(int preferenceCount = 0; preferenceCount < countSmartCall; preferenceCount++) {
                Preference pref = mSmartCallCategory.getPreference(preferenceCount);
                if (pref instanceof SmartSwitchPreference) {
                    mControlSwitchBar += ((SmartSwitchPreference)pref).isChecked() ? 1:0;
                }
            }
            for(int preferenceCount = 0; preferenceCount < countSmartPlay; preferenceCount++) {
                Preference pref = mSmartPlayCategory.getPreference(preferenceCount);
                if (pref instanceof SmartSwitchPreference) {
                    mControlSwitchBar += ((SmartSwitchPreference)pref).isChecked() ? 1:0;
                }
            }
            for(int preferenceCount = 0; preferenceCount < countMore; preferenceCount++) {
                Preference pref = mMoreCategory.getPreference(preferenceCount);
                if (pref instanceof SmartSwitchPreference) {
                    mControlSwitchBar += ((SmartSwitchPreference)pref).isChecked() ? 1:0;
                }
            }
            mSwitchBar.setChecked(mControlSwitchBar != 0);
            mControlSwitchBar = 0;
        }
    }

    private void resetPreference() {
        Settings.Global.putInt(getContentResolver(), EASY_DIAL, 0);
        mEasyDialPreference.setChecked(false);

        Settings.Global.putInt(getContentResolver(), EASY_ANSWER, 0);
        mEasyAnswerPreference.setChecked(false);

        Settings.Global.putInt(getContentResolver(), HANDSFREE_SWITCH, 0);
        mHandsfreeSwitchPreference.setChecked(false);

        Settings.Global.putInt(getContentResolver(), SMART_CALL_RECORDER, 0);
        mSmartCallRecorderPreference.setChecked(false);

        Settings.Global.putInt(getContentResolver(), EASY_BELL, 0);
        mEasyBellPreference.setChecked(false);

        Settings.Global.putInt(getContentResolver(), MUTE_INCOMING_CALLS, 0);
        mMuteIncomingCallsPreference.setChecked(false);

        Settings.Global.putInt(getContentResolver(), PLAY_CONTROL, 0);
        mPlayControlPreference.setChecked(false);

        Settings.Global.putInt(getContentResolver(), MUSIC_SWITCH, 0);
        mMusicSwitchPreference.setChecked(false);

        Settings.Global.putInt(getContentResolver(), LOCK_MUSIC_SWITCH, 0);
        mLockMusicSwitchPreference.setChecked(false);

        Settings.Global.putInt(getContentResolver(), EASY_START, 0);
        mEasyStartPreference.setChecked(false);

        Settings.Global.putInt(getContentResolver(), MUTE_ALARMS, 0);
        mMuteAlarmsPreference.setChecked(false);

        Settings.Global.putInt(getContentResolver(), SHAKE_TO_SWITCH, 0);
        mShakeToSwitchPreference.setChecked(false);

        Settings.Global.putInt(getContentResolver(), QUICK_BROWSE, 0);
        mQuickBrowsePreference.setChecked(false);

        Settings.Global.putInt(getContentResolver(), EASY_CLEAR_MEMORY, 0);
        mEasyClearMemoryPreference.setChecked(false);
    }

    private void updateState(boolean isChecked) {
        mSwitchBar.setChecked(isChecked);
        getPreferenceScreen().setEnabled(isChecked);

        if (isChecked) {
            if (mEasyDialPreference != null) {
                int value = Settings.Global.getInt(getContentResolver(), EASY_DIAL, 0);
                mEasyDialPreference.setChecked(value != 0);
            }
            if (mEasyAnswerPreference != null) {
                int value = Settings.Global.getInt(getContentResolver(), EASY_ANSWER, 0);
                mEasyAnswerPreference.setChecked(value != 0);
            }
            if (mHandsfreeSwitchPreference != null) {
                int value = Settings.Global.getInt(getContentResolver(), HANDSFREE_SWITCH, 0);
                mHandsfreeSwitchPreference.setChecked(value != 0);
            }
            if (mSmartCallRecorderPreference != null) {
                int value = Settings.Global.getInt(getContentResolver(), SMART_CALL_RECORDER, 0);
                mSmartCallRecorderPreference.setChecked(value != 0);
            }
            if (mEasyBellPreference != null) {
                int value = Settings.Global.getInt(getContentResolver(), EASY_BELL, 0);
                mEasyBellPreference.setChecked(value != 0);
            }
            if (mMuteIncomingCallsPreference != null) {
                int value = Settings.Global.getInt(getContentResolver(), MUTE_INCOMING_CALLS, 0);
                mMuteIncomingCallsPreference.setChecked(value != 0);
            }
            if (mPlayControlPreference != null) {
                int value = Settings.Global.getInt(getContentResolver(), PLAY_CONTROL, 0);
                mPlayControlPreference.setChecked(value != 0);
            }
            if (mMusicSwitchPreference != null) {
                int value = Settings.Global.getInt(getContentResolver(), MUSIC_SWITCH, 0);
                mMusicSwitchPreference.setChecked(value != 0);
            }
            if (mLockMusicSwitchPreference != null) {
                int value = Settings.Global.getInt(getContentResolver(), LOCK_MUSIC_SWITCH, 0);
                mLockMusicSwitchPreference.setChecked(value != 0);
            }
            if (mEasyStartPreference != null) {
                int value = Settings.Global.getInt(getContentResolver(), EASY_START, 0);
                mEasyStartPreference.setChecked(value != 0);
            }
            if (mMuteAlarmsPreference != null) {
                int value = Settings.Global.getInt(getContentResolver(), MUTE_ALARMS, 0);
                mMuteAlarmsPreference.setChecked(value != 0);
            }
            if (mShakeToSwitchPreference != null) {
                int value = Settings.Global.getInt(getContentResolver(), SHAKE_TO_SWITCH, 0);
                mShakeToSwitchPreference.setChecked(value != 0);
            }
            if (mQuickBrowsePreference != null) {
                int value = Settings.Global.getInt(getContentResolver(), QUICK_BROWSE, 0);
                mQuickBrowsePreference.setChecked(value != 0);
            }
            if (mEasyClearMemoryPreference != null) {
                int value = Settings.Global.getInt(getContentResolver(), EASY_CLEAR_MEMORY, 0);
                mEasyClearMemoryPreference.setChecked(value != 0);
            }
        }
    }

    private void initializeAllPreferences() {
        mSmartCallCategory = (PreferenceCategory) findPreference(KEY_SMART_CALL);
        mSmartPlayCategory = (PreferenceCategory) findPreference(KEY_SMART_PLAY);
        mMoreCategory = (PreferenceCategory) findPreference(KEY_MORE);

        if (isEasyDialAvailable(getResources())) {
            mEasyDialPreference = (SmartSwitchPreference) findPreference(KEY_EASY_DIAL);
            mEasyDialPreference.setOnViewClickedListener(new OnViewClickedListener() {
                @Override
                public void OnViewClicked(View v) {
                    showEasyDialAnimation();
                }
            });

            mEasyDialPreference.setOnPreferenceSwitchCheckedListener(new OnPreferenceSwitchChangeListener() {
                @Override
                public void onPreferenceSwitchChanged(boolean checked) {
                    Settings.Global.putInt(getContentResolver(), EASY_DIAL, checked ? 1 : 0);
                    mEasyDialPreference.setChecked(checked);
                    updateSwitchBar();
                }
            });
        } else {
            removePreference(KEY_EASY_DIAL);
        }

        if (isEasyAnswerAvailable(getResources())) {
            mEasyAnswerPreference = (SmartSwitchPreference) findPreference(KEY_EASY_ANSWER);
            mEasyAnswerPreference.setOnViewClickedListener(new OnViewClickedListener() {
                @Override
                public void OnViewClicked(View v) {
                    showEasyAnswerAnimation();
                }
            });

            mEasyAnswerPreference.setOnPreferenceSwitchCheckedListener(new OnPreferenceSwitchChangeListener() {
                @Override
                public void onPreferenceSwitchChanged(boolean checked) {
                    Settings.Global.putInt(getContentResolver(), EASY_ANSWER, checked ? 1 : 0);
                    updateSwitchBar();
                }
            });
       } else {
            removePreference(KEY_EASY_ANSWER);
       }

        if (isHandsfreeSwitchAvailable(getResources())) {
            mHandsfreeSwitchPreference = (SmartSwitchPreference) findPreference(KEY_HANDSFREE_SWITCH);
            mHandsfreeSwitchPreference.setOnViewClickedListener(new OnViewClickedListener() {
                @Override
                public void OnViewClicked(View v) {
                    showHandsfreeSwitchAnimation();
                }
            });

            mHandsfreeSwitchPreference.setOnPreferenceSwitchCheckedListener(new OnPreferenceSwitchChangeListener() {
                @Override
                public void onPreferenceSwitchChanged(boolean checked) {
                    Settings.Global.putInt(getContentResolver(), HANDSFREE_SWITCH, checked ? 1 : 0);
                    updateSwitchBar();
                }
            });
       } else {
            removePreference(KEY_HANDSFREE_SWITCH);
       }

        if (isSmartCallRecorderAvailable(getResources())) {
            mSmartCallRecorderPreference = (SmartSwitchPreference) findPreference(KEY_SMART_CALL_RECORDER);
            mSmartCallRecorderPreference.setOnViewClickedListener(new OnViewClickedListener() {
                @Override
                public void OnViewClicked(View v) {
                    showSmartCallRecorderAnimation();
                }
            });

            mSmartCallRecorderPreference.setOnPreferenceSwitchCheckedListener(new OnPreferenceSwitchChangeListener() {
                @Override
                public void onPreferenceSwitchChanged(boolean checked) {
                    Settings.Global.putInt(getContentResolver(), SMART_CALL_RECORDER, checked ? 1 : 0);
                    updateSwitchBar();
                }
            });
       } else {
            removePreference(KEY_SMART_CALL_RECORDER);
       }

        if (isEasyBellAvailable(getResources())) {
            mEasyBellPreference = (SmartSwitchPreference) findPreference(KEY_EASY_BELL);
            mEasyBellPreference.setOnViewClickedListener(new OnViewClickedListener() {
                @Override
                public void OnViewClicked(View v) {
                    showEasyBellAnimation();
                }
            });

            mEasyBellPreference.setOnPreferenceSwitchCheckedListener(new OnPreferenceSwitchChangeListener() {
                @Override
                public void onPreferenceSwitchChanged(boolean checked) {
                    Settings.Global.putInt(getContentResolver(), EASY_BELL, checked ? 1 : 0);
                    updateSwitchBar();
                }
            });
       } else {
            removePreference(KEY_EASY_BELL);
       }

        if (isMuteIncomingCallsAvailable(getResources())) {
            mMuteIncomingCallsPreference = (SmartSwitchPreference) findPreference(KEY_MUTE_INCOMING_CALLS);
            mMuteIncomingCallsPreference.setOnViewClickedListener(new OnViewClickedListener() {
                @Override
                public void OnViewClicked(View v) {
                    showMuteIncomingCallsAnimation();
                }
            });

            mMuteIncomingCallsPreference.setOnPreferenceSwitchCheckedListener(new OnPreferenceSwitchChangeListener() {
                @Override
                public void onPreferenceSwitchChanged(boolean checked) {
                    Settings.Global.putInt(getContentResolver(), MUTE_INCOMING_CALLS, checked ? 1 : 0);
                    updateSwitchBar();
                }
            });
       } else {
            removePreference(KEY_MUTE_INCOMING_CALLS);
       }

        if (isPlayControlAvailable(getResources())) {
            mPlayControlPreference = (SmartSwitchPreference) findPreference(KEY_PLAY_CONTROL);
            mPlayControlPreference.setOnViewClickedListener(new OnViewClickedListener() {
                @Override
                public void OnViewClicked(View v) {
                    showPlayControlAnimation();
                }
            });

            mPlayControlPreference.setOnPreferenceSwitchCheckedListener(new OnPreferenceSwitchChangeListener() {
                @Override
                public void onPreferenceSwitchChanged(boolean checked) {
                    Settings.Global.putInt(getContentResolver(), PLAY_CONTROL, checked ? 1 : 0);
                    updateSwitchBar();
                }
            });
       } else {
            removePreference(KEY_PLAY_CONTROL);
       }

        if (isMusicSwitchAvailable(getResources())) {
            mMusicSwitchPreference = (SmartSwitchPreference) findPreference(KEY_MUSIC_SWITCH);
            mMusicSwitchPreference.setOnViewClickedListener(new OnViewClickedListener() {
                @Override
                public void OnViewClicked(View v) {
                    showMusicSwitchAnimation();
                }
            });

            mMusicSwitchPreference.setOnPreferenceSwitchCheckedListener(new OnPreferenceSwitchChangeListener() {
                @Override
                public void onPreferenceSwitchChanged(boolean checked) {
                    Settings.Global.putInt(getContentResolver(), MUSIC_SWITCH, checked ? 1 : 0);
                    updateSwitchBar();
                }
            });
       } else {
            removePreference(KEY_MUSIC_SWITCH);
       }

        if (isLockMusicSwitchAvailable(getResources())) {
            mLockMusicSwitchPreference = (SmartSwitchPreference) findPreference(KEY_LOCK_MUSIC_SWITCH);
            mLockMusicSwitchPreference.setOnViewClickedListener(new OnViewClickedListener() {
                @Override
                public void OnViewClicked(View v) {
                    showLockMusicSwitchAnimation();
                }
            });

            mLockMusicSwitchPreference.setOnPreferenceSwitchCheckedListener(new OnPreferenceSwitchChangeListener() {
                @Override
                public void onPreferenceSwitchChanged(boolean checked) {
                    Settings.Global.putInt(getContentResolver(), LOCK_MUSIC_SWITCH, checked ? 1 : 0);
                    updateSwitchBar();
                }
            });
       } else {
            removePreference(KEY_LOCK_MUSIC_SWITCH);
       }

        if (isEasyStartAvailable(getResources())) {
            mEasyStartPreference = (SmartSwitchPreference) findPreference(KEY_EASY_START);
            mEasyStartPreference.setOnViewClickedListener(new OnViewClickedListener() {
                @Override
                public void OnViewClicked(View v) {
                    showEasyStartAnimation();
                }
            });

            mEasyStartPreference.setOnPreferenceSwitchCheckedListener(new OnPreferenceSwitchChangeListener() {
                @Override
                public void onPreferenceSwitchChanged(boolean checked) {
                    Settings.Global.putInt(getContentResolver(), EASY_START, checked ? 1 : 0);
                    updateSwitchBar();
                }
            });
       } else {
            removePreference(KEY_EASY_START);
       }

        if (isMuteAlarmsAvailable(getResources())) {
            mMuteAlarmsPreference = (SmartSwitchPreference) findPreference(KEY_MUTE＿ALARMS);
            mMuteAlarmsPreference.setOnViewClickedListener(new OnViewClickedListener() {
                @Override
                public void OnViewClicked(View v) {
                    showMuteAlarmsAnimation();
                }
            });

            mMuteAlarmsPreference.setOnPreferenceSwitchCheckedListener(new OnPreferenceSwitchChangeListener() {
                @Override
                public void onPreferenceSwitchChanged(boolean checked) {
                    Settings.Global.putInt(getContentResolver(), MUTE_ALARMS, checked ? 1 : 0);
                    updateSwitchBar();
                }
            });
       } else {
            removePreference(KEY_MUTE＿ALARMS);
       }

        if (isShakeToSwitchAvailable(getResources())) {
            mShakeToSwitchPreference = (SmartSwitchPreference) findPreference(KEY_SHAKE_TO_SWITCH );
            mShakeToSwitchPreference.setOnViewClickedListener(new OnViewClickedListener() {
                @Override
                public void OnViewClicked(View v) {
                    showShakeToSwitchAnimation();
                }
            });

            mShakeToSwitchPreference.setOnPreferenceSwitchCheckedListener(new OnPreferenceSwitchChangeListener() {
                @Override
                public void onPreferenceSwitchChanged(boolean checked) {
                    Settings.Global.putInt(getContentResolver(), SHAKE_TO_SWITCH, checked ? 1 : 0);
                    updateSwitchBar();
                }
            });
       } else {
            removePreference(KEY_SHAKE_TO_SWITCH);
       }

        if (isQuickBrowseAvailable(getResources())) {
            mQuickBrowsePreference = (SmartSwitchPreference) findPreference(KEY_QUICK_BROWSE );
            mQuickBrowsePreference.setOnViewClickedListener(new OnViewClickedListener() {
                @Override
                public void OnViewClicked(View v) {
                    showQuickBrowseAnimation();
                }
            });

            mQuickBrowsePreference.setOnPreferenceSwitchCheckedListener(new OnPreferenceSwitchChangeListener() {
                @Override
                public void onPreferenceSwitchChanged(boolean checked) {
                    Settings.Global.putInt(getContentResolver(), QUICK_BROWSE, checked ? 1 : 0);
                    updateSwitchBar();
                }
            });
       } else {
            removePreference(KEY_QUICK_BROWSE);
       }

        if (isEasyClearMemoryAvailable(getResources())) {
            mEasyClearMemoryPreference = (SmartSwitchPreference) findPreference(KEY_EASY_CLEAR_MEMORY );
            mEasyClearMemoryPreference.setOnViewClickedListener(new OnViewClickedListener() {
                @Override
                public void OnViewClicked(View v) {
                    showEasyClearMemoryAnimation();
                }
            });

            mEasyClearMemoryPreference.setOnPreferenceSwitchCheckedListener(new OnPreferenceSwitchChangeListener() {
                @Override
                public void onPreferenceSwitchChanged(boolean checked) {
                    Settings.Global.putInt(getContentResolver(), EASY_CLEAR_MEMORY, checked ? 1 : 0);
                    updateSwitchBar();
                }
            });
       } else {
            removePreference(KEY_EASY_CLEAR_MEMORY);
       }
    }

    public final boolean isSmartMotionEnabled() {
        return Settings.Global.getInt(getContentResolver(),
                Settings.Global.SMART_MOTION_ENABLED, DEFAULT_ENABLED) == 1;
    }

    private static boolean isEasyDialAvailable(Resources res) {
        return res.getBoolean(com.android.internal.R.bool.config_supportEasyDial);
    }
    private static boolean isEasyAnswerAvailable(Resources res) {
        return res.getBoolean(com.android.internal.R.bool.config_supportEasyAnswer);
    }
    private static boolean isHandsfreeSwitchAvailable(Resources res) {
        return res.getBoolean(com.android.internal.R.bool.config_supportHandsfreeSwitch);
    }
    private static boolean isSmartCallRecorderAvailable(Resources res) {
        return res.getBoolean(com.android.internal.R.bool.config_supportSmartCallRecorder);
    }
    private static boolean isEasyBellAvailable(Resources res) {
        return res.getBoolean(com.android.internal.R.bool.config_supportEasyBell);
    }
    private static boolean isMuteIncomingCallsAvailable(Resources res) {
        return res.getBoolean(com.android.internal.R.bool.config_supportMuteIncomingCalls);
    }
    private static boolean isPlayControlAvailable(Resources res) {
        return res.getBoolean(com.android.internal.R.bool.config_supportPlayControl);
    }
    private static boolean isMusicSwitchAvailable(Resources res) {
        return res.getBoolean(com.android.internal.R.bool.config_supportMusicSwitch);
    }
    private static boolean isLockMusicSwitchAvailable(Resources res) {
        return res.getBoolean(com.android.internal.R.bool.config_supportLockMusicSwitch);
    }
    private static boolean isEasyStartAvailable(Resources res) {
        return res.getBoolean(com.android.internal.R.bool.config_supportEasyStart);
    }
    private static boolean isMuteAlarmsAvailable(Resources res) {
        return res.getBoolean(com.android.internal.R.bool.config_supportMuteAlarms);
    }
    private static boolean isShakeToSwitchAvailable(Resources res) {
        return res.getBoolean(com.android.internal.R.bool.config_supportShakeToSwitch);
    }
    private static boolean isQuickBrowseAvailable(Resources res) {
        return res.getBoolean(com.android.internal.R.bool.config_supportQuickBrowse);
    }
    private static boolean isEasyClearMemoryAvailable(Resources res) {
        return res.getBoolean(com.android.internal.R.bool.config_supportEasyClearMemory);
    }

    private void showEasyDialAnimation() {
        final FragmentTransaction ft = getFragmentManager().beginTransaction();
        final Fragment prev = getFragmentManager().findFragmentByTag(KEY_EASY_DIAL);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        final EasyDialAnimation newFragment = EasyDialAnimation.newInstance(mEasyDialPreference);
        if (newFragment != null && getActivity().isResumed() && !newFragment.isAdded()) {
            newFragment.show(ft, KEY_EASY_DIAL);
        }
    }

    private void showEasyAnswerAnimation() {
        final FragmentTransaction ft = getFragmentManager().beginTransaction();
        final Fragment prev = getFragmentManager().findFragmentByTag(KEY_EASY_ANSWER);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        final EasyAnswerAnimation newFragment = EasyAnswerAnimation.newInstance(mEasyAnswerPreference);
        if (newFragment != null && getActivity().isResumed() && !newFragment.isAdded()) {
            newFragment.show(ft, KEY_EASY_ANSWER);
        }
    }

    private void showHandsfreeSwitchAnimation() {
        final FragmentTransaction ft = getFragmentManager().beginTransaction();
        final Fragment prev = getFragmentManager().findFragmentByTag(KEY_HANDSFREE_SWITCH);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        final HandsfreeSwitchAnimation newFragment = HandsfreeSwitchAnimation.newInstance(mHandsfreeSwitchPreference);
        if (newFragment != null && getActivity().isResumed() && !newFragment.isAdded()) {
            newFragment.show(ft, KEY_HANDSFREE_SWITCH);
        }
    }

    private void showSmartCallRecorderAnimation() {
        final FragmentTransaction ft = getFragmentManager().beginTransaction();
        final Fragment prev = getFragmentManager().findFragmentByTag(KEY_SMART_CALL_RECORDER);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        final SmartCallRecorderAnimation newFragment = SmartCallRecorderAnimation.newInstance(mSmartCallRecorderPreference);
        if (newFragment != null && getActivity().isResumed() && !newFragment.isAdded()) {
            newFragment.show(ft, KEY_SMART_CALL_RECORDER);
        }
    }

    private void showEasyBellAnimation() {
        final FragmentTransaction ft = getFragmentManager().beginTransaction();
        final Fragment prev = getFragmentManager().findFragmentByTag(KEY_EASY_BELL);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        final EasyBellAnimation newFragment = EasyBellAnimation.newInstance(mEasyBellPreference);
        if (newFragment != null && getActivity().isResumed() && !newFragment.isAdded()) {
            newFragment.show(ft, KEY_EASY_BELL);
        }
    }

    private void showMuteIncomingCallsAnimation() {
        final FragmentTransaction ft = getFragmentManager().beginTransaction();
        final Fragment prev = getFragmentManager().findFragmentByTag(KEY_MUTE_INCOMING_CALLS);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        final MuteIncomingCallsAnimation newFragment = MuteIncomingCallsAnimation.newInstance(mMuteIncomingCallsPreference);
        if (newFragment != null && getActivity().isResumed() && !newFragment.isAdded()) {
            newFragment.show(ft, KEY_MUTE_INCOMING_CALLS);
        }
    }

    private void showPlayControlAnimation() {
        final FragmentTransaction ft = getFragmentManager().beginTransaction();
        final Fragment prev = getFragmentManager().findFragmentByTag(KEY_PLAY_CONTROL);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        final PlayControlAnimation newFragment = PlayControlAnimation.newInstance(mPlayControlPreference);
        if (newFragment != null && getActivity().isResumed() && !newFragment.isAdded()) {
            newFragment.show(ft, KEY_PLAY_CONTROL);
        }
    }

    private void showMusicSwitchAnimation() {
        final FragmentTransaction ft = getFragmentManager().beginTransaction();
        final Fragment prev = getFragmentManager().findFragmentByTag(KEY_MUSIC_SWITCH);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        final MusicSwitchAnimation newFragment = MusicSwitchAnimation.newInstance(mMusicSwitchPreference);
        if (newFragment != null && getActivity().isResumed() && !newFragment.isAdded()) {
            newFragment.show(ft, KEY_MUSIC_SWITCH);
        }
    }

    private void showLockMusicSwitchAnimation() {
        final FragmentTransaction ft = getFragmentManager().beginTransaction();
        final Fragment prev = getFragmentManager().findFragmentByTag(KEY_LOCK_MUSIC_SWITCH);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        final LockMusicSwitchAnimation newFragment = LockMusicSwitchAnimation.newInstance(mLockMusicSwitchPreference);
        if (newFragment != null && getActivity().isResumed() && !newFragment.isAdded()) {
            newFragment.show(ft, KEY_LOCK_MUSIC_SWITCH);
        }
    }

    private void showEasyStartAnimation() {
        final FragmentTransaction ft = getFragmentManager().beginTransaction();
        final Fragment prev = getFragmentManager().findFragmentByTag(KEY_EASY_START);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        final EasyStartAnimation newFragment = EasyStartAnimation.newInstance(mEasyStartPreference);
        if (newFragment != null && getActivity().isResumed() && !newFragment.isAdded()) {
            newFragment.show(ft, KEY_EASY_START);
        }
    }

    private void showMuteAlarmsAnimation() {
        final FragmentTransaction ft = getFragmentManager().beginTransaction();
        final Fragment prev = getFragmentManager().findFragmentByTag(KEY_MUTE＿ALARMS);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        final MuteAlarmsAnimation newFragment = MuteAlarmsAnimation.newInstance(mMuteAlarmsPreference);
        if (newFragment != null && getActivity().isResumed() && !newFragment.isAdded()) {
            newFragment.show(ft, KEY_MUTE＿ALARMS);
        }
    }

    private void showShakeToSwitchAnimation() {
        final FragmentTransaction ft = getFragmentManager().beginTransaction();
        final Fragment prev = getFragmentManager().findFragmentByTag(KEY_SHAKE_TO_SWITCH);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        final ShakeToSwitchAnimation newFragment = ShakeToSwitchAnimation.newInstance(mShakeToSwitchPreference);
        if (newFragment != null && getActivity().isResumed() && !newFragment.isAdded()) {
            newFragment.show(ft, KEY_SHAKE_TO_SWITCH);
        }
    }

    private void showQuickBrowseAnimation() {
        final FragmentTransaction ft = getFragmentManager().beginTransaction();
        final Fragment prev = getFragmentManager().findFragmentByTag(KEY_QUICK_BROWSE);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        final QuickBrowseAnimation newFragment = QuickBrowseAnimation.newInstance(mQuickBrowsePreference);
        if (newFragment != null && getActivity().isResumed() && !newFragment.isAdded()) {
            newFragment.show(ft, KEY_QUICK_BROWSE);
        }
    }

    private void showEasyClearMemoryAnimation() {
        final FragmentTransaction ft = getFragmentManager().beginTransaction();
        final Fragment prev = getFragmentManager().findFragmentByTag(KEY_EASY_CLEAR_MEMORY);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        final EasyClearMemoryAnimation newFragment = EasyClearMemoryAnimation.newInstance(mEasyClearMemoryPreference);
        if (newFragment != null && getActivity().isResumed() && !newFragment.isAdded()) {
            newFragment.show(ft, KEY_EASY_CLEAR_MEMORY);
        }
    }
}
