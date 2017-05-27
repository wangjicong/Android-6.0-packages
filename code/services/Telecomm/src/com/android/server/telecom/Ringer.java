/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.server.telecom;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemVibrator;
import android.os.UserHandle;
import android.os.Vibrator;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.android.internal.telephony.IRingerService;
import com.android.internal.telephony.TelephonyIntents;

import java.util.LinkedList;
import java.util.List;

/**
 * Controls the ringtone player.
 * TODO: Turn this into a proper state machine: Ringing, CallWaiting, Stopped.
 */
public final class Ringer extends CallsManagerListenerBase {
    private static final long[] VIBRATION_PATTERN = new long[] {
        0, // No delay before starting
        1000, // How long to vibrate
        1000, // How long to wait before vibrating again
    };

    private static final int STATE_RINGING = 1;
    private static final int STATE_CALL_WAITING = 2;
    private static final int STATE_STOPPED = 3;

    private static final AudioAttributes VIBRATION_ATTRIBUTES = new AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .build();

    /** Indicate that we want the pattern to repeat at the step which turns on vibration. */
    private static final int VIBRATION_PATTERN_REPEAT = 1;

    private final AsyncRingtonePlayer mRingtonePlayer;

    /**
     * Used to keep ordering of unanswered incoming calls. There can easily exist multiple incoming
     * calls and explicit ordering is useful for maintaining the proper state of the ringer.
     */
    private final List<Call> mRingingCalls = new LinkedList<>();

    private final CallAudioManager mCallAudioManager;
    private final CallsManager mCallsManager;
    private final InCallTonePlayer.Factory mPlayerFactory;
    private final Context mContext;
    private final Vibrator mVibrator;

    private int mState = STATE_STOPPED;
    private InCallTonePlayer mCallWaitingPlayer;

    /**
     * Used to track the status of {@link #mVibrator} in the case of simultaneous incoming calls.
     */
    private boolean mIsVibrating = false;
    /* SPRD: Fix bug 496670. Move ringtone player to TeleService. @{*/
    private IRingerService mRingerService;
    static final String RINGER_SERVICE_ACTION = "com.android.phone.RingerService";
    /* @} */

    // SPRD: Add for bug 521132, bind ringer service when phone app created
    private BindRingerServiceReciver mBindRingerServiceReciver;

    // SPRD: Fade down ringtone to vibrate.
    private boolean mIsPlayRingtoneOnRingerService = false;

    /** Initializes the Ringer. */
    Ringer(
            CallAudioManager callAudioManager,
            CallsManager callsManager,
            InCallTonePlayer.Factory playerFactory,
            Context context) {

        mCallAudioManager = callAudioManager;
        mCallsManager = callsManager;
        mPlayerFactory = playerFactory;
        mContext = context;
        // We don't rely on getSystemService(Context.VIBRATOR_SERVICE) to make sure this
        // vibrator object will be isolated from others.
        mVibrator = new SystemVibrator(context);
        mRingtonePlayer = new AsyncRingtonePlayer(context);
        /* SPRD: Add for bug 521132, bind ringer service when phone app created @{ */
        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(TelephonyIntents.ACTION_BIND_RINGER_SERVICE);
        mBindRingerServiceReciver = new BindRingerServiceReciver();
        mContext.registerReceiver(mBindRingerServiceReciver, mIntentFilter);
        /* @} */
    }

    @Override
    public void onCallAdded(final Call call) {
        if (call.isIncoming() && call.getState() == CallState.RINGING) {
            if (mRingingCalls.contains(call)) {
                Log.wtf(this, "New ringing call is already in list of unanswered calls");
            }
            mRingingCalls.add(call);
            updateRinging(call);
        }
    }

    @Override
    public void onCallRemoved(Call call) {
        removeFromUnansweredCall(call);
    }

    @Override
    public void onCallStateChanged(Call call, int oldState, int newState) {
        if (newState != CallState.RINGING) {
            removeFromUnansweredCall(call);
        }
    }

    @Override
    public void onIncomingCallAnswered(Call call) {
        onRespondedToIncomingCall(call);
    }

    @Override
    public void onIncomingCallRejected(Call call, boolean rejectWithMessage, String textMessage) {
        onRespondedToIncomingCall(call);
    }

    @Override
    public void onForegroundCallChanged(Call oldForegroundCall, Call newForegroundCall) {
        Call ringingCall = null;
        if (mRingingCalls.contains(newForegroundCall)) {
            ringingCall = newForegroundCall;
        } else if (mRingingCalls.contains(oldForegroundCall)) {
            ringingCall = oldForegroundCall;
        }
        if (ringingCall != null) {
            updateRinging(ringingCall);
        }
    }

    /**
     * Silences the ringer for any actively ringing calls.
     */
    // SPRD: Flip to mute for incoming call.
    public void silence() {
        // Remove all calls from the "ringing" set and then update the ringer.
        mRingingCalls.clear();
        updateRinging(null);
    }

    private void onRespondedToIncomingCall(Call call) {
        // Only stop the ringer if this call is the top-most incoming call.
        if (getTopMostUnansweredCall() == call) {
            removeFromUnansweredCall(call);
        }
    }

    private Call getTopMostUnansweredCall() {
        return mRingingCalls.isEmpty() ? null : mRingingCalls.get(0);
    }

    /**
     * Removes the specified call from the list of unanswered incoming calls and updates the ringer
     * based on the new state of {@link #mRingingCalls}. Safe to call with a call that is not
     * present in the list of incoming calls.
     */
    private void removeFromUnansweredCall(Call call) {
        mRingingCalls.remove(call);
        updateRinging(call);
    }

    private void updateRinging(Call call) {
        if (mRingingCalls.isEmpty()) {
            stopRinging(call, "No more ringing calls found");
            stopCallWaiting(call);
        } else {
            startRingingOrCallWaiting(call);
        }
    }

    private void startRingingOrCallWaiting(Call call) {
        Call foregroundCall = mCallsManager.getForegroundCall();
        Log.v(this, "startRingingOrCallWaiting, foregroundCall: %s.", foregroundCall);

        if (mRingingCalls.contains(foregroundCall)) {
            // The foreground call is one of incoming calls so play the ringer out loud.
            stopCallWaiting(call);

            //SPRD: Request audio focus if has ringing call.
            mCallAudioManager.setIsRinging(call, true);

            if (!shouldRingForContact(foregroundCall.getContactUri())) {
                return;
            }

            AudioManager audioManager =
                    (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
            if (audioManager.getStreamVolume(AudioManager.STREAM_RING) > 0) {
                if (mState != STATE_RINGING) {
                    Log.event(call, Log.Events.START_RINGER);
                    mState = STATE_RINGING;
                }

                // mCallAudioManager.setIsRinging(call, true);
                // Because we wait until a contact info query to complete before processing a
                // call (for the purposes of direct-to-voicemail), the information about custom
                // ringtones should be available by the time this code executes. We can safely
                // request the custom ringtone from the call and expect it to be current.
                /** SPRD: Porting multi-sim ringtone uri. @{
                 * @orig
                 *      mRingtonePlayer.play(foregroundCall.getRingtone());
                 **/
                Uri ringToneUri = foregroundCall.getRingtone();
                if (ringToneUri != null) {
                    /* SPRD: Fix bug 496670. Move ringtone player to TeleService. @{ */
                    if (mRingerService != null) {
                        try {
                            mRingerService.play(ringToneUri);
                            // SPRD: Fade down ringtone to vibrate.
                            mIsPlayRingtoneOnRingerService = true;
                        } catch (RemoteException ignored) {
                            Log.v(this, "startRingingOrCallWaiting, request remote player failed!");
                            mRingtonePlayer.play(ringToneUri);
                            // SPRD: Fade down ringtone to vibrate.
                            mIsPlayRingtoneOnRingerService = false;
                        }
                    } else {
                        mRingtonePlayer.play(ringToneUri);
                        // SPRD: Fade down ringtone to vibrate.
                        mIsPlayRingtoneOnRingerService = false;
                    }
                    /* @} */
                } else {
                    int slotId = TelephonyUtil.getSlotIdForPhoneAccountHandle(foregroundCall.getContext(), foregroundCall.getTargetPhoneAccount());

                    String uri = null;
                    if (TelephonyManager.getDefault().isMultiSimEnabled()) {
                        uri = Settings.System.getString(foregroundCall.getContext().getContentResolver(),
                                Settings.System.RINGTONE + slotId);
                    } else {
                        uri = Settings.System.getString(foregroundCall.getContext().getContentResolver(),
                                Settings.System.RINGTONE);
                    }
                    Log.i(this, "startRingingOrCallWaiting, ringtone uri: %s from phone: %s", ringToneUri, slotId);

                    if (!TextUtils.isEmpty(uri)) {
                        /* SPRD: Fix bug 496670. Move ringtone player to TeleService. @{ */
                        if (mRingerService != null) {
                            try {
                                mRingerService.play(Uri.parse(uri));
                                // SPRD: Fade down ringtone to vibrate.
                                mIsPlayRingtoneOnRingerService = true;
                            } catch (RemoteException ignored) {
                                Log.v(this, "startRingingOrCallWaiting, request remote player failed!");
                                mRingtonePlayer.play(Uri.parse(uri));
                                // SPRD: Fade down ringtone to vibrate.
                                mIsPlayRingtoneOnRingerService = false;
                            }
                        } else {
                            mRingtonePlayer.play(Uri.parse(uri));
                            // SPRD: Fade down ringtone to vibrate.
                            mIsPlayRingtoneOnRingerService = false;
                        }
                        /* @} */
                    }
                }
                /** @} */
            } else {
                Log.v(this, "startRingingOrCallWaiting, skipping because volume is 0");
            }

            if (shouldVibrate(mContext) && !mIsVibrating) {
                mVibrator.vibrate(VIBRATION_PATTERN, VIBRATION_PATTERN_REPEAT,
                        VIBRATION_ATTRIBUTES);
                mIsVibrating = true;
            }
        } else if (foregroundCall != null) {
            // The first incoming call added to Telecom is not a foreground call at this point
            // in time. If the current foreground call is null at point, don't play call-waiting
            // as the call will eventually be promoted to the foreground call and play the
            // ring tone.
            Log.v(this, "Playing call-waiting tone.");

            // All incoming calls are in background so play call waiting.
            stopRinging(call, "Stop for call-waiting");


            if (mState != STATE_CALL_WAITING) {
                Log.event(call, Log.Events.START_CALL_WAITING_TONE);
                mState = STATE_CALL_WAITING;
            }

            if (mCallWaitingPlayer == null) {
                mCallWaitingPlayer =
                        mPlayerFactory.createPlayer(InCallTonePlayer.TONE_CALL_WAITING);
                mCallWaitingPlayer.startTone();
            }
        }
    }

    private boolean shouldRingForContact(Uri contactUri) {
        final NotificationManager manager =
                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        final Bundle extras = new Bundle();
        if (contactUri != null) {
            extras.putStringArray(Notification.EXTRA_PEOPLE, new String[] {contactUri.toString()});
        }
        return manager.matchesCallFilter(extras);
    }

    private void stopRinging(Call call, String reasonTag) {
        if (mState == STATE_RINGING) {
            Log.event(call, Log.Events.STOP_RINGER, reasonTag);
            mState = STATE_STOPPED;
        }

        /* SPRD: Fix bug 496670. Move ringtone player to TeleService. @{ */
        if (mRingerService != null) {
            try {
                mRingerService.stop();
            } catch (RemoteException ignored) {
                Log.v(this, "startRingingOrCallWaiting, request remote player failed!");
                mRingtonePlayer.stop();
            }
        } else {
            mRingtonePlayer.stop();
        }
        /* @} */

        if (mIsVibrating) {
            mVibrator.cancel();
            mIsVibrating = false;
        }

        // Even though stop is asynchronous it's ok to update the audio manager. Things like audio
        // focus are voluntary so releasing focus too early is not detrimental.
        mCallAudioManager.setIsRinging(call, false);
    }

    private void stopCallWaiting(Call call) {
        Log.v(this, "stop call waiting.");
        if (mCallWaitingPlayer != null) {
            mCallWaitingPlayer.stopTone();
            mCallWaitingPlayer = null;
        }

        if (mState == STATE_CALL_WAITING) {
            Log.event(call, Log.Events.STOP_CALL_WAITING_TONE);
            mState = STATE_STOPPED;
        }
    }

    private boolean shouldVibrate(Context context) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int ringerMode = audioManager.getRingerModeInternal();
        if (getVibrateWhenRinging(context)) {
            return ringerMode != AudioManager.RINGER_MODE_SILENT;
        } else {
            return ringerMode == AudioManager.RINGER_MODE_VIBRATE;
        }
    }

    private boolean getVibrateWhenRinging(Context context) {
        if (!mVibrator.hasVibrator()) {
            return false;
        }
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.VIBRATE_WHEN_RINGING, 0) != 0;
    }

    // --------------------------------SPRD----------------------------------------
    /* SPRD: Fix bug 496670. Move ringtone player to TeleService. @{ */
    private class RingerServiceConnection implements ServiceConnection {
        /**
         * {@inheritDoc}
         */
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(this, "onServiceConnected: %s", name);
            onConnected(name, service);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(this, "onDisconnected: %s", name);
            onDisconnected(name);
        }
    }

    private void onConnected(ComponentName componentName, IBinder service) {
        ThreadUtil.checkOnMainThread();
        Log.i(this, "onConnected to %s", componentName);

        mRingerService = IRingerService.Stub.asInterface(service);
        /* SPRD: Add for bug 521132, bind ringer service when phone app created @{ */
        if (mBindRingerServiceReciver != null) {
            mContext.unregisterReceiver(mBindRingerServiceReciver);
            mBindRingerServiceReciver = null;
        }
        /* @} */
    }

    private void onDisconnected(ComponentName componentName) {
        ThreadUtil.checkOnMainThread();
        Log.i(this, "onDisconnected to %s", componentName);

        mRingerService = null;
    }

    private void bindRingerService() {
        RingerServiceConnection ringerConnection = new RingerServiceConnection();
        Intent intent = new Intent(RINGER_SERVICE_ACTION);
        ComponentName componentName = new ComponentName("com.android.phone",
                "com.sprd.phone.RingerService");
        intent.setComponent(componentName);
        mContext.bindServiceAsUser(intent, ringerConnection, Context.BIND_AUTO_CREATE,
                UserHandle.CURRENT);
    }
    /* @} */

    /* SPRD: Add for bug 521132, bind ringer service when phone app created @{ */
    private class BindRingerServiceReciver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // bind ringer service
            if (TelephonyIntents.ACTION_BIND_RINGER_SERVICE.equals(intent.getAction())) {
                bindRingerService();
            }
        }
    }

    /* SPRD: Fade down ringtone to vibrate. @{ */
    public void fadeDownRingtone() {
        if (mIsPlayRingtoneOnRingerService) {
            if (mRingerService != null) {
                try {
                    mRingerService.fadeDownRingtone();
                } catch (RemoteException ignored) {
                    Log.v(this, "fadeDownRingtone, request remote RingerService failed!");
                }
            }
        } else {
            mRingtonePlayer.fadeDownRingtone();
        }
    }
    /* @} */

    /* SPRD: MaxRingingVolume and Vibrate. @{  */
    public void maxRingingVolume() {
        if (mIsPlayRingtoneOnRingerService) {
            if (mRingerService != null) {
                try {
                    mRingerService.maxRingingVolume();
                } catch (RemoteException ignored) {
                    Log.v(this, "maxRingingVolume, request remote RingerService failed!");
                }
            }
        } else {
            mRingtonePlayer.handleMaxRingingVolume();
        }
    }
   /* @} */
}
