/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.deskclock;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.telephony.SubscriptionManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import com.android.deskclock.timer.TimerAlertFullScreen;
import com.android.deskclock.timer.TimerReceiver;
import android.util.Log;
import android.os.Vibrator;

/**
 * Play the timer's ringtone. Will continue playing the same alarm until service is stopped.
 */
public class TimerRingService extends Service implements AudioManager.OnAudioFocusChangeListener {

    private boolean mPlaying = false;
    private MediaPlayer mMediaPlayer;
    private TelephonyManager mTelephonyManager[];
    private PhoneStateListener mPhoneStateListener[];
    private int mInitialCallState[];
    // SPRD: Bug 521975,535996 check the callstate of sim card when timer come
    private AudioManager mAudioManager;

    private PhoneStateListener getPhoneStateListener(final int phoneId) {
        PhoneStateListener phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String ignored) {
                // The user might already be in a call when the alarm fires. When
                // we register onCallStateChanged, we get the initial in-call state
                // which kills the alarm. Check against the initial call state so
                // we don't kill the alarm during a call.

                /* SPRD: Bug 521975,535996 check the callstate of sim card when timer come @{
                 * if (state != TelephonyManager.CALL_STATE_IDLE
                        && state != mInitialCallState) {
                    stopSelf();
                }
                 * */
                state = TelephonyManager.getDefault().getCallState(SubscriptionManager.getSubId(phoneId)[0]);
                Log.i(LogUtils.LOGTAG, "PhoneStateListener,onCallStateChanged phoneId:" + phoneId + ", state:"
                        + state + ", mInitialCallState[phoneId]" + mInitialCallState[phoneId]);
                if (state == mInitialCallState[phoneId]) {
                    return;
                } else if (mInitialCallState[phoneId] != TelephonyManager.CALL_STATE_IDLE
                        && state != TelephonyManager.CALL_STATE_IDLE) {
                    mInitialCallState[phoneId] = state;
                    return;
                } else {
                    mInitialCallState[phoneId] = state;
                }
                if (state == TelephonyManager.CALL_STATE_IDLE) {
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (mMediaPlayer != null) {
                        mMediaPlayer.start();
                    } else {
                        play();
                    }
                    // Start the TimerAlertFullScreen activity.
                    Intent timersAlert = new Intent(getApplicationContext(), TimerAlertFullScreen.class);
                    timersAlert.setFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
                    getApplicationContext().startActivity(timersAlert);
                } else if (state != TelephonyManager.CALL_STATE_IDLE) {
                    if (mMediaPlayer != null) {
                        mMediaPlayer.pause();
                    }
                }
            }
        };
        return phoneStateListener;
    }

    private OnAudioFocusChangeListener mAudioFocusListener = new OnAudioFocusChangeListener() {
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN:
                    if (mMediaPlayer != null) {
                        mMediaPlayer.start();
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                case AudioManager.AUDIOFOCUS_LOSS:
                    if (mMediaPlayer != null) {
                        mMediaPlayer.pause();
                    }
                    break;
            }
        }
    };
    /* @} */

    @Override
    public void onCreate() {
        // Listen for incoming calls to kill the alarm.
        /* SPRD: Bug 535996 multi-sim PhoneStateListener for the timer @{
         * @orig
         * mTelephonyManager =
                   (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
           mTelephonyManager.listen(
                   mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
         */
        mTelephonyManager = new TelephonyManager [TelephonyManager.getDefault().getPhoneCount()];
        mPhoneStateListener = new PhoneStateListener [TelephonyManager.getDefault().getPhoneCount()] ;
        for(int i = 0 ; i < TelephonyManager.getDefault().getPhoneCount() ; i++){
            mTelephonyManager [i] = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            mPhoneStateListener [i] = getPhoneStateListener(i);
            mInitialCallState = new int [TelephonyManager.getDefault().getPhoneCount()];
        }
        for (int i = 0; i < TelephonyManager.getDefault().getPhoneCount(); i++) {
            mTelephonyManager[i].listen(mPhoneStateListener[i], PhoneStateListener.LISTEN_CALL_STATE);
        }
        /* @} */

        AlarmAlertWakeLock.acquireScreenCpuWakeLock(this);
    }

    @Override
    public void onDestroy() {
        stop();
        // Stop listening for incoming calls.
        /* SPRD: Bug 535996 multi-sim PhoneStateListener for the timer @{
         * @orig
         * mTelephonyManager.listen(mPhoneStateListener, 0);
         */
        for(int i = 0 ; i < TelephonyManager.getDefault().getPhoneCount() ; i++){
            mTelephonyManager [i].listen(mPhoneStateListener [i], PhoneStateListener.LISTEN_NONE);
        }
        /* @} */

        AlarmAlertWakeLock.releaseCpuLock();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // No intent, tell the system not to restart us.
        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        play();
        // Record the initial call state here so that the new alarm has the newest state.
        /* SPRD: Bug 535996 multi-sim PhoneStateListener for the timer @{
         * @orig
         * mInitialCallState = mTelephonyManager.getCallState();
         */
        for (int i = 0; i < TelephonyManager.getDefault().getPhoneCount(); i++) {
            mInitialCallState[i] = TelephonyManager.getDefault().getCallState(SubscriptionManager.getSubId(i)[0]);
        }
        /* @} */

        return START_STICKY;
    }

    // Volume suggested by media team for in-call alarms.
    private static final float IN_CALL_VOLUME = 0.125f;

    private void play() {

        if(isInCall()) {
            // SPRD: Bug 565700 Alarm does not notify(not ringing )during active call
            Utils.showTimesUpNotifications(this);
            return;
        }

        if (mPlaying) {
            return;
        }

        LogUtils.v("TimerRingService.play()");

        // TODO: Reuse mMediaPlayer instead of creating a new one and/or use
        // RingtoneManager.
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnErrorListener(new OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                LogUtils.e("Error occurred while playing audio.");
                mp.stop();
                mp.release();
                mMediaPlayer = null;
                return true;
            }
        });

        try {
            // Check if we are in a call. If we are, use the in-call alarm
            // resource at a low volume to not disrupt the call.
            if (isInCall()) {
                LogUtils.v("Using the in-call alarm");
                mMediaPlayer.setVolume(IN_CALL_VOLUME, IN_CALL_VOLUME);
                setDataSourceFromResource(getResources(), mMediaPlayer,
                        R.raw.in_call_alarm);
            } else {
                AssetFileDescriptor afd = getAssets().openFd("sounds/Timer_Expire.ogg");
                mMediaPlayer.setDataSource(
                        afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            }
            startAlarm(mMediaPlayer);
        } catch (Exception ex) {
            LogUtils.v("Using the fallback ringtone");
            // The alert may be on the sd card which could be busy right
            // now. Use the fallback ringtone.
            try {
                // Must reset the media player to clear the error state.
                mMediaPlayer.reset();
                setDataSourceFromResource(getResources(), mMediaPlayer,
                        R.raw.fallbackring);
                startAlarm(mMediaPlayer);
            } catch (Exception ex2) {
                // At this point we just don't play anything.
                LogUtils.e("Failed to play fallback ringtone", ex2);
            }
        }

        mPlaying = true;
    }

    // Do the common stuff when starting the alarm.
    private void startAlarm(MediaPlayer player)
            throws java.io.IOException, IllegalArgumentException,
                   IllegalStateException {
        final AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        // do not play alarms if stream volume is 0
        // (typically because ringer mode is silent).
        if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
            player.setAudioStreamType(AudioManager.STREAM_ALARM);
            player.setLooping(true);
            player.prepare();
            audioManager.requestAudioFocus(
                    mAudioFocusListener, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            player.start();
        }
    }

    private void setDataSourceFromResource(Resources resources,
            MediaPlayer player, int res) throws java.io.IOException {
        AssetFileDescriptor afd = resources.openRawResourceFd(res);
        if (afd != null) {
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(),
                    afd.getLength());
            afd.close();
        }
    }

    /**
     * Stops timer audio
     */
    public void stop() {
        LogUtils.v("TimerRingService.stop()");
        if (mPlaying) {
            mPlaying = false;

            // Stop audio playing
            if (mMediaPlayer != null) {
                mMediaPlayer.stop();
                final AudioManager audioManager =
                        (AudioManager)getSystemService(Context.AUDIO_SERVICE);
                audioManager.abandonAudioFocus(mAudioFocusListener);
                mMediaPlayer.release();
                mMediaPlayer = null;
            }
        }
    }


    @Override
    public void onAudioFocusChange(int focusChange) {
        // Do nothing
    }

    /* SPRD: Bug 535996 multi-sim PhoneStateListener for the timer @{ */
    private boolean isInCall() {
        boolean isInCall = false;
        for (int i = 0; i < TelephonyManager.getDefault().getPhoneCount(); i++) {
            if (TelephonyManager.getDefault().getCallState(SubscriptionManager.getSubId(i)[0]) != TelephonyManager.CALL_STATE_IDLE) {
                isInCall = true;
                break;
            }
        }
        return isInCall;
    }
    /* @} */
}
