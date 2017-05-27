package com.android.messaging.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;

import java.util.ArrayList;

public class AudioPlayerPolicy {

    private String TAG = "AudioPlayerPolicy";
    private static AudioPlayerPolicy mAudioPlayerPolicy;

    private ArrayList<MediaPlayer> mPlayers = new ArrayList<MediaPlayer>();
    private ArrayList<AudioPlayerPolicyCallback> mCallbacks = new ArrayList<AudioPlayerPolicyCallback>();
    private ArrayList<MediaPlayer> mActivePlayers = new ArrayList<MediaPlayer>();
    private AudioManager mAudioManager;
    private Context mContext;

    public interface AudioPlayerPolicyCallback {
        public void reportResetAudioPlayer(MediaPlayer player);

        public void reportReleaseAudioPlayer(MediaPlayer player);

        public OnAudioFocusChangeListener getOnAudioFocusChangeListener(MediaPlayer player);
    }

    private AudioPlayerPolicy(Context context) {
        mContext = context;
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
    }

    public synchronized static AudioPlayerPolicy get(Context context) {
        if (mAudioPlayerPolicy == null) {
            mAudioPlayerPolicy = new AudioPlayerPolicy(context);
        }
        return mAudioPlayerPolicy;
    }

    public int registerAudioPlayer(MediaPlayer player,
            AudioPlayerPolicyCallback callback) {
        if (mPlayers.indexOf(player) >= 0 || callback == null) {
            Log.d(TAG, "player " + player
                    + " already register or has not callback");
            return mPlayers.indexOf(player);
        }
        Log.d(TAG, "registerAudioPlayer player  " + player);
        return registerAudioPlayerInternal(player, callback);
    }

    private int registerAudioPlayerInternal(MediaPlayer player,
            AudioPlayerPolicyCallback callback) {
        mPlayers.add(player);
        mCallbacks.add(mPlayers.indexOf(player), callback);

        return mPlayers.indexOf(player);
    }

    public boolean unregisterAudioPlayer(MediaPlayer player) {
        if (player == null || mPlayers.indexOf(player) < 0) {
            Log.d(TAG, "unregisterAudioPlayer player is no register");
            return false;
        }
        Log.d(TAG, "unregisterAudioPlayer player  " + player);
        return unregisterAduioPlayerInternal(player);
    }

    private boolean unregisterAduioPlayerInternal(MediaPlayer player) {
        int index = mPlayers.indexOf(player);
        mAudioManager.abandonAudioFocus(mCallbacks.get(index).getOnAudioFocusChangeListener(player));    
        mPlayers.remove(index);
        mCallbacks.remove(index);
        if (mActivePlayers.contains(player)) {
            mActivePlayers.remove(player);
        }

        return true;
    }

    public boolean prepareAudioPlayer(MediaPlayer player) {
        if (player == null || mPlayers.indexOf(player) < 0) {
            Log.d(TAG, "prepare player" + player + " is invalid");
            return false;
        }

        return prepareAudioPlayerInternal(player);
    }

    private boolean prepareAudioPlayerInternal(MediaPlayer player) {
        return true;
    }

    public boolean startAudioPlayer(MediaPlayer player) {
        if (player == null || mPlayers.indexOf(player) < 0) {
            Log.d(TAG, "start player" + player + " is not register");
            return false;
        }
        return startAudioPlayerInternal(player);
    }

    private boolean startAudioPlayerInternal(MediaPlayer player) {
        boolean alreadyStarted = false;
        Log.i(TAG, "players(s) is " + mPlayers.size() + " activeplayers is "
                + mActivePlayers.size() + " callback is " + mCallbacks.size());
        if (!mActivePlayers.isEmpty()) {
            for (int i = 0; i < mActivePlayers.size(); ++i) {
                MediaPlayer p = mActivePlayers.get(i);
                if (p == player) {
                    alreadyStarted = true;
                } else {
                    resetAudioPlayerInternal(p);
                }
            }
        }
        Log.i(TAG, "players(e) is " + mPlayers.size() + " activeplayers is "
                + mActivePlayers.size() + " callback is " + mCallbacks.size());
        if (alreadyStarted) {
            Log.i(TAG, "player " + player + " alreadyStarted");
            return true;
        }
        mActivePlayers.add(player);
        mAudioManager.requestAudioFocus(mCallbacks.get(mPlayers.indexOf(player)).getOnAudioFocusChangeListener(player), AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        return true;
    }

    public boolean stopAudioPlayer(MediaPlayer player) {
        if (player == null || mActivePlayers.indexOf(player) < 0) {
            Log.d(TAG, "stop player" + player + " is not start");
            return false;
        }
        mAudioManager.abandonAudioFocus(mCallbacks.get(mPlayers.indexOf(player)).getOnAudioFocusChangeListener(player));
        return stopAudioPlayerInternal(player);
    }

    private boolean stopAudioPlayerInternal(MediaPlayer player) {
        Log.d(TAG, "stopAudioPlayerInternal player" + player);
        mActivePlayers.remove(player);
        Log.i(TAG, "stopAudioPlayerInternal is " + mPlayers.size() + " activeplayers is "
                + mActivePlayers.size() + " callback is " + mCallbacks.size());
        return true;
    }

    private boolean resetAudioPlayerInternal(MediaPlayer player) {
        int index = mPlayers.indexOf(player);
        Log.d(TAG, "resetAudioPlayerInternal player index " + index);
        AudioPlayerPolicyCallback callback = mCallbacks.get(index);
        callback.reportResetAudioPlayer(player);
        return true;
    }

    private boolean releaseAudioPlayerInternal(MediaPlayer player) {
        int index = mPlayers.indexOf(player);
        Log.d(TAG, "releaseAudioPlayerInternal player index " + index);
        AudioPlayerPolicyCallback callback = mCallbacks.get(index);
        callback.reportReleaseAudioPlayer(player);

        return true;
    }

    public boolean releaseAudioPlayer(int player) {
        MediaPlayer p = mPlayers.get(player);
        if (p == null || !mActivePlayers.contains(p)) {
            Log.i(TAG, " player " + p + " is not register or not started");
            return false;
        }
        return releaseAudioPlayerInternal(p);
    }

    public boolean releaseAllAudioPlayers() {
        if (!mActivePlayers.isEmpty()) {
            for (int i = 0; i < mActivePlayers.size(); ++i) {
                MediaPlayer p = mActivePlayers.get(i);
                releaseAudioPlayerInternal(p);
            }
        }
        mPlayers.clear();
        mActivePlayers.clear();
        mCallbacks.clear();
        return true;
    }
}
