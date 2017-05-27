//
//package com.sprd.engineermode.connectivity.fm;
//
//import com.android.fmradio.FmNative;
//import com.sprd.engineermode.connectivity.fm.FmConstants.*;
//import android.util.Log;
//import android.os.SystemProperties;
//import android.content.Context;
//
//public class FmManagerSelect {
//    private final Context mContext;
//
//    private static final String LOGTAG = "FmManagerSelect";
//    // add for universe_ui_support
//    private static boolean mIsUseBrcmFmChip = SystemProperties
//            .getBoolean("use_brcm_fm_chip", false);
//
//    public FmManagerSelect(Context context) {
//        mContext = context;
//    }
//
//    public boolean powerUp() {
//        if (mIsUseBrcmFmChip) {
//            return mFmManagerForBrcm.powerUp();
//        } else {
//            return mFmManager.powerUp();
//        }
//    }
//
//    public boolean powerDown() {
//        
//            return mFmManager.powerDown();
//    }
//
//    public int searchStation(int freq, SearchDirection direction, int timeout) {
//        if (mIsUseBrcmFmChip) {
//            return mFmManagerForBrcm.searchStation(freq, convertDirectionForBrcm(direction),
//                    timeout);
//        } else {
//            return mFmManager.searchStation(freq, convertDirectionForSprd(direction), timeout);
//        }
//    }
//
//    private FmSearchDirectionForBrcm convertDirectionForBrcm(SearchDirection direction) {
//        if (direction == SearchDirection.FM_SEARCH_UP) {
//            return FmSearchDirectionForBrcm.FM_SEARCH_UP;
//        } else {
//            return FmSearchDirectionForBrcm.FM_SEARCH_DOWN;
//        }
//    }
//
//    private FmSearchDirection convertDirectionForSprd(SearchDirection direction) {
//        if (direction == SearchDirection.FM_SEARCH_UP) {
//            return FmSearchDirection.FM_SEARCH_UP;
//        } else {
//            return FmSearchDirection.FM_SEARCH_DOWN;
//        }
//    }
//
//    public boolean cancelSearch() {
//        if (mIsUseBrcmFmChip) {
//            return mFmManagerForBrcm.cancelSearch();
//        } else {
//            return mFmManager.cancelSearch();
//        }
//    }
//
//    public boolean setFreq(int freq) {
//        if (mIsUseBrcmFmChip) {
//            return mFmManagerForBrcm.setFreq(freq);
//        } else {
//            return mFmManager.setFreq(freq);
//        }
//    }
//
//    public int getFreq() {
//        if (mIsUseBrcmFmChip) {
//            return mFmManagerForBrcm.getFreq();
//        } else {
//            return mFmManager.getFreq();
//        }
//    }
//
//    public boolean setVolume(int volume) {
//        if (mIsUseBrcmFmChip) {
//            return mFmManagerForBrcm.setVolume(volume);
//        } else {
//            return mFmManager.setVolume(volume);
//        }
//    }
//
//    public boolean mute() {
//        if (mIsUseBrcmFmChip) {
//            return mFmManagerForBrcm.mute();
//        } else {
//            return mFmManager.mute();
//        }
//    }
//
//    public boolean unmute() {
//        if (mIsUseBrcmFmChip) {
//            return mFmManagerForBrcm.unmute();
//        } else {
//            return mFmManager.unmute();
//        }
//    }
//
//    public boolean setAudioPath(AudioPath path) {
//        if (mIsUseBrcmFmChip) {
//            return mFmManagerForBrcm.setAudioPath(convertAudioPathForBrcm(path));
//        } else {
//            return mFmManager.setAudioPath(convertAudioPathForSprd(path));
//        }
//    }
//
//    private FmAudioPathForBrcm convertAudioPathForBrcm(AudioPath path) {
//        if (path == AudioPath.FM_AUDIO_PATH_HEADSET) {
//            return FmAudioPathForBrcm.FM_AUDIO_PATH_HEADSET;
//        } else if (path == AudioPath.FM_AUDIO_PATH_SPEAKER) {
//            return FmAudioPathForBrcm.FM_AUDIO_PATH_SPEAKER;
//        } else if (path == AudioPath.FM_AUDIO_PATH_NONE) {
//            return FmAudioPathForBrcm.FM_AUDIO_PATH_NONE;
//        } else {
//            return FmAudioPathForBrcm.FM_AUDIO_PATH_UNKNOWN;
//        }
//    }
//
//    private FmAudioPath convertAudioPathForSprd(AudioPath path) {
//        if (path == AudioPath.FM_AUDIO_PATH_HEADSET) {
//            return FmAudioPath.FM_AUDIO_PATH_HEADSET;
//        } else if (path == AudioPath.FM_AUDIO_PATH_SPEAKER) {
//            return FmAudioPath.FM_AUDIO_PATH_SPEAKER;
//        } else if (path == AudioPath.FM_AUDIO_PATH_NONE) {
//            return FmAudioPath.FM_AUDIO_PATH_NONE;
//        } else {
//            return FmAudioPath.FM_AUDIO_PATH_UNKNOWN;
//        }
//    }
//
//    public boolean isFmOn() {
//        if (mIsUseBrcmFmChip) {
//            return mFmManagerForBrcm.isFmOn();
//        } else {
//            return mFmManager.isFmOn();
//        }
//    }
//
//    public boolean setStepType(StepType type) {
//        if (mIsUseBrcmFmChip) {
//            return mFmManagerForBrcm.setStepType(convertStepTypeForBrcm(type));
//        } else {
//            return mFmManager.setStepType(convertStepTypeForSprd(type));
//        }
//    }
//
//    private FmStepTypeForBrcm convertStepTypeForBrcm(StepType type) {
//        if (type == StepType.FM_STEP_50KHZ) {
//            return FmStepTypeForBrcm.FM_STEP_50KHZ;
//        } else if (type == StepType.FM_STEP_100KHZ) {
//            return FmStepTypeForBrcm.FM_STEP_100KHZ;
//        } else {
//            return FmStepTypeForBrcm.FM_STEP_UNKNOWN;
//        }
//    }
//
//    private FmStepType convertStepTypeForSprd(StepType type) {
//        if (type == StepType.FM_STEP_50KHZ) {
//            return FmStepType.FM_STEP_50KHZ;
//        } else if (type == StepType.FM_STEP_100KHZ) {
//            return FmStepType.FM_STEP_100KHZ;
//        } else {
//            return FmStepType.FM_STEP_UNKNOWN;
//        }
//    }
//}
