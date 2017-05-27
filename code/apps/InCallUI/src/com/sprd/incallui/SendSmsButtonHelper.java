package com.sprd.incallui;

import com.android.incallui.Call;
import com.android.incallui.Log;
import com.android.incallui.R;
import android.content.Context;
import android.app.AddonManager;

/**
 * Deal send sms button issue
 */
public class SendSmsButtonHelper {
    private static final String TAG = "SendSmsButtonHelper";
    static SendSmsButtonHelper mInstance;

    public static SendSmsButtonHelper getsInstance(Context context) {
        if (mInstance != null) {
            return mInstance;
        }
        AddonManager addonManager = new AddonManager(context);
        mInstance = (SendSmsButtonHelper) addonManager
                .getAddon(R.string.send_sms_button_plugin_name,
                        SendSmsButtonHelper.class);
        Log.i(TAG, "getInstance [" + mInstance + "]");
        return mInstance;
    }

    public SendSmsButtonHelper() {
    }

    public boolean isSupportSendSms() {
        /* SPRD: return false @{ */
        return false;
    }

    public void sendSms(Context context, Call mCall) {
        /* SPRD: Do nothing @{ */
    }

}
