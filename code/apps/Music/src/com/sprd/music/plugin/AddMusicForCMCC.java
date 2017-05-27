package com.sprd.music.plugin;

import android.app.AddonManager;
import android.util.Log;

import com.android.music.R;

public class AddMusicForCMCC {
    private static final String LOGTAG = "AddMusicForCMCC";
    static AddMusicForCMCC sInstance;

    public static AddMusicForCMCC getInstance() {
        Log.d(LOGTAG, "sInstance :" + sInstance);
        if (sInstance != null) {
            return sInstance;
        } else {
            sInstance = (AddMusicForCMCC) AddonManager.getDefault().getAddon(R.string.music_cmcc,
                    AddMusicForCMCC.class);
        }
        return sInstance;
    }

    public AddMusicForCMCC() {

    }

    public boolean isCMCCVersion() {
        return false;
    }
}
