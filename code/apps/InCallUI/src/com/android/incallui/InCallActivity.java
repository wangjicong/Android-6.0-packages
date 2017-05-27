/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.incallui;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.KeyguardManager;
import android.app.LowmemoryUtils;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Point;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Trace;
import android.telecom.DisconnectCause;
import android.telecom.PhoneAccountHandle;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import com.android.contacts.common.interactions.TouchPointManager;
import com.android.contacts.common.util.PermissionsUtil;
import com.android.contacts.common.widget.SelectPhoneAccountDialogFragment;
import com.android.contacts.common.widget.SelectPhoneAccountDialogFragment.SelectPhoneAccountListener;
import com.android.incallui.Call.State;
import com.android.phone.common.animation.AnimUtils;
import com.android.phone.common.animation.AnimationListenerAdapter;
import com.sprd.incallui.PhoneRecorderHelper;
import com.sprd.incallui.PhoneRecorderHelper.OnStateChangedListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.RECORD_AUDIO;
/* Sunvov:jiazhenl 20150609 add start @{ */
import android.provider.Settings;
/* Sunvov:jiazhenl 20150609 add end @} */

//qiuyaobo,20160716,begin
import com.sprd.android.config.OptConfig;
import android.os.SystemProperties;
import android.content.BroadcastReceiver;
import android.content.SharedPreferences;
import android.content.IntentFilter;
import android.content.pm.PackageManager.NameNotFoundException;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.android.incallui.InCallPresenter.InCallState;
import android.widget.ImageView;
import android.os.Handler;
import android.widget.LinearLayout;
import android.telecom.VideoProfile;
import android.os.Vibrator;
import com.sprd.incallui.MultiPartCallHelper;
//qiuyaobo,20160716,end
/**
 * Main activity that the user interacts with while in a live call.
 */
public class InCallActivity extends Activity implements FragmentDisplayManager {

    public static final String TAG = InCallActivity.class.getSimpleName();

    public static final String SHOW_DIALPAD_EXTRA = "InCallActivity.show_dialpad";
    public static final String DIALPAD_TEXT_EXTRA = "InCallActivity.dialpad_text";
    public static final String NEW_OUTGOING_CALL_EXTRA = "InCallActivity.new_outgoing_call";

    private static final String TAG_DIALPAD_FRAGMENT = "tag_dialpad_fragment";
    private static final String TAG_CONFERENCE_FRAGMENT = "tag_conference_manager_fragment";
    private static final String TAG_CALLCARD_FRAGMENT = "tag_callcard_fragment";
    private static final String TAG_ANSWER_FRAGMENT = "tag_answer_fragment";
    private static final String TAG_SELECT_ACCT_FRAGMENT = "tag_select_acct_fragment";

    // SPRD: Porting dealing with SS notification.
    private static final String ACTION_SUPP_SERVICE_FAILURE =
            "org.codeaurora.ACTION_SUPP_SERVICE_FAILURE";

    private CallButtonFragment mCallButtonFragment;
    private CallCardFragment mCallCardFragment;
    private AnswerFragment mAnswerFragment;
    private DialpadFragment mDialpadFragment;
    private ConferenceManagerFragment mConferenceManagerFragment;
    private FragmentManager mChildFragmentManager;
    //SPRD: add for conferencecall list
    private ConferenceListFragment mConferenceListFragment;

    private boolean mIsVisible;
    private AlertDialog mDialog;

    /** Use to pass 'showDialpad' from {@link #onNewIntent} to {@link #onResume} */
    private boolean mShowDialpadRequested;

    /** Use to determine if the dialpad should be animated on show. */
    private boolean mAnimateDialpadOnShow;

    /** Use to determine the DTMF Text which should be pre-populated in the dialpad. */
    private String mDtmfText;

    /** Use to pass parameters for showing the PostCharDialog to {@link #onResume} */
    private boolean mShowPostCharWaitDialogOnResume;
    private String mShowPostCharWaitDialogCallId;
    private String mShowPostCharWaitDialogChars;

    private boolean mIsLandscape;
    private Animation mSlideIn;
    private Animation mSlideOut;
    private boolean mDismissKeyguard = false;

    /* SPRD: Add for recorder && bug501927 @{ */
    private PhoneRecorderHelper mRecorderHelper;
    private String mRecordingLabelStr;
    private BroadcastReceiver mSDCardMountEventReceiver;
    private static final int MICROPHONE_AND_STORAGE_PERMISSION_REQUEST_CODE = 1;
    private PhoneRecorderHelper.State mRecorderState;
    /* @} */
    // SPRD: Add for dealing with SS notification.
    private SuppServFailureNotificationReceiver mReceiver;

    /* Sunvov:jiazhenl 20150609 add for call auto record start @{ */
    private boolean	is_auto_record = false;
    public static  boolean IsStratRecording = false;
    /* Sunvov:jiazhenl 20150609 add for call auto record end @} */
    /*SUN:jicong.wang add for vibrate when call connect start {@*/
    private boolean is_vibrate_when_call_connection = false;
    /*SUN:jicong.wang add for vibrate when call connection end @}*/
    //qiuyaobo,20160716,SUNVOV_USE_HALL_LEATHER_FUNCTION,begin
    //private RelativeLayout mlidIncallScreenView;
    private LinearLayout mlidIncallScreenView;
    private TextView mcallName;
    private TextView moperatorNameOrCallElapsedTime;
    private TextView mphoneNumber;
    private ImageView mwait_answer;
    private CallTimer mCallTimer;
    private BroadcastReceiver mHallEventReceiver;
    //qiuyaobo,20160716,SUNVOV_USE_HALL_LEATHER_FUNCTION,end
        
    AnimationListenerAdapter mSlideOutListener = new AnimationListenerAdapter() {
        @Override
        public void onAnimationEnd(Animation animation) {
            showFragment(TAG_DIALPAD_FRAGMENT, false, true);
        }
    };

    private SelectPhoneAccountListener mSelectAcctListener = new SelectPhoneAccountListener() {
        @Override
        public void onPhoneAccountSelected(PhoneAccountHandle selectedAccountHandle,
                boolean setDefault) {
            InCallPresenter.getInstance().handleAccountSelection(selectedAccountHandle,
                    setDefault);
        }
        @Override
        public void onDialogDismissed() {
            /* SPRD: add for bug494731 @{ */
            // InCallPresenter.getInstance().cancelAccountSelection();
            KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            boolean keyguardRestricted = keyguardManager.inKeyguardRestrictedInputMode();
            if (!keyguardRestricted || !SelectPhoneAccountDialogFragment.isDialogShowing()) {
                InCallPresenter.getInstance().cancelAccountSelection();
            }
            /* @} */
        }
    };

    /** Listener for orientation changes. */
    private OrientationEventListener mOrientationEventListener;

    /**
     * Used to determine if a change in rotation has occurred.
     */
    private static int sPreviousRotation = -1;

    @Override
    protected void onCreate(Bundle icicle) {
        Log.d(this, "onCreate()...  this = " + this);

        super.onCreate(icicle);

        // set this flag so this activity will stay in front of the keyguard
        // Have the WindowManager filter out touch events that are "too fat".
        int flags = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES;

        getWindow().addFlags(flags);

        // Setup action bar for the conference call manager.
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.hide();
        }

				//qiuyaobo,20160716,begin
				if(OptConfig.SUNVOV_USE_HALL_LEATHER_FUNCTION){
        		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN); 
        }
        //qiuyaobo,20160716,end
                      
        // TODO(klp): Do we need to add this back when prox sensor is not available?
        // lp.inputFeatures |= WindowManager.LayoutParams.INPUT_FEATURE_DISABLE_USER_ACTIVITY;

        setContentView(R.layout.incall_screen);

        internalResolveIntent(getIntent());

        mIsLandscape = getResources().getConfiguration().orientation ==
                Configuration.ORIENTATION_LANDSCAPE;

        final boolean isRtl = TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) ==
                View.LAYOUT_DIRECTION_RTL;

        if (mIsLandscape) {
            mSlideIn = AnimationUtils.loadAnimation(this,
                    isRtl ? R.anim.dialpad_slide_in_left : R.anim.dialpad_slide_in_right);
            mSlideOut = AnimationUtils.loadAnimation(this,
                    isRtl ? R.anim.dialpad_slide_out_left : R.anim.dialpad_slide_out_right);
        } else {
            mSlideIn = AnimationUtils.loadAnimation(this, R.anim.dialpad_slide_in_bottom);
            mSlideOut = AnimationUtils.loadAnimation(this, R.anim.dialpad_slide_out_bottom);
        }

        mSlideIn.setInterpolator(AnimUtils.EASE_IN);
        mSlideOut.setInterpolator(AnimUtils.EASE_OUT);

        mSlideOut.setAnimationListener(mSlideOutListener);

        if (icicle != null) {
            // If the dialpad was shown before, set variables indicating it should be shown and
            // populated with the previous DTMF text.  The dialpad is actually shown and populated
            // in onResume() to ensure the hosting CallCardFragment has been inflated and is ready
            // to receive it.
            mShowDialpadRequested = icicle.getBoolean(SHOW_DIALPAD_EXTRA);
            mAnimateDialpadOnShow = false;
            mDtmfText = icicle.getString(DIALPAD_TEXT_EXTRA);

            SelectPhoneAccountDialogFragment dialogFragment = (SelectPhoneAccountDialogFragment)
                getFragmentManager().findFragmentByTag(TAG_SELECT_ACCT_FRAGMENT);
            if (dialogFragment != null) {
                dialogFragment.setListener(mSelectAcctListener);
            }
        }

        mOrientationEventListener = new OrientationEventListener(this,
                SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int orientation) {
                // Device is flat, don't change orientation.
                if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
                    return;
                }

                int newRotation = Surface.ROTATION_0;
                // We only shift if we're within 22.5 (23) degrees of the target
                // orientation. This avoids flopping back and forth when holding
                // the device at 45 degrees or so.
                if (orientation >= 337 || orientation <= 23) {
                    newRotation = Surface.ROTATION_0;
                } else if (orientation >= 67 && orientation <= 113) {
                    // Why not 90? Because screen and sensor orientation are
                    // reversed.
                    newRotation = Surface.ROTATION_270;
                } else if (orientation >= 157 && orientation <= 203) {
                    newRotation = Surface.ROTATION_180;
                } else if (orientation >= 247 && orientation <= 293) {
                    newRotation = Surface.ROTATION_90;
                }

                // Orientation is the current device orientation in degrees.  Ultimately we want
                // the rotation (in fixed 90 degree intervals).
                if (newRotation != sPreviousRotation) {
                    doOrientationChanged(newRotation);
                }
            }
        };

        /* SPRD: Add for recorder @{ */
        mRecorderHelper = PhoneRecorderHelper.getInstance(getApplicationContext());
        mRecorderHelper.setOnStateChangedListener(mRecorderStateChangedListener);
        registerExternalStorageStateListener();
        /* @} */
        /* SPRD: Porting dealing with SS notification. @{ */
        // Register for supplementary service failure  broadcasts.
        mReceiver = new SuppServFailureNotificationReceiver();
        IntentFilter intentFilter =
                new IntentFilter(ACTION_SUPP_SERVICE_FAILURE);
        registerReceiver(mReceiver, intentFilter);
        /* @} */
        
				//qiuyaobo,20160716,begin
				if (OptConfig.SUNVOV_USE_HALL_LEATHER_FUNCTION){
		        if(mlidIncallScreenView == null){//hj
		        	  mlidIncallScreenView = (LinearLayout) findViewById(R.id.lidIncallScreen);//RelativeLayout
		        	  mcallName = (TextView) findViewById(R.id.callname);
		            moperatorNameOrCallElapsedTime = (TextView) findViewById(R.id.operatorNameOrCallElapsedTime);
		            mphoneNumber = (TextView) findViewById(R.id.phonenumber);
		            WaitSliderRelativeLayout mwaitSliderRelativeLayout = (WaitSliderRelativeLayout) findViewById(R.id.slider_layout);
		            mwait_answer = (ImageView) findViewById(R.id.wait_answer);	
		            mwaitSliderRelativeLayout.setInCallActivity(this);
		          }
				}else{
						mlidIncallScreenView = (LinearLayout) findViewById(R.id.lidIncallScreen);//RelativeLayout
						if(mlidIncallScreenView != null){//hj
							mlidIncallScreenView.setVisibility(View.GONE);
						}
				}				
				
				if (OptConfig.SUNVOV_USE_HALL_LEATHER_FUNCTION){
						registerHallLeatherStateListener();
						// create the call timer
						mCallTimer = new CallTimer(new Runnable() {
								@Override
								public void run() {
								updateLidCallTime();
								}
							});
						mCallTimer.start(500);
				}
				//qiuyaobo,20160716,end
		        
        Log.d(this, "onCreate(): exit");
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        // TODO: The dialpad fragment should handle this as part of its own state
        out.putBoolean(SHOW_DIALPAD_EXTRA,
                mCallButtonFragment != null && mCallButtonFragment.isDialpadVisible());
        if (mDialpadFragment != null) {
            out.putString(DIALPAD_TEXT_EXTRA, mDialpadFragment.getDtmfText());
        }
        super.onSaveInstanceState(out);
    }

    @Override
    protected void onStart() {
        Log.d(this, "onStart()...");
        super.onStart();

        mIsVisible = true;

        if (mOrientationEventListener.canDetectOrientation()) {
            Log.v(this, "Orientation detection enabled.");
            mOrientationEventListener.enable();
        } else {
            Log.v(this, "Orientation detection disabled.");
            mOrientationEventListener.disable();
        }
        // setting activity should be last thing in setup process
        InCallPresenter.getInstance().setActivity(this);
        /* Sunvov:jiazhenl 20150609 add for call auto record start @{ */
        is_auto_record = getCallSetAutoRecord(this);
        /* Sunvov:jiazhenl 20150609 add for call auto record end @} */
        /*SUN:jicong.wang add for vibrate when call connection start {@*/
        is_vibrate_when_call_connection = Settings.System.getInt(getContentResolver(),Settings.System.VIBRATE_WHEN_CALL_CONNECTION, 0) == 1 ? true : false;
        /*SUN:jicong.wang add for vibrate when call connection end @}*/
        InCallPresenter.getInstance().onActivityStarted();
        
        //qiuyaobo,20160716,begin
        if(OptConfig.SUNVOV_USE_HALL_LEATHER_FUNCTION){
        	  boolean lidOpen=SystemProperties.getBoolean("persist.sys.lidopen",false);
            updateInCallUI(lidOpen);
        }
        //qiuyaobo,20160716,end        
    }

    @Override
    protected void onResume() {
        Log.i(this, "onResume()...");
        super.onResume();

        InCallPresenter.getInstance().setThemeColors();
        InCallPresenter.getInstance().onUiShowing(true);

        if (mShowDialpadRequested) {
            mCallButtonFragment.displayDialpad(true /* show */,
                    mAnimateDialpadOnShow /* animate */);
            mShowDialpadRequested = false;
            mAnimateDialpadOnShow = false;

            if (mDialpadFragment != null) {
                mDialpadFragment.setDtmfText(mDtmfText);
                mDtmfText = null;
            }
        }

        if (mShowPostCharWaitDialogOnResume) {
            showPostCharWaitDialog(mShowPostCharWaitDialogCallId, mShowPostCharWaitDialogChars);
        }
        /* SPRD: Add for recorder @{ */
        mRecordingLabelStr = getResources().getString(R.string.recording);
        mRecorderHelper.notifyCurrentState();
        /* @} */
    }

    // onPause is guaranteed to be called when the InCallActivity goes
    // in the background.
    @Override
    protected void onPause() {
        Log.d(this, "onPause()...");
        if (mDialpadFragment != null ) {
            mDialpadFragment.onDialerKeyUp(null);
        }

        InCallPresenter.getInstance().onUiShowing(false);
        if (isFinishing()) {
            InCallPresenter.getInstance().unsetActivity(this);
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.d(this, "onStop()...");
        mIsVisible = false;
        InCallPresenter.getInstance().updateIsChangingConfigurations();
        InCallPresenter.getInstance().onActivityStopped();
        mOrientationEventListener.disable();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(this, "onDestroy()...  this = " + this);
        InCallPresenter.getInstance().unsetActivity(this);
        InCallPresenter.getInstance().updateIsChangingConfigurations();
        // SPRD: Stop recorder for disconnect
        InCallPresenter.getInstance().stopRecorderForDisconnect();
        unRegisterExternalStorageStateListener(); // SPRD: Add for recorder
        unregisterReceiver(mReceiver); // SPRD: Porting dealing with SS notification.
        
        //qiuyaobo,20160716,begin
				if (OptConfig.SUNVOV_USE_HALL_LEATHER_FUNCTION){
			        unRegisterHallLeatherStateListener();
			        mCallTimer.cancel();
				}  
				//qiuyaobo,20160716,end
				      
        super.onDestroy();
    }

    /**
     * When fragments have a parent fragment, onAttachFragment is not called on the parent
     * activity. To fix this, register our own callback instead that is always called for
     * all fragments.
     *
     * @see {@link BaseFragment#onAttach(Activity)}
     */
    @Override
    public void onFragmentAttached(Fragment fragment) {
        if (fragment instanceof DialpadFragment) {
            mDialpadFragment = (DialpadFragment) fragment;
        } else if (fragment instanceof AnswerFragment) {
            mAnswerFragment = (AnswerFragment) fragment;
        } else if (fragment instanceof CallCardFragment) {
            mCallCardFragment = (CallCardFragment) fragment;
            mChildFragmentManager = mCallCardFragment.getChildFragmentManager();
        } else if (fragment instanceof ConferenceManagerFragment) {
            mConferenceManagerFragment = (ConferenceManagerFragment) fragment;
        } else if (fragment instanceof CallButtonFragment) {
            mCallButtonFragment = (CallButtonFragment) fragment;
        /*SPRD: add for VoLTE{@*/
        } else if(fragment instanceof ConferenceListFragment){
            mConferenceListFragment = (ConferenceListFragment) fragment;
        }
        /*@}*/
    }

    /**
     * Returns true when the Activity is currently visible (between onStart and onStop).
     */
    /* package */ boolean isVisible() {
        return mIsVisible;
    }

    private boolean hasPendingDialogs() {
        return mDialog != null || (mAnswerFragment != null && mAnswerFragment.hasPendingDialogs());
    }

    @Override
    public void finish() {
        Log.i(this, "finish().  Dialog showing: " + (mDialog != null));

        // skip finish if we are still showing a dialog.
        if (!hasPendingDialogs()) {
            super.finish();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.d(this, "onNewIntent: intent = " + intent);

        // We're being re-launched with a new Intent.  Since it's possible for a
        // single InCallActivity instance to persist indefinitely (even if we
        // finish() ourselves), this sequence can potentially happen any time
        // the InCallActivity needs to be displayed.

        // Stash away the new intent so that we can get it in the future
        // by calling getIntent().  (Otherwise getIntent() will return the
        // original Intent from when we first got created!)
        setIntent(intent);

        // Activities are always paused before receiving a new intent, so
        // we can count on our onResume() method being called next.

        // Just like in onCreate(), handle the intent.
        internalResolveIntent(intent);
    }

    @Override
    public void onBackPressed() {
        Log.i(this, "onBackPressed");

        // BACK is also used to exit out of any "special modes" of the
        // in-call UI:

        if ((mConferenceManagerFragment == null || !mConferenceManagerFragment.isVisible())
                && (mCallCardFragment == null || !mCallCardFragment.isVisible())) {
            return;
        }

        /* SPRD: Fix AOB: Hide ConferenceFragment first for special modes
         * @orig
        if (mDialpadFragment != null && mDialpadFragment.isVisible()) {
            mCallButtonFragment.displayDialpad(false *//* show *//*, true *//* animate *//*);
            return;
        } else if (mConferenceManagerFragment != null && mConferenceManagerFragment.isVisible()) {
            showConferenceFragment(false);
            return;
        }*/
        if (mConferenceManagerFragment != null && mConferenceManagerFragment.isVisible()) {
            showConferenceFragment(false);
            return;
        } else if (mDialpadFragment != null && mDialpadFragment.isVisible()) {
            mCallButtonFragment.displayDialpad(false /* show */, true /* animate */);
            return;
        }
        /* @} */

        // Always disable the Back key while an incoming call is ringing
        final Call call = CallList.getInstance().getIncomingCall();
        if (call != null) {
            Log.i(this, "Consume Back press for an incoming call");
            return;
        }

        // Nothing special to do.  Fall back to the default behavior.
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // push input to the dialer.
        if (mDialpadFragment != null && (mDialpadFragment.isVisible()) &&
                (mDialpadFragment.onDialerKeyUp(event))){
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_CALL) {
            // Always consume CALL to be sure the PhoneWindow won't do anything with it
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_CALL:
                boolean handled = InCallPresenter.getInstance().handleCallKey();
                if (!handled) {
                    Log.w(this, "InCallActivity should always handle KEYCODE_CALL in onKeyDown");
                }
                // Always consume CALL to be sure the PhoneWindow won't do anything with it
                return true;

            // Note there's no KeyEvent.KEYCODE_ENDCALL case here.
            // The standard system-wide handling of the ENDCALL key
            // (see PhoneWindowManager's handling of KEYCODE_ENDCALL)
            // already implements exactly what the UI spec wants,
            // namely (1) "hang up" if there's a current active call,
            // or (2) "don't answer" if there's a current ringing call.

            case KeyEvent.KEYCODE_CAMERA:
                // Disable the CAMERA button while in-call since it's too
                // easy to press accidentally.
                return true;

            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_MUTE:
                // Ringer silencing handled by PhoneWindowManager.
                break;

            case KeyEvent.KEYCODE_MUTE:
                // toggle mute
                TelecomAdapter.getInstance().mute(!AudioModeProvider.getInstance().getMute());
                return true;

            // Various testing/debugging features, enabled ONLY when VERBOSE == true.
            case KeyEvent.KEYCODE_SLASH:
                if (Log.VERBOSE) {
                    Log.v(this, "----------- InCallActivity View dump --------------");
                    // Dump starting from the top-level view of the entire activity:
                    Window w = this.getWindow();
                    View decorView = w.getDecorView();
                    Log.d(this, "View dump:" + decorView);
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_EQUALS:
                // TODO: Dump phone state?
                break;
        }

        if (event.getRepeatCount() == 0 && handleDialerKeyDown(keyCode, event)) {
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    private boolean handleDialerKeyDown(int keyCode, KeyEvent event) {
        Log.v(this, "handleDialerKeyDown: keyCode " + keyCode + ", event " + event + "...");

        // As soon as the user starts typing valid dialable keys on the
        // keyboard (presumably to type DTMF tones) we start passing the
        // key events to the DTMFDialer's onDialerKeyDown.
        if (mDialpadFragment != null && mDialpadFragment.isVisible()) {
            return mDialpadFragment.onDialerKeyDown(event);
        }

        return false;
    }

    /**
     * Handles changes in device rotation.
     *
     * @param rotation The new device rotation (one of: {@link Surface#ROTATION_0},
     *      {@link Surface#ROTATION_90}, {@link Surface#ROTATION_180},
     *      {@link Surface#ROTATION_270}).
     */
    private void doOrientationChanged(int rotation) {
        Log.d(this, "doOrientationChanged prevOrientation=" + sPreviousRotation +
                " newOrientation=" + rotation);
        // Check to see if the rotation changed to prevent triggering rotation change events
        // for other configuration changes.
        if (rotation != sPreviousRotation) {
            sPreviousRotation = rotation;
            InCallPresenter.getInstance().onDeviceRotationChange(rotation);
            InCallPresenter.getInstance().onDeviceOrientationChange(sPreviousRotation);
        }
    }

    public CallButtonFragment getCallButtonFragment() {
        return mCallButtonFragment;
    }

    public CallCardFragment getCallCardFragment() {
        return mCallCardFragment;
    }

    public AnswerFragment getAnswerFragment() {
        return mAnswerFragment;
    }

    private void internalResolveIntent(Intent intent) {
        final String action = intent.getAction();
        if (action.equals(Intent.ACTION_MAIN)) {
            // This action is the normal way to bring up the in-call UI.
            //
            // But we do check here for one extra that can come along with the
            // ACTION_MAIN intent:

            if (intent.hasExtra(SHOW_DIALPAD_EXTRA)) {
                // SHOW_DIALPAD_EXTRA can be used here to specify whether the DTMF
                // dialpad should be initially visible.  If the extra isn't
                // present at all, we just leave the dialpad in its previous state.

                final boolean showDialpad = intent.getBooleanExtra(SHOW_DIALPAD_EXTRA, false);
                Log.d(this, "- internalResolveIntent: SHOW_DIALPAD_EXTRA: " + showDialpad);

                relaunchedFromDialer(showDialpad);
            }

            boolean newOutgoingCall = false;
            if (intent.getBooleanExtra(NEW_OUTGOING_CALL_EXTRA, false)) {
                intent.removeExtra(NEW_OUTGOING_CALL_EXTRA);
                Call call = CallList.getInstance().getOutgoingCall();
                if (call == null) {
                    call = CallList.getInstance().getPendingOutgoingCall();
                }

                Bundle extras = null;
                if (call != null) {
                    extras = call.getTelecommCall().getDetails().getIntentExtras();
                }
                if (extras == null) {
                    // Initialize the extras bundle to avoid NPE
                    extras = new Bundle();
                }

                Point touchPoint = null;
                if (TouchPointManager.getInstance().hasValidPoint()) {
                    // Use the most immediate touch point in the InCallUi if available
                    touchPoint = TouchPointManager.getInstance().getPoint();
                } else {
                    // Otherwise retrieve the touch point from the call intent
                    if (call != null) {
                        touchPoint = (Point) extras.getParcelable(TouchPointManager.TOUCH_POINT);
                    }
                }

                // Start animation for new outgoing call
                CircularRevealFragment.startCircularReveal(getFragmentManager(), touchPoint,
                        InCallPresenter.getInstance());

                // InCallActivity is responsible for disconnecting a new outgoing call if there
                // is no way of making it (i.e. no valid call capable accounts)
                if (InCallPresenter.isCallWithNoValidAccounts(call)) {
                    TelecomAdapter.getInstance().disconnectCall(call.getId());
                }

                dismissKeyguard(true);
                newOutgoingCall = true;
            }

            Call pendingAccountSelectionCall = CallList.getInstance().getWaitingForAccountCall();
            if (pendingAccountSelectionCall != null) {
                showCallCardFragment(false);
                Bundle extras = pendingAccountSelectionCall
                        .getTelecommCall().getDetails().getIntentExtras();

                final List<PhoneAccountHandle> phoneAccountHandles;
                if (extras != null) {
                    phoneAccountHandles = extras.getParcelableArrayList(
                            android.telecom.Call.AVAILABLE_PHONE_ACCOUNTS);
                } else {
                    phoneAccountHandles = new ArrayList<>();
                }

                DialogFragment dialogFragment = SelectPhoneAccountDialogFragment.newInstance(
                        R.string.select_phone_account_for_calls, true, phoneAccountHandles,
                        mSelectAcctListener);
                dialogFragment.show(getFragmentManager(), TAG_SELECT_ACCT_FRAGMENT);
            } else if (!newOutgoingCall) {
                showCallCardFragment(true);
            }

            return;
        }
    }

    private void relaunchedFromDialer(boolean showDialpad) {
        mShowDialpadRequested = showDialpad;
        mAnimateDialpadOnShow = true;

        if (mShowDialpadRequested) {
            // If there's only one line in use, AND it's on hold, then we're sure the user
            // wants to use the dialpad toward the exact line, so un-hold the holding line.
            final Call call = CallList.getInstance().getActiveOrBackgroundCall();
            if (call != null && call.getState() == State.ONHOLD) {
                TelecomAdapter.getInstance().unholdCall(call.getId());
            }
        }
    }

    public void dismissKeyguard(boolean dismiss) {
        if (mDismissKeyguard == dismiss) {
            return;
        }
        mDismissKeyguard = dismiss;
        if (dismiss) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        }
    }

    /* SPRD: force screen unlock fix for bug543928@{ */
    public void forceDismissKeyguard() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
    }
    /* @} */

    private void showFragment(String tag, boolean show, boolean executeImmediately) {
        Trace.beginSection("showFragment - " + tag);
        final FragmentManager fm = getFragmentManagerForTag(tag);

        if (fm == null) {
            Log.w(TAG, "Fragment manager is null for : " + tag);
            return;
        }

        Fragment fragment = fm.findFragmentByTag(tag);
        if (!show && fragment == null) {
            // Nothing to show, so bail early.
            return;
        }

        final FragmentTransaction transaction = fm.beginTransaction();
        if (show) {
            if (fragment == null) {
                fragment = createNewFragmentForTag(tag);
                transaction.add(getContainerIdForFragment(tag), fragment, tag);
            } else {
                transaction.show(fragment);
            }
        } else {
            transaction.hide(fragment);
        }

        transaction.commitAllowingStateLoss();
        if (executeImmediately) {
            fm.executePendingTransactions();
        }
        Trace.endSection();
    }

    private Fragment createNewFragmentForTag(String tag) {
        if (TAG_DIALPAD_FRAGMENT.equals(tag)) {
            mDialpadFragment = new DialpadFragment();
            return mDialpadFragment;
        } else if (TAG_ANSWER_FRAGMENT.equals(tag)) {
            mAnswerFragment = new AnswerFragment();
            return mAnswerFragment;
        } else if (TAG_CONFERENCE_FRAGMENT.equals(tag)) {
            mConferenceManagerFragment = new ConferenceManagerFragment();
            return mConferenceManagerFragment;
        } else if (TAG_CALLCARD_FRAGMENT.equals(tag)) {
            mCallCardFragment = new CallCardFragment();
            return mCallCardFragment;
        }
        throw new IllegalStateException("Unexpected fragment: " + tag);
    }

    private FragmentManager getFragmentManagerForTag(String tag) {
        if (TAG_DIALPAD_FRAGMENT.equals(tag)) {
            return mChildFragmentManager;
        } else if (TAG_ANSWER_FRAGMENT.equals(tag)) {
            return mChildFragmentManager;
        } else if (TAG_CONFERENCE_FRAGMENT.equals(tag)) {
            return getFragmentManager();
        } else if (TAG_CALLCARD_FRAGMENT.equals(tag)) {
            return getFragmentManager();
        }
        throw new IllegalStateException("Unexpected fragment: " + tag);
    }

    private int getContainerIdForFragment(String tag) {
        if (TAG_DIALPAD_FRAGMENT.equals(tag)) {
            return R.id.answer_and_dialpad_container;
        } else if (TAG_ANSWER_FRAGMENT.equals(tag)) {
            return R.id.answer_and_dialpad_container;
        } else if (TAG_CONFERENCE_FRAGMENT.equals(tag)) {
            return R.id.main;
        } else if (TAG_CALLCARD_FRAGMENT.equals(tag)) {
            return R.id.main;
        }
        throw new IllegalStateException("Unexpected fragment: " + tag);
    }

    public void showDialpadFragment(boolean show, boolean animate) {
        // If the dialpad is already visible, don't animate in. If it's gone, don't animate out.
        if ((show && isDialpadVisible()) || (!show && !isDialpadVisible())) {
            return;
        }
        // We don't do a FragmentTransaction on the hide case because it will be dealt with when
        // the listener is fired after an animation finishes.
        if (!animate) {
            showFragment(TAG_DIALPAD_FRAGMENT, show, true);
        } else {
            if (show) {
                showFragment(TAG_DIALPAD_FRAGMENT, true, true);
                mDialpadFragment.animateShowDialpad();
            }
            mCallCardFragment.onDialpadVisibilityChange(show);
            mDialpadFragment.getView().startAnimation(show ? mSlideIn : mSlideOut);
        }

        final ProximitySensor sensor = InCallPresenter.getInstance().getProximitySensor();
        if (sensor != null) {
            sensor.onDialpadVisible(show);
        }
    }
    /*SPRD: add for VoLTE{@*/
    public void showConferenceListFragmet(boolean show) {
        if(mConferenceListFragment != null){
            mConferenceListFragment.setVisible(show);
        }
    }
    /*@}*/
    public boolean isDialpadVisible() {
        return mDialpadFragment != null && mDialpadFragment.isVisible();
    }

    public void showCallCardFragment(boolean show) {
        showFragment(TAG_CALLCARD_FRAGMENT, show, true);
    }

    /**
     * Hides or shows the conference manager fragment.
     *
     * @param show {@code true} if the conference manager should be shown, {@code false} if it
     *                         should be hidden.
     */
    public void showConferenceFragment(boolean show) {
        showFragment(TAG_CONFERENCE_FRAGMENT, show, true);
        mConferenceManagerFragment.onVisibilityChanged(show);

        // Need to hide the call card fragment to ensure that accessibility service does not try to
        // give focus to the call card when the conference manager is visible.
        mCallCardFragment.getView().setVisibility(show ? View.GONE : View.VISIBLE);
    }

    public void showAnswerFragment(boolean show) {
        showFragment(TAG_ANSWER_FRAGMENT, show, true);
    }

    public void showPostCharWaitDialog(String callId, String chars) {
        if (isVisible()) {
            final PostCharDialogFragment fragment = new PostCharDialogFragment(callId,  chars);
            fragment.show(getFragmentManager(), "postCharWait");

            mShowPostCharWaitDialogOnResume = false;
            mShowPostCharWaitDialogCallId = null;
            mShowPostCharWaitDialogChars = null;
        } else {
            mShowPostCharWaitDialogOnResume = true;
            mShowPostCharWaitDialogCallId = callId;
            mShowPostCharWaitDialogChars = chars;
        }
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        if (mCallCardFragment != null) {
            mCallCardFragment.dispatchPopulateAccessibilityEvent(event);
        }
        return super.dispatchPopulateAccessibilityEvent(event);
    }

    public void maybeShowErrorDialogOnDisconnect(DisconnectCause disconnectCause) {
        Log.d(this, "maybeShowErrorDialogOnDisconnect");

        if (!isFinishing() && !TextUtils.isEmpty(disconnectCause.getDescription())
                && (disconnectCause.getCode() == DisconnectCause.ERROR ||
                        disconnectCause.getCode() == DisconnectCause.RESTRICTED)) {
            showErrorDialog(disconnectCause.getDescription());
        }
    }

    public void dismissPendingDialogs() {
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
        if (mAnswerFragment != null) {
            mAnswerFragment.dismissPendingDialogs();
        }
    }

    /**
     * Utility function to bring up a generic "error" dialog.
     */
    private void showErrorDialog(CharSequence msg) {
        Log.i(this, "Show Dialog: " + msg);

        dismissPendingDialogs();

        mDialog = new AlertDialog.Builder(this)
                .setMessage(msg)
                .setPositiveButton(android.R.string.ok, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        onDialogDismissed();
                    }})
                .setOnCancelListener(new OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        onDialogDismissed();
                    }})
                .create();

        mDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        mDialog.show();
    }

    private void onDialogDismissed() {
        mDialog = null;
        CallList.getInstance().onErrorDialogDismissed();
        InCallPresenter.getInstance().onDismissDialog();
    }

    public void setExcludeFromRecents(boolean exclude) {
        ActivityManager am = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.AppTask> tasks = am.getAppTasks();
        int taskId = getTaskId();
        for (int i=0; i<tasks.size(); i++) {
            ActivityManager.AppTask task = tasks.get(i);
            /* SPRD: modify for bug 512897 {@ */
            try {
                if (task.getTaskInfo().id == taskId) {
                    task.setExcludeFromRecents(exclude);
                }
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "IllegalArgumentException when getTaskInfo from recents.", e);
            } catch (RuntimeException e) {
                Log.e(TAG, "RuntimeException when excluding task from recents.", e);
            }
            /* @} */
        }
    }

    // ------------------- SPRD -------------------

    /** Add for recorder @{ */
    private OnStateChangedListener mRecorderStateChangedListener = new OnStateChangedListener() {
        public void onTimeChanged(long time) {
            // Add not null judgement for mCallCardFragment
            if (mCallCardFragment != null) {
                mCallCardFragment.setRecordText(mRecordingLabelStr
                        + DateUtils.formatElapsedTime(time / 1000));
            }

        }

        public void onStateChanged(PhoneRecorderHelper.State state) {
            setRecorderState(state);
            if (mCallButtonFragment != null) {
                mCallButtonFragment.setRecord(state.isActive());
            }
            if (mCallButtonFragment != null && mCallCardFragment.getRecordingLabel() != null) {
                mCallCardFragment
                        .setRecordingVisibility(state.isActive() ? View.VISIBLE
                                : View.GONE);
            }
        }

        @Override
        public void onShowMessage(int type, String msg) {
            String res = null;
            boolean resetIcon = false;
            switch (type) {
                case PhoneRecorderHelper.TYPE_ERROR_SD_NOT_EXIST:
                    res = getString(R.string.no_sd_card);
                    resetIcon = true;
                    break;
                case PhoneRecorderHelper.TYPE_ERROR_SD_FULL:
                    res = getString(R.string.storage_is_full);
                    resetIcon = true;
                    break;
                case PhoneRecorderHelper.TYPE_ERROR_SD_ACCESS:
                    res = getString(R.string.sdcard_access_error);
                    resetIcon = true;
                    break;
                case PhoneRecorderHelper.TYPE_ERROR_IN_RECORD:
                    res = getString(R.string.used_by_other_applications);
                    resetIcon = true;
                    break;
                case PhoneRecorderHelper.TYPE_ERROR_INTERNAL:
                    resetIcon = true;
                    break;
                case PhoneRecorderHelper.TYPE_MSG_PATH:
                case PhoneRecorderHelper.TYPE_SAVE_FAIL:
                    res = msg;
                    resetIcon = true;
                    break;
                case PhoneRecorderHelper.TYPE_NO_AVAILABLE_STORAGE:
                    res = getString(R.string.no_available_storage);
                    resetIcon = true;
                    break;
            }
            if (mCallButtonFragment != null && resetIcon) {
                mCallButtonFragment.setRecord(false);
            }
            Log.d(this, " toast message: " + res);
            if(!TextUtils.isEmpty(res)){
                Toast.makeText(InCallActivity.this, res, Toast.LENGTH_SHORT).show();
            }
        }
    };

    private void registerExternalStorageStateListener() {
        if (mSDCardMountEventReceiver == null) {
            mSDCardMountEventReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if(intent == null){
                        return;
                    }
                    String action = intent.getAction();
                    if (action.equals(Intent.ACTION_MEDIA_EJECT)) {
                        if (mRecorderHelper != null) {
                            mRecorderHelper.stop();
                            return;
                        }
                    }
                    boolean hasSdcard = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
                    if (mRecorderHelper != null && !hasSdcard) {
                        mRecorderHelper.stop();
                    }
                }
            };
            IntentFilter iFilter = new IntentFilter();
            iFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
            iFilter.addAction(Intent.ACTION_MEDIA_EJECT);
            iFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
            iFilter.addDataScheme("file");
            registerReceiver(mSDCardMountEventReceiver, iFilter);
        }
    }

    private void unRegisterExternalStorageStateListener() {
        if (mSDCardMountEventReceiver != null) {
            unregisterReceiver(mSDCardMountEventReceiver);
        }
    }

    // Modify permissions judgements for call recording
    // There are three permissions needed for recording, we need make sure dialer have all of these
    // permissions, if we grant part of these permissions,just request for the rest permissions
    public void toggleRecord() {
        if (PermissionsUtil.hasPermission(this, WRITE_EXTERNAL_STORAGE) && PermissionsUtil.hasPermission(this, READ_EXTERNAL_STORAGE)
                && PermissionsUtil.hasPermission(this, RECORD_AUDIO)) {
            InCallPresenter.getInstance().toggleRecorder();
        } else if (PermissionsUtil.hasPermission(this, RECORD_AUDIO)) {
            requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE}, MICROPHONE_AND_STORAGE_PERMISSION_REQUEST_CODE);
        } else if (PermissionsUtil.hasPermission(this, WRITE_EXTERNAL_STORAGE) && PermissionsUtil.hasPermission(this, READ_EXTERNAL_STORAGE)) {
            requestPermissions(new String[]{RECORD_AUDIO}, MICROPHONE_AND_STORAGE_PERMISSION_REQUEST_CODE);
        } else {
            requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE, RECORD_AUDIO}, MICROPHONE_AND_STORAGE_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == MICROPHONE_AND_STORAGE_PERMISSION_REQUEST_CODE) {
            // toggleRecorder since we were missing the permission before this.
            boolean isPermissionGranted = false;
            // Only if all requested permissions granted, can we toggleRecorder.
            if (grantResults.length > 0) {
                // grantResults's length greater than 0 means user has make a decision
                isPermissionGranted = true;
            }
            for (int i = 0; i < grantResults.length; i++) {
                isPermissionGranted = isPermissionGranted && grantResults[i] == PackageManager.PERMISSION_GRANTED;
            }
            if (isPermissionGranted) {
                InCallPresenter.getInstance().toggleRecorder();
            } else {
                Toast.makeText(this, R.string.permission_no_record, Toast.LENGTH_LONG).show();
            }
        }
    }

    public void setRecorderState(PhoneRecorderHelper.State state) {
        mRecorderState = state;
    }

    public PhoneRecorderHelper.State getRecorderState() {
        return mRecorderState;
    }
    /** @} */

    /**
     * Porting dealing with SS notification. @{
     * This enum maps to Phone.SuppService defined in telephony
     */
    private enum SuppService {
        UNKNOWN, SWITCH, SEPARATE, TRANSFER, CONFERENCE, REJECT, HANGUP, RESUME;
    }

    public class SuppServFailureNotificationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "Action: " + action);

            if (action.equals(ACTION_SUPP_SERVICE_FAILURE)) {
                int service = intent.getIntExtra("supp_serv_failure", 0);
                Log.d(TAG, "SuppServFailureNotificationReceiver: " + service);
                onSuppServiceFailed(service);
            }
        }
    }

    /**
      * Handle a failure notification for a supplementary service
      * (i.e. conference, switch, separate, transfer, etc.).
      */
    void onSuppServiceFailed(int service) {
        Log.d(TAG, "onSuppServiceFailed: " + service);
        SuppService  result = SuppService.values()[service];
        int errorMessageResId;

        switch (result) {
            case CONFERENCE:
            case RESUME:
                // Attempt to add a call to conference call failed
                // ("RESUME call failed")
                //has aready show in CallNotifier.java onSuppServiceFailed
                return;

            case SWITCH:
                // Attempt to switch foreground and background/incoming calls failed
                // ("Failed to switch or hold calls")
                final Call activeCall = CallList.getInstance().getActiveCall();
                if (activeCall != null && (CallList.getInstance().getCallSize() == 1)) {
                    errorMessageResId = R.string.incall_error_supp_service_hold;
                } else {
                    errorMessageResId = R.string.incall_error_supp_service_switch;
                }
                final Call call = CallList.getInstance().getActiveOrBackgroundCall();
                if (call != null && call.getState() != State.ONHOLD) {
                    mCallButtonFragment.setHold(false);
                }
                if (null != mAnswerFragment) {
                    mAnswerFragment.getPresenter().onSuppServiceFailed();
                }
                break;

            case SEPARATE:
                // Attempt to separate a call from a conference call
                // failed ("Failed to separate out call")
                errorMessageResId = R.string.incall_error_supp_service_separate;
                break;

            case TRANSFER:
                // Attempt to connect foreground and background calls to
                // each other (and hanging up user's line) failed ("Call
                // transfer failed")
                errorMessageResId = R.string.incall_error_supp_service_transfer;
                break;

            case REJECT:
                // Attempt to reject an incoming call failed
                // ("Call rejection failed")
                errorMessageResId = R.string.incall_error_supp_service_reject;
                break;

            case HANGUP:
                // Attempt to release a call failed ("Failed to release call(s)")
                errorMessageResId = R.string.incall_error_supp_service_hangup;
                break;

            case UNKNOWN:
            default:
                // Attempt to use a service we don't recognize or support
                // ("Unsupported service" or "Selected service failed")
                errorMessageResId = R.string.incall_error_supp_service_unknown;
                break;
        }
        final CharSequence msg = getResources().getText(errorMessageResId);
        showErrorDialog(msg);
    }
    /** @} */

    /** kill-stop mechanism BEGIN */
    @Override
    protected void onPostResume() {
        super.onPostResume();
        try {
            LowmemoryUtils.killStopFrontApp(ActivityManager.CANCEL_KILL_STOP_TIMEOUT);
        } catch (Exception e) {
            Log.e(this, "killStopFrontApp : CANCEL_KILL_STOP_TIMEOUT");
            e.printStackTrace();
        }
    }
    /** @} */

    /* add for bug 565897 dismiss pending dialogs when disconnected @{ */
    public void dismissPendingDialogsOfAnswerFragment() {
        if (mAnswerFragment != null) {
            mAnswerFragment.dismissPendingDialogs();
        }
    }
    /* @} */
    
    /* Sunvov:jiazhenl 20150609 add for call auto record start @{ */
	public void autoRecorder(){
		Log.d(this, "autoRecorder()...  is_auto_record = " + is_auto_record);
		if(is_auto_record && (IsStratRecording == false)){
			IsStratRecording = true;
			toggleRecord();
		}
	}

    /*SUN:jicong.wang add for vibrate when call connection start {@*/
    public void vibrateWhenCallConnection(){
        Log.d(this, "vibrate when call connection flag = " + is_vibrate_when_call_connection);
        if(is_vibrate_when_call_connection){
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Vibrator vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
                    vibrator.vibrate(100);
                }
            },0);            
        }
    }
    /*SUN:jicong.wang add for vibrate when call connection end @}*/
	public static boolean getCallSetAutoRecord(Context context) {
		boolean value = Settings.System.getInt(context.getContentResolver(),Settings.System.CALL_SET_AUTO_RECORD, 0) != 0;
		return value;
	}

	public PhoneRecorderHelper getRecorderHelper() {
		return mRecorderHelper;
	}
	/* Sunvov:jiazhenl 20150609 add for call auto record end @} */

    //qiuyaobo,20160716,SUNVOV_USE_HALL_LEATHER_FUNCTION,begin
    public void updateLidCallTime() {
	     Log.d(this, "qiuyaobo updateLidCallTime begin" );
    	 if (InCallPresenter.getInstance().getInCallState() == InCallState.INCALL) {
		      mwait_answer.setVisibility(View.INVISIBLE);
		      if(mCallCardFragment != null){//qiuyaobo,20160723
			    		moperatorNameOrCallElapsedTime.setText(mCallCardFragment.pelapsedtime);
			    		moperatorNameOrCallElapsedTime.setContentDescription(mCallCardFragment.pdurationDescription);
			    }		
    	 }else{
		        if((mCallCardFragment != null) && !TextUtils.isEmpty(mCallCardFragment.poperatorname)){
		    	       moperatorNameOrCallElapsedTime.setText(mCallCardFragment.poperatorname);
		    	  }else{
		             moperatorNameOrCallElapsedTime.setText(R.string.unknown);
		        }
			 }	
    }

    private void registerHallLeatherStateListener() {
        if (mHallEventReceiver == null) {
            mHallEventReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    Log.d(InCallActivity.this, "action: " + action);
                    boolean lidOpen=SystemProperties.getBoolean("persist.sys.lidopen",false);
                    if ("com.android.incallui.hal.ON".equals(action)) {		
                        updateInCallUI(lidOpen);
                    }else if ("com.android.incallui.hal.OFF".equals(action)) {       
                        updateInCallUI(lidOpen);
                    }
                }
            };
            IntentFilter iFilter = new IntentFilter();
            iFilter.addAction("com.android.incallui.hal.ON");
            iFilter.addAction("com.android.incallui.hal.OFF");
            registerReceiver(mHallEventReceiver, iFilter);
        }
    }

    private void unRegisterHallLeatherStateListener() {
        if (mHallEventReceiver != null) {
            unregisterReceiver(mHallEventReceiver);
        }
    }

    public void updateInCallUI(boolean lidOpen) 
    {
    		Log.d(this,"qiuyaobo InCallActivity *******updateInCallUI*****getInCallState()="+InCallPresenter.getInstance().getInCallState()+";lidOpen="+lidOpen);
		    if(lidOpen){
		    		 if(mCallCardFragment != null) 	//qiuyaobo,20160723
		    	   		mCallCardFragment.setVisible(false);//qiuyaobo,20160716
		    	   
		    	   if((mCallCardFragment != null) && !TextUtils.isEmpty(mCallCardFragment.pcallname)){
			    	     mcallName.setText(mCallCardFragment.pcallname);
				         mcallName.setSelected(true);			 
		    	   }else{
		             mcallName.setText(R.string.unknown);
		         }
		         if((mCallCardFragment != null) && !TextUtils.isEmpty(mCallCardFragment.poperatorname)){
		    	       moperatorNameOrCallElapsedTime.setText(mCallCardFragment.poperatorname);
		    	   }else{
		             moperatorNameOrCallElapsedTime.setText(R.string.unknown);
		         }
		         if((mCallCardFragment != null) && !TextUtils.isEmpty(mCallCardFragment.pphonenumber)){
		    	       mphoneNumber.setText(mCallCardFragment.pphonenumber);
		    	   }else{
		             mphoneNumber.setText(R.string.unknown);
		         }
		         mlidIncallScreenView.setVisibility(View.VISIBLE);
			       
		    	  if((mCallCardFragment != null) && (InCallPresenter.getInstance().getInCallState() == InCallState.INCALL)) {
				         moperatorNameOrCallElapsedTime.setText(mCallCardFragment.pelapsedtime);
		    		}
		    	  if (InCallPresenter.getInstance().getInCallState() == InCallState.INCOMING) {
		             mwait_answer.setVisibility(View.VISIBLE);
		        }else{
		             mwait_answer.setVisibility(View.INVISIBLE);
		        }
		    }else{
		    	  if(mCallCardFragment != null)
		    	  		mCallCardFragment.setVisible(true);//qiuyaobo,20160716
		    	  
		    	  mlidIncallScreenView.setVisibility(View.GONE);
		    }
    }

    public boolean getIsTimeShow(){
        boolean value = false;
				if (OptConfig.SUNVOV_USE_HALL_LEATHER_FUNCTION){
            Context ct = null;
            try{
                ct = createPackageContext("com.example.timeshowactivity", Context.CONTEXT_IGNORE_SECURITY); 
            } catch (NameNotFoundException e) { 
                e.printStackTrace(); 
            }
            if(ct != null){
                SharedPreferences sp = ct.getSharedPreferences("TIME_SHOW", Context.MODE_WORLD_WRITEABLE + Context.MODE_WORLD_READABLE);           
                value = sp.getBoolean("IS_SHOW", false);
            }
        }
        return value;
    }	 

    public void slideToAnswer(){
        //if ((mAnswerFragment != null) && (CallList.getInstance().getIncomingCall() != null)) {
    if(InCallPresenter.getInstance().getInCallState() == InCallState.INCOMING )
       {
          virbate();
          mAnswerFragment.onAnswer(MultiPartCallHelper.MPC_MODE_HB, VideoProfile.STATE_AUDIO_ONLY, this); 
        }
        //}
    }

    public void slideToReject(){
        if ((mAnswerFragment != null) && (CallList.getInstance().getIncomingCall() != null)) {
            mAnswerFragment.onDecline(this);
        }else{
            mCallCardFragment.getPresenter().endCallClicked();
        }
        updateInCallUI(true);
    }

    private void virbate(){
        Vibrator vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(200);
    }
    //qiuyaobo,20160716,SUNVOV_USE_HALL_LEATHER_FUNCTION,end
    	
}
