package com.android.phone.plugin;
import com.android.phone.R;
import android.app.AddonManager;

public class CallSettingsPluginsHelper {
    private static CallSettingsPluginsHelper mInstance;

    public CallSettingsPluginsHelper() {
        // TODO Auto-generated constructor stub
    }

    public static CallSettingsPluginsHelper getInstance() {
        if (mInstance != null) {
            return mInstance;
        }
        AddonManager addonManager = AddonManager.getDefault();
        mInstance = (CallSettingsPluginsHelper) addonManager.getAddon(
                R.string.feature_edit_voice_mail_number, CallSettingsPluginsHelper.class);
        return mInstance;
    }

    public boolean isSupportEditVoicemail() {
        return true;
    }
}
