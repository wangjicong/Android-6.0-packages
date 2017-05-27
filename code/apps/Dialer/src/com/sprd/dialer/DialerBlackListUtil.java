package com.sprd.dialer;

import android.app.AddonManager;
import android.content.Context;
import android.view.View;
import android.util.Log;

import com.android.dialer.R;

public class DialerBlackListUtil {
    private static final String TAG = "DialerBlackListUtil";

    public DialerBlackListUtil() {
    }

    static DialerBlackListUtil mInstance;


    public static DialerBlackListUtil getInstance(Context context) {
        if (mInstance != null) {
            return mInstance;
        }
        mInstance = (DialerBlackListUtil) AddonManager.getDefault().getAddon(
                R.string.black_list_plugin_name, DialerBlackListUtil.class);

        return mInstance;
    }

    public void addBlackItem(Context context, View view, String number, CharSequence displayName) {
        Log.d(TAG, "addBlackItem,no plugin");
        view.setVisibility(View.GONE);
    }
}
