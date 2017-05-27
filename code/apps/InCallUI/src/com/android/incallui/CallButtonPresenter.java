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
 * limitations under the License
 */

package com.android.incallui;

import static com.android.incallui.CallButtonFragment.Buttons.*;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Vibrator;
import android.telecom.CallAudioState;
import android.telecom.InCallService.VideoCall;
import android.telecom.VideoProfile;

import com.android.dialer.DialerApplication;
import com.android.dialer.DialtactsActivity;
import com.android.incallui.AudioModeProvider.AudioModeListener;
import com.android.incallui.InCallCameraManager.Listener;
import com.android.incallui.InCallCameraManager.CameraPauseStateListener;
import com.android.incallui.InCallPresenter.CanAddCallListener;
import com.android.incallui.InCallPresenter.InCallState;
import com.android.incallui.InCallPresenter.InCallStateListener;
import com.android.incallui.InCallPresenter.IncomingCallListener;
import com.android.incallui.InCallPresenter.InCallDetailsListener;

import java.util.Objects;
import com.android.incallui.InCallVideoCallCallbackNotifier.CameraEventListener;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import com.android.ims.ImsManager;
import android.content.Intent;
import com.sprd.incallui.ExplicitCallTransferPluginHelper;
import com.sprd.incallui.SendSmsButtonHelper;

import android.widget.Toast;

import static android.Manifest.permission.READ_PHONE_STATE;
import android.content.pm.PackageManager;

import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.telephony.VoLteServiceState;
/**
 * Logic for call buttons.
 */
public class CallButtonPresenter extends Presenter<CallButtonPresenter.CallButtonUi>
        implements InCallStateListener, AudioModeListener, IncomingCallListener,
        InCallDetailsListener, CanAddCallListener, Listener ,CameraEventListener, CameraPauseStateListener {//SPRD:add CameraEventListener for bug493880 and 544111

    private static final String KEY_AUTOMATICALLY_MUTED = "incall_key_automatically_muted";
    private static final String KEY_PREVIOUS_MUTE_STATE = "incall_key_previous_mute_state";

    private Call mCall;
    private boolean mAutomaticallyMuted = false;
    private boolean mPreviousMuteState = false;

    /* SPRD: AUTOMATIC RECORD FEATURE. @{ */
    private static final String AUTOMATIC_RECORDING_PREFERENCES_NAME = "automatic_recording_key";
    private boolean mAutomaticRecording;
    private boolean mIsAutomaticRecordingStart;
    /* @} */
    /* SPRD: vibration feedback for call connection. See bug#505177 && bug525650 @{ */
    private static final String VIBRATION_FEEDBACK_PREFERENCES_NAME = "call_connection_prompt_key";
    private boolean mVibrateForCallConnection;
    private int mVibrationDuration = 100;
    private String mOldCallId;
    private int mOldCallState;
    /* @} */
    /* SPRD: Add for Volte @{ */
    private static final String MULTI_PICK_CONTACTS_ACTION = "com.android.contacts.action.MULTI_TAB_PICK";
    private static final String ADD_MULTI_CALL_AGAIN = "addMultiCallAgain";
    private static final int MAX_GROUP_CALL_NUMBER = 5;
    private static final int MIN_CONTACTS_NUMBER = 1;
    private boolean isVolteEnable;
    /* @}*/
    /* sprd: add PhoneStateListener for bug535008 { */
    private boolean mStayVolte;
    private TelephonyManager mTelephonyManager;
    private PhoneStateListener mLtePhoneStateListener;
    /* } */

    public CallButtonPresenter() {
    }

    @Override
    public void onUiReady(CallButtonUi ui) {
        super.onUiReady(ui);

        AudioModeProvider.getInstance().addListener(this);

        // register for call state changes last
        final InCallPresenter inCallPresenter = InCallPresenter.getInstance();
        inCallPresenter.addListener(this);
        inCallPresenter.addIncomingCallListener(this);
        inCallPresenter.addDetailsListener(this);
        inCallPresenter.addCanAddCallListener(this);
        inCallPresenter.getInCallCameraManager().addCameraSelectionListener(this);
        inCallPresenter.getInCallCameraManager().addCameraPauseStateListener(this);//SPRD: Add for bug544111
        InCallVideoCallCallbackNotifier.getInstance().addCameraEventListener(this);//SPRD: add for bug493880
        // Update the buttons state immediately for the current call
        onStateChange(InCallState.NO_CALLS, inCallPresenter.getInCallState(),
                CallList.getInstance());
    }

    @Override
    public void onUiUnready(CallButtonUi ui) {
        super.onUiUnready(ui);

        InCallPresenter.getInstance().removeListener(this);
        AudioModeProvider.getInstance().removeListener(this);
        InCallPresenter.getInstance().removeIncomingCallListener(this);
        InCallPresenter.getInstance().removeDetailsListener(this);
        InCallPresenter.getInstance().getInCallCameraManager().removeCameraSelectionListener(this);
        InCallPresenter.getInstance().getInCallCameraManager().removeCameraPauseStateListener(this);//SPRD: Add for bug544111
        InCallPresenter.getInstance().removeCanAddCallListener(this);
        InCallVideoCallCallbackNotifier.getInstance().removeCameraEventListener(this);//SPRD: add for bug493880
    }

    @Override
    public void onStateChange(InCallState oldState, InCallState newState, CallList callList) {
        CallButtonUi ui = getUi();
        /* SPRD: Add for Volte @{ */
        if(ui != null){
            /* SPRD: fix bug 532464 @{ */
            if (getUi().getContext()!=null && getUi().getContext().checkSelfPermission(READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                isVolteEnable = ImsManager.isVolteEnabledByPlatform(getUi().getContext());
            }
            /* @} */
         }
        /* @}*/
        /* SPRD: AUTOMATIC RECORD FEATURE. @{ */
        Context context = ui.getContext().getApplicationContext();
        /* SPRD: Add for bug535008 @{ */
        if (context != null) {
            mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        }
        /* @} */
        DialerApplication dialerApplication = (DialerApplication) context;
        mIsAutomaticRecordingStart = dialerApplication.getIsAutomaticRecordingStart();
        final SharedPreferences sp = context.getSharedPreferences(
                DialtactsActivity.SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        mAutomaticRecording = sp.getBoolean(
                AUTOMATIC_RECORDING_PREFERENCES_NAME, false);
        /* @} */

        // SPRD: vibration feedback for call connection. See bug#505177
        mVibrateForCallConnection = sp.getBoolean(
                VIBRATION_FEEDBACK_PREFERENCES_NAME, false);

        if (newState == InCallState.OUTGOING) {
            mCall = callList.getOutgoingCall();
        } else if (newState == InCallState.INCALL) {
            mCall = callList.getActiveOrBackgroundCall();

            // When connected to voice mail, automatically shows the dialpad.
            // (On previous releases we showed it when in-call shows up, before waiting for
            // OUTGOING.  We may want to do that once we start showing "Voice mail" label on
            // the dialpad too.)
            if (ui != null) {
                if (oldState == InCallState.OUTGOING && mCall != null) {
                    if (CallerInfoUtils.isVoiceMailNumber(ui.getContext(), mCall)) {
                        ui.displayDialpad(true /* show */, true /* animate */);
                    }
                }
            }
        } else if (newState == InCallState.INCOMING) {
            if (ui != null) {
                ui.displayDialpad(false /* show */, true /* animate */);
            }
            mCall = callList.getIncomingCall();
        } else {
            mCall = null;
        }
        updateUi(newState, mCall);
        /* SPRD: AUTOMATIC RECORD FEATURE. @{
        * Using function toggleRecorder for triggering the automatic recording only once on the following conditions:
        * 1) mAutomaticRecording is true :Automatic recording switch to open In the general setting before dialing.
        * 2) Call State is ACTIVE.
        * mIsAutomaticRecordingStart is used for identifying automatic recording started or not
        * TODO: When we cancel recording in first call and add a new call ,the new call will not trigger automatic recording currently.
        * */
        if (mAutomaticRecording && !mIsAutomaticRecordingStart
                && mCall != null && mCall.getState() == Call.State.ACTIVE) {
            // SPRD: request permissions for access storage
            getUi().toggleRecord();
            dialerApplication.setIsAutomaticRecordingStart(true);
        }
        if (newState == InCallState.NO_CALLS) {
            dialerApplication.setIsAutomaticRecordingStart(false);
        }
        /* @} */

        /* SPRD: vibration feedback for call connection. See bug#505177 && bug525650 @{ */
        if (mVibrateForCallConnection
                && mCall != null
                && mCall.getState() == Call.State.ACTIVE
                && oldState == InCallState.OUTGOING
                && newState == InCallState.INCALL
                && mOldCallState == Call.State.DIALING && mCall.getId() == mOldCallId) {
            Vibrator vibrator = (Vibrator) context
                    .getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(mVibrationDuration);
            Log.d(this, "vibrate for call connection...");
        }
        if (mCall != null) {
            mOldCallId = mCall.getId();
            mOldCallState = mCall.getState();
        }
        /* @} */
    }

    /**
     * Updates the user interface in response to a change in the details of a call.
     * Currently handles changes to the call buttons in response to a change in the details for a
     * call.  This is important to ensure changes to the active call are reflected in the available
     * buttons.
     *
     * @param call The active call.
     * @param details The call details.
     */
    @Override
    public void onDetailsChanged(Call call, android.telecom.Call.Details details) {
        // Only update if the changes are for the currently active call
        if (getUi() != null && call != null && call.equals(mCall)) {
            updateButtonsState(call);
        }
    }

    @Override
    public void onIncomingCall(InCallState oldState, InCallState newState, Call call) {
        onStateChange(oldState, newState, CallList.getInstance());
    }

    @Override
    public void onCanAddCallChanged(boolean canAddCall) {
        if (getUi() != null && mCall != null) {
            updateButtonsState(mCall);
        }
    }

    @Override
    public void onAudioMode(int mode) {
        if (getUi() != null) {
            getUi().setAudio(mode);
        }
    }

    @Override
    public void onSupportedAudioMode(int mask) {
        if (getUi() != null) {
            getUi().setSupportedAudio(mask);
        }
    }

    @Override
    public void onMute(boolean muted) {
        if (getUi() != null && !mAutomaticallyMuted) {
            getUi().setMute(muted);
        }
    }

    public int getAudioMode() {
        return AudioModeProvider.getInstance().getAudioMode();
    }

    public int getSupportedAudio() {
        return AudioModeProvider.getInstance().getSupportedModes();
    }

    public void setAudioMode(int mode) {

        // TODO: Set a intermediate state in this presenter until we get
        // an update for onAudioMode().  This will make UI response immediate
        // if it turns out to be slow

        Log.d(this, "Sending new Audio Mode: " + CallAudioState.audioRouteToString(mode));
        TelecomAdapter.getInstance().setAudioRoute(mode);
    }

    /**
     * Function assumes that bluetooth is not supported.
     */
    public void toggleSpeakerphone() {
        // this function should not be called if bluetooth is available
        if (0 != (CallAudioState.ROUTE_BLUETOOTH & getSupportedAudio())) {

            // It's clear the UI is wrong, so update the supported mode once again.
            Log.e(this, "toggling speakerphone not allowed when bluetooth supported.");
            getUi().setSupportedAudio(getSupportedAudio());
            return;
        }

        int newMode = CallAudioState.ROUTE_SPEAKER;

        // if speakerphone is already on, change to wired/earpiece
        if (getAudioMode() == CallAudioState.ROUTE_SPEAKER) {
            newMode = CallAudioState.ROUTE_WIRED_OR_EARPIECE;
        }

        setAudioMode(newMode);
    }

    public void muteClicked(boolean checked) {
        Log.d(this, "turning on mute: " + checked);
        TelecomAdapter.getInstance().mute(checked);
    }

    public void holdClicked(boolean checked) {
        if (mCall == null) {
            return;
        }
        /* SPRD:bug525089 @{ */
        if (isWaitForRespose()) {
            return;
        }/* @} */

        if (checked) {
            Log.i(this, "Putting the call on hold: " + mCall);
            TelecomAdapter.getInstance().holdCall(mCall.getId());
        } else {
            Log.i(this, "Removing the call from hold: " + mCall);
            TelecomAdapter.getInstance().unholdCall(mCall.getId());
        }
    }

    /* SPRD: Add for Volte @{ */
    public void inviteClicked() {
        Log.i(this, "inviteClicked");
        final CallButtonUi ui = getUi();
        Intent intentPick = new Intent(MULTI_PICK_CONTACTS_ACTION).
                putExtra("checked_limit_count",MAX_GROUP_CALL_NUMBER - CallList.getInstance().getConferenceCallSize()).
                putExtra("checked_min_limit_count", MIN_CONTACTS_NUMBER).
                putExtra("cascading",new Intent(MULTI_PICK_CONTACTS_ACTION).setType(Phone.CONTENT_ITEM_TYPE)).
                putExtra("multi",ADD_MULTI_CALL_AGAIN);;
        ui.getContext().startActivity(intentPick);
    }
    /* @}*/

    public void swapClicked() {
        if (mCall == null) {
            return;
        }

        Log.i(this, "Swapping the call: " + mCall);
        TelecomAdapter.getInstance().swap(mCall.getId());
    }

    public void mergeClicked() {
        TelecomAdapter.getInstance().merge(mCall.getId());
    }

    public void addCallClicked() {

        /* SPRD:bug525089 @{ */
        if (mCall == null || isWaitForRespose()) {
            return;
        }/* @} */

        // Automatically mute the current call
        mAutomaticallyMuted = true;
        mPreviousMuteState = AudioModeProvider.getInstance().getMute();
        // Simulate a click on the mute button
        muteClicked(true);
        TelecomAdapter.getInstance().addCall();
    }

    public void changeToVoiceClicked() {
        VideoCall videoCall = mCall.getVideoCall();
        if (videoCall == null) {
            return;
        }

        VideoProfile videoProfile = new VideoProfile(
                VideoProfile.STATE_AUDIO_ONLY, VideoProfile.QUALITY_DEFAULT);
        videoCall.sendSessionModifyRequest(videoProfile);
    }

    public void showDialpadClicked(boolean checked) {
        Log.v(this, "Show dialpad " + String.valueOf(checked));
        getUi().displayDialpad(checked /* show */, true /* animate */);
    }

    public void changeToVideoClicked() {
        /* SPRD:bug525089 and bug541324@{ */
        if (mCall == null || isWaitForRespose()) {
            return;
        }/* @} */

        VideoCall videoCall = mCall.getVideoCall();
        if (videoCall == null) {
            return;
        }

        int currVideoState = mCall.getVideoState();
        int currUnpausedVideoState = CallUtils.getUnPausedVideoState(currVideoState);
        currUnpausedVideoState |= VideoProfile.STATE_BIDIRECTIONAL;

        VideoProfile videoProfile = new VideoProfile(currUnpausedVideoState);
        videoCall.sendSessionModifyRequest(videoProfile);
        mCall.setSessionModificationState(Call.SessionModificationState.WAITING_FOR_RESPONSE);
    }

    /*SPRD: add for VoLTE{@*/
    public void changeToAudioClicked() {
        if (mCall == null) {
            return;
        }
        VideoCall videoCall = mCall.getVideoCall();
        if (videoCall == null) {
            return;
        }
        VideoProfile videoProfile = new VideoProfile(VideoProfile.STATE_AUDIO_ONLY, VideoProfile.QUALITY_DEFAULT);
        videoCall.sendSessionModifyRequest(videoProfile);
    }
    /* @} */

    /**
     * Switches the camera between the front-facing and back-facing camera.
     * @param useFrontFacingCamera True if we should switch to using the front-facing camera, or
     *     false if we should switch to using the back-facing camera.
     */
    public void switchCameraClicked(boolean useFrontFacingCamera) {
        InCallCameraManager cameraManager = InCallPresenter.getInstance().getInCallCameraManager();
        cameraManager.setUseFrontFacingCamera(useFrontFacingCamera);
        /*SPRD: add for 523771{@*/
        if (mCall == null) {
            return;
        }
        /* @} */
        VideoCall videoCall = mCall.getVideoCall();
        if (videoCall == null) {
            return;
        }
        /* SPRD:Add for bug531878 @{ */
        if (mCall.getState()!=Call.State.ACTIVE && getUi()!=null) {
            Toast.makeText(getUi().getContext(), R.string.camera_fail_to_operate,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        /* @} */
        String cameraId = cameraManager.getActiveCameraId();
        if (cameraId != null) {
            final int cameraDir = cameraManager.isUsingFrontFacingCamera()
                    ? Call.VideoSettings.CAMERA_DIRECTION_FRONT_FACING
                    : Call.VideoSettings.CAMERA_DIRECTION_BACK_FACING;
            mCall.getVideoSettings().setCameraDir(cameraDir);
            videoCall.setCamera(cameraId);
            cameraManager.setCameraPaused(false);//SPRD:Add for bug544111
            videoCall.requestCameraCapabilities();
        }
    }


    /**
     * Stop or start client's video transmission.
     * @param pause True if pausing the local user's video, or false if starting the local user's
     *    video.
     */
    public void pauseVideoClicked(boolean pause) {
        /*SPRD: Add for bug493880*/
        if (mCall == null) {
            return;
        }
        /* @} */
        VideoCall videoCall = mCall.getVideoCall();
        if (videoCall == null) {
            return;
        }
        /* SPRD:Add for bug531878 @{ */
        if (mCall.getState()!=Call.State.ACTIVE && getUi()!=null) {
            Toast.makeText(getUi().getContext(), R.string.camera_fail_to_operate,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        /* @} */
        /*SPRD: Add for bug493880*/
        InCallCameraManager cameraManager = InCallPresenter.getInstance().getInCallCameraManager();
        cameraManager.setCameraPaused(pause);
        /* @} */
        if (pause) {
            videoCall.setCamera(null);
            cameraManager.setCameraPaused(true);//SPRD:Add for bug544111
            VideoProfile videoProfile = new VideoProfile(
                    mCall.getVideoState() & ~VideoProfile.STATE_TX_ENABLED);
            videoCall.sendSessionModifyRequest(videoProfile);
        } else {
            videoCall.setCamera(cameraManager.getActiveCameraId());
            cameraManager.setCameraPaused(false);//SPRD:Add for bug544111
            VideoProfile videoProfile = new VideoProfile(
                    mCall.getVideoState() | VideoProfile.STATE_TX_ENABLED);
            videoCall.sendSessionModifyRequest(videoProfile);
            mCall.setSessionModificationState(Call.SessionModificationState.WAITING_FOR_RESPONSE);
        }
        getUi().setVideoPaused(pause);
        updateButtonsState(mCall);//SPRD:Add for bug539991
    }

    /* SPRD: Add for send sms button bug565307 */
    public void sendSMSClicked() {
        Log.i(this, "sendSMSClicked");
        final CallButtonUi ui = getUi();
        if (mCall != null && ui != null) {
            SendSmsButtonHelper sendsmsHelper = SendSmsButtonHelper
                    .getsInstance(ui.getContext());
            sendsmsHelper.sendSms(ui.getContext(), mCall);
        } else {
            Log.i(this, "The call is null,can't send message.");
        }
    }
    /* @} */

    /*SPRD: Add for bug539991*/
    public void refreshPauseState(boolean pause) {
        if (mCall == null) {
            return;
        }
        if (getUi() != null) {
            getUi().setVideoPaused(pause);
        }
        updateButtonsState(mCall);
    }
    /* @} */

    private void updateUi(InCallState state, Call call) {
        Log.d(this, "Updating call UI for call: ", call);

        final CallButtonUi ui = getUi();
        if (ui == null) {
            return;
        }

        final boolean isEnabled =
                state.isConnectingOrConnected() &&!state.isIncoming() && call != null;
        ui.setEnabled(isEnabled);

        if (call == null) {
            return;
        }

        updateButtonsState(call);
    }

    /**
     * Updates the buttons applicable for the UI.
     *
     * @param call The active call.
     */
    private void updateButtonsState(Call call) {
        Log.v(this, "updateButtonsState");
        final CallButtonUi ui = getUi();
        /* SPRD: add for bug535008 @{ */
        if (ui == null) {
            Log.d(this,"CallButtonUi is null.");
            return;
        }
        /* @} */
        final boolean isVideo = CallUtils.isVideoCall(call);

        // Common functionality (audio, hold, etc).
        // Show either HOLD or SWAP, but not both. If neither HOLD or SWAP is available:
        //     (1) If the device normally can hold, show HOLD in a disabled state.
        //     (2) If the device doesn't have the concept of hold/swap, remove the button.
        final boolean showSwap = call.can(
                android.telecom.Call.Details.CAPABILITY_SWAP_CONFERENCE);
        /* SPRD: add for bug 493620 and 529137@{ */
        final boolean showHold;
        if (!isVolteEnable) {
            showHold = !isVideo && !showSwap
                    && call.can(android.telecom.Call.Details.CAPABILITY_SUPPORT_HOLD)
                    && call.can(android.telecom.Call.Details.CAPABILITY_HOLD);
        } else {
            showHold = !showSwap
                    && call.can(android.telecom.Call.Details.CAPABILITY_SUPPORT_HOLD)
                    && call.can(android.telecom.Call.Details.CAPABILITY_HOLD);
        }
        /* @} */
        final boolean isCallOnHold = call.getState() == Call.State.ONHOLD;
        /* SPRD: add for bug 523923 @{ */
        final boolean isCallActive = call.getState() == Call.State.ACTIVE;
        /* @} */
        /* SPRD: add for bug 495445 @{ */
        final boolean showAddCall = !isVideo && TelecomAdapter.getInstance().canAddCall();
        /* @} */
        /*SPRD: add for bug 541566 @{*/
        final boolean showMerge = ShowMergeOptionUtil.getInstance(ui.getContext())
                                        .showMergeButton(call);
        /* @} */
        final boolean showUpgradeToVideo = !isVideo &&
                !call.isGenericConferenceCall() && //SPRD:add for VoLTE
                (call.can(android.telecom.Call.Details.CAPABILITY_SUPPORTS_VT_LOCAL_TX)
                && call.can(android.telecom.Call.Details.CAPABILITY_SUPPORTS_VT_REMOTE_RX)) && isVolteEnable && mStayVolte && TelephonyManager.isSupportVT();//SPRD:Add for bug535008 and bug547328

        final boolean showMute = call.can(android.telecom.Call.Details.CAPABILITY_MUTE);
        /* SPRD: add for bug 524669 @{ */
        int conferenceSize = 0;
        if (call.isConferenceCall() && call.getChildCallIds() != null) {
            conferenceSize = call.getChildCallIds().size();
        }
        final boolean canInvite = call.isConferenceCall() && isVolteEnable && (conferenceSize < 5) && mStayVolte && isCallActive && CallUtils.isPrimaryCard(ui.getContext(), call);//SPRD:Add for bug535008 and bug538810 and bug542932
        /* @} */
        final boolean canChangToAudio = isVideo && isVolteEnable && isCallActive;//SPRD:add volte for bug519225 and bug523923
        ui.showButton(BUTTON_AUDIO, true);
        ui.showButton(BUTTON_SWAP, showSwap);
        ui.showButton(BUTTON_HOLD, showHold);
        ui.setHold(isCallOnHold);
        ui.showButton(BUTTON_MUTE, showMute);
        ui.showButton(BUTTON_ADD_CALL, showAddCall);
        ui.showButton(BUTTON_UPGRADE_TO_VIDEO, showUpgradeToVideo);
        ui.showButton(BUTTON_SWITCH_CAMERA, isVideo);
        ui.showButton(BUTTON_PAUSE_VIDEO, isVideo);
        /* SPRD: modify for Video Call don't display dialpad @{ */
        ui.showButton(BUTTON_DIALPAD, true);
        /* @} */
        ui.showButton(BUTTON_MERGE, showMerge);

        if (isVideo) {
            if (call.getState() == Call.State.ACTIVE) {
                ui.showButton(BUTTON_RECORD,true);  // SPRD: recorder
                /* SPRD:bug494554 @{ */
                ui.enableButton(BUTTON_PAUSE_VIDEO, true);
                ui.enableButton(BUTTON_SWITCH_CAMERA, true);
                /* @} */
            } else {
                ui.showButton(BUTTON_RECORD, false);  // SPRD: recorder
                /* SPRD:bug494554 @{ */
                ui.enableButton(BUTTON_PAUSE_VIDEO, false);
                ui.enableButton(BUTTON_SWITCH_CAMERA, false);
                /* @} */
            }
            ui.showButton(BUTTON_ECT, false);  // SPRD: ECT, Do not show ECT button when video call.
        } else {
            ui.showButton(BUTTON_RECORD, enableRecorderOrAddCall(call));
            ui.showButton(BUTTON_ECT, enableTransferButton());  // SPRD: Add for Explicit Transfer Call
        }
        /* @} */
        /*SPRD: add for VoLTE and modify to canChangToAudio for bug519225{@*/
        ui.showButton(BUTTON_INVITE, canInvite);
        ui.showButton(BUTTON_CHANGED_TO_AUDIO, canChangToAudio);
        /* @} */
        /* SPRD: Add for send sms button bug565307 */
        SendSmsButtonHelper sendsmsHelper = SendSmsButtonHelper.getsInstance(ui
                .getContext());
        if (isCallActive && sendsmsHelper.isSupportSendSms()) {
            ui.showButton(BUTTON_SEND_SMS, true);
        }
        /* @} */
        ui.updateButtonStates();
    }

    public void refreshMuteState() {
        // Restore the previous mute state
        if (mAutomaticallyMuted &&
                AudioModeProvider.getInstance().getMute() != mPreviousMuteState) {
            if (getUi() == null) {
                return;
            }
            muteClicked(mPreviousMuteState);
        }
        mAutomaticallyMuted = false;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_AUTOMATICALLY_MUTED, mAutomaticallyMuted);
        outState.putBoolean(KEY_PREVIOUS_MUTE_STATE, mPreviousMuteState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        mAutomaticallyMuted =
                savedInstanceState.getBoolean(KEY_AUTOMATICALLY_MUTED, mAutomaticallyMuted);
        mPreviousMuteState =
                savedInstanceState.getBoolean(KEY_PREVIOUS_MUTE_STATE, mPreviousMuteState);
        super.onRestoreInstanceState(savedInstanceState);
    }

    public interface CallButtonUi extends Ui {
        void showButton(int buttonId, boolean show);
        void enableButton(int buttonId, boolean enable);
        void setEnabled(boolean on);
        void setMute(boolean on);
        void setHold(boolean on);
        void setCameraSwitched(boolean isBackFacingCamera);
        void setVideoPaused(boolean isPaused);
        void setAudio(int mode);
        void setSupportedAudio(int mask);
        void displayDialpad(boolean on, boolean animate);
        boolean isDialpadVisible();
        // SPRD: Add for recorder. @{
        void setRecord(boolean on);
        void toggleRecord();
        // @}

        /**
         * Once showButton() has been called on each of the individual buttons in the UI, call
         * this to configure the overflow menu appropriately.
         */
        void updateButtonStates();
        Context getContext();
        /* SPRD: add for bug493880@{*/
        void enableSwitchCameraButton(boolean enabled);
        /* @} */
    }

    @Override
    public void onActiveCameraSelectionChanged(boolean isUsingFrontFacingCamera) {
        if (getUi() == null) {
            return;
        }
        getUi().setCameraSwitched(!isUsingFrontFacingCamera);
    }
    /* SPRD: add for bug544111@{*/
    @Override
    public void onCameraPauseStateChanged(boolean isCameraPaused) {
        refreshPauseState(isCameraPaused);
    }
    /* @} */
    //------------------------------ SPRD --------------------------

    /* Add for recorder */
    public void recordClicked(boolean checked) {
        Log.d(this,"recordClicked... checked ==  " + checked);
        getUi().toggleRecord();
    }

    public boolean enableRecorderOrAddCall(Call call) {
        int state = call.getState();
        return (state == Call.State.ACTIVE || state == Call.State.ONHOLD
                || state == Call.State.CONFERENCED);
    }

    /* SPRD: add for bug493880@{*/
    @Override
    public void onCameraSwitchStateChanged(boolean isSwithing){
        final CallButtonUi ui = getUi();
        if (ui == null) {
            return;
        }
        /* SPRD: add for bug520684@{*/
        if (mCall == null) {
            ui.enableButton(BUTTON_PAUSE_VIDEO, !isSwithing);
            ui.enableSwitchCameraButton(!isSwithing);
        } else {
            /* SPRD: Modify for bug531878@{*/
            ui.enableButton(BUTTON_PAUSE_VIDEO, !isSwithing && (mCall.getState() == Call.State.ACTIVE || mCall.getState() == Call.State.ONHOLD));
            ui.enableSwitchCameraButton(!isSwithing && (mCall.getState() == Call.State.ACTIVE || mCall.getState() == Call.State.ONHOLD));
            /* @} */
        }
        /* @} */
    }
    /* @} */

    /* SPRD: Add for Volte @{ */
    public boolean isMultiCall() {
        if (mCall == null) {
            return false;
        }
        return mCall.can(android.telecom.Call.Details.CAPABILITY_MANAGE_CONFERENCE);
    }
    /* @}*/

    /*SPRD:bug525089 @{*/
    private boolean isWaitForRespose() {

        boolean bRet = false;

        if (Call.SessionModificationState.WAITING_FOR_RESPONSE == mCall
                .getSessionModificationState()) {
            Toast.makeText(getUi().getContext(), R.string.card_title_video_call_requesting,
                    Toast.LENGTH_SHORT).show();
            bRet = true;
        }
        return bRet;
    }/*@}*/

    /**
     * Porting Explicit Transfer Call.
     */
    public boolean enableTransferButton() {
        // If ECT plug in?
        boolean isEctPlugin = ExplicitCallTransferPluginHelper.getInstance(getUi().getContext())
                .isExplicitCallTransferPlugin();
        // According to 3GPP TS23.091, only when background call is HOLDING and foreground call
        // is DIALING, ALERTING, or ACTIVE, transfer button will display.
        CallList calllist = CallList.getInstance();
        return isEctPlugin && calllist != null
                && calllist.getBackgroundCall() != null // HOLDING
                && calllist.getOutgoingOrActive() != null; // DIALING/ALERTING/ACTIVE
    }

    /**
     * Porting Explicit Transfer Call.
     */
    public void transferClicked() {
        TelecomAdapter.getInstance().explicitCallTransfer(mCall.getId());
    }

    /* sprd: add for bug535008 { */
    public void createPhoneStateListener() {
        mLtePhoneStateListener = new PhoneStateListener() {
            @Override
            public void onVoLteServiceStateChanged(VoLteServiceState serviceState) {
                mStayVolte = (serviceState.getSrvccState() == VoLteServiceState.IMS_REG_STATE_REGISTERED);
                if (mCall != null) {
                    updateButtonsState(mCall);
                }
            }
        };
    }

    public void startMonitor() {
        /* sprd: modify for bug536643 { */
        if (mTelephonyManager != null
                && mTelephonyManager.isVolteCallEnabled()
                && mLtePhoneStateListener != null
                && getUi() != null
                && getUi().getContext() != null
                && getUi().getContext().checkSelfPermission(READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            mTelephonyManager.listen(mLtePhoneStateListener,PhoneStateListener.LISTEN_VOLTE_STATE);
        }
        /* } */
    }

    public void stopMonitor() {
        /* sprd: modify for bug536643 { */
        if (mTelephonyManager != null
                && mTelephonyManager.isVolteCallEnabled()
                && mLtePhoneStateListener != null
                && getUi() != null
                && getUi().getContext() != null
                && getUi().getContext().checkSelfPermission(READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            mTelephonyManager.listen(mLtePhoneStateListener,PhoneStateListener.LISTEN_NONE);
        }
        /* } */
        mLtePhoneStateListener = null;
    }
    /* } */

}
