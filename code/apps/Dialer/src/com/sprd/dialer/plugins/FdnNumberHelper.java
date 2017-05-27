package com.sprd.dialer.plugins;

import com.android.dialer.R;

import android.app.AddonManager;
import android.content.Context;
import android.util.Log;

import java.util.List;
import java.util.ArrayList;

public class FdnNumberHelper {
    private static final String TAG = "FdnNumberHelper";
    private static List<OnFdnCacheRefreshListener> mListeners
            = new ArrayList<OnFdnCacheRefreshListener>();

    public interface OnFdnCacheRefreshListener {
        public void OnFdnCacheRefresh();
    }

    public FdnNumberHelper() {
    }

    static FdnNumberHelper mInstance;

    public static FdnNumberHelper getInstance(Context context) {
        if (mInstance != null) {
            return mInstance;
        }
        AddonManager addonManager = new AddonManager(context);
        mInstance = (FdnNumberHelper) addonManager.getAddon(R.string.fdn_number_plugin_name, FdnNumberHelper.class);
        return mInstance;
    }

    public String queryFdnCache(String number, int subId, String name) {
        return name;
    }

    public void queryFdnList(int subId, Context context) {
        Log.d(TAG, "query FdnList");
    }

    public void refreshFdnListCache(int subId, Context context) {
        Log.d(TAG, "refresh FdnList Cache");
    }

    public String queryFdnCacheForAllSubs(Context context, String number, String name) {
        Log.d(TAG, "queryFdnCacheForAllSubs");
        return name;
    }

    public void addOnFdnCacheRefreshListener(OnFdnCacheRefreshListener listener) {
        Log.d(TAG, "setOnFdnCacheRefreshListener");
        mListeners.add(listener);
    }

    public void removeOnFdnCacheRefreshListener(OnFdnCacheRefreshListener listener) {
        Log.d(TAG, "removeOnFdnCacheRefreshListener");
        mListeners.remove(listener);
    }

    public void notifyAllListeners() {
        for (int i = 0; i < mListeners.size(); i++) {
            mListeners.get(i).OnFdnCacheRefresh();
        }
    }
}
