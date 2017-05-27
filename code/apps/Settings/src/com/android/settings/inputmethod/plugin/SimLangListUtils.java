package com.android.settings.inputmethod.plugin;

import android.app.AddonManager;
import android.app.AlertDialog;
import android.content.Context;
import android.telephony.SubscriptionInfo;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.sim.SimDialogActivity;
import com.android.settings.inputmethod.InputMethodAndLanguageSettings;
import com.android.settings.inputmethod.plugin.SimLangListUtils;

import java.util.List;

public class SimLangListUtils {

    private static String TAG = "SimLangListUtils";
    static SimLangListUtils sInstance;

    private static final String KEY_SIM_STORED_LANG_EDIT = "simcard_stored_lang_edit";
    public static SimLangListUtils getInstance(Context context) {
        if (sInstance != null) return sInstance;
        sInstance = (SimLangListUtils) AddonManager.getDefault().getAddon(R.string.feature_simlanglist, SimLangListUtils.class);
        return sInstance;
    }

    public SimLangListUtils() {
    }

    public void setPreferenceByCardForOrange(InputMethodAndLanguageSettings baseClass){
        if(baseClass.findPreference(KEY_SIM_STORED_LANG_EDIT) != null) {
            baseClass.getPreferenceScreen().removePreference(baseClass.findPreference(KEY_SIM_STORED_LANG_EDIT));
        }
        Log.d(TAG,"setPreferenceByCardForOrange");
    }

    public void startActivityByCards(Context context){
         Log.d(TAG,"startActivityByCards");
    }

    public void setTitle(AlertDialog.Builder builder ,int title){
         Log.d(TAG,"setTitle");
    }
    public void startSimLangListActivity(Context context,int chooseId, List<SubscriptionInfo> subInfoList){
         Log.d(TAG,"startSimLangListActivity");
    }
}