/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.deskclock.alarms;

import android.content.Context;
import android.media.AudioAttributes;
import android.os.Build;
import android.os.Vibrator;
/* SPRD:  bug 499636  @{ */
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.AudioManager;
/* @] */
import com.android.deskclock.AsyncRingtonePlayer;
import com.android.deskclock.LogUtils;
import com.android.deskclock.provider.AlarmInstance;

/**
 * Manages playing ringtone and vibrating the device.
 */
public final class AlarmKlaxon {
    private static final long[] sVibratePattern = {500, 500};

    private static boolean sStarted = false;
    private static AsyncRingtonePlayer sAsyncRingtonePlayer;
    /* SPRD:  bug 499636  registered the OnAudioFouceChangListener for FoucesChange@{ */
    private static AudioManager sAudioManager;
    private static Context sContext;
    public static OnAudioFocusChangeListener mAudioFocusListener = new OnAudioFocusChangeListener() {
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
            case AudioManager.AUDIOFOCUS_LOSS:
                getAsyncRingtonePlayer(sContext).stop();
                break;
            }
        }
    };
    /* @} */
    private AlarmKlaxon() {}

    public static void stop(Context context) {
        LogUtils.v("AlarmKlaxon.stop()");
        // SPRD:  bug 499636 get this context
        sContext = context;
        if (sStarted) {
            sStarted = false;
            getAsyncRingtonePlayer(context).stop();
            ((Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE)).cancel();
            // SPRD:  bug 499636  through OnAudioFouceChangListener abandon AdioFoucus.
            sAudioManager.abandonAudioFocus(mAudioFocusListener);
        }
    }

    public static void start(Context context, AlarmInstance instance) {
        LogUtils.v("AlarmKlaxon.start()");
        // Make sure we are stopped before starting
        // SPRD:  bug 499636 get AudioManager object
        if (sAudioManager == null) {
            sAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        }
        stop(context);

        if (!AlarmInstance.NO_RINGTONE_URI.equals(instance.mRingtone)) {
            // SPRD:  bug 499636  AudioManager get the AdioFoucus @{ */
            sAudioManager.requestAudioFocus(mAudioFocusListener,AudioManager.STREAM_ALARM,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            /* @} */
            getAsyncRingtonePlayer(context).play(instance.mRingtone);
        }

        if (instance.mVibrate) {
            final Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                vibrator.vibrate(sVibratePattern, 0, new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build());
            } else {
                vibrator.vibrate(sVibratePattern, 0);
            }
        }

        sStarted = true;
    }

    private static synchronized AsyncRingtonePlayer getAsyncRingtonePlayer(Context context) {
        if (sAsyncRingtonePlayer == null) {
            sAsyncRingtonePlayer = new AsyncRingtonePlayer(context.getApplicationContext());
        }

        return sAsyncRingtonePlayer;
    }
}
