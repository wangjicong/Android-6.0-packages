
package com.sprd.engineermode.connectivity.fm;

import java.util.ArrayList;

import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import com.android.fmradio.FmNative;

import android.media.AudioDevicePort;
import android.media.AudioDevicePortConfig;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.AudioManager.OnAudioPortUpdateListener;
import android.media.AudioMixPort;
import android.media.AudioPatch;
import android.media.AudioPort;
import android.media.AudioPortConfig;
import android.media.AudioSystem;
import android.media.MediaRecorder;
import android.app.Activity;

import com.sprd.engineermode.R;

public class EarphoneFragment extends AbsFMFragment implements OnClickListener {
    private static final int RADIO_AUDIO_DEVICE_WIRED_HEADSET = 0;

    private static final float POWER_UP_START_FREQUENCY = 87.5f;

    private Button mEarphonePlayButton;
    private Button mExtrovertedPlayButton;
    private Button mSwitchEarphoneaAndExtroverButton;
    private EditText mSwitchEarphoneaAndExtroverEditText;

    private ProgressDialog mSearchStationDialog = null;
    private boolean isFmOn = false;
    private int isFmPro = 0;
    private Context mContext;

    private static int switchType = 0;

    private static int mSwitchCounts = 0;

    private static final String TAG = "EarphoneFragment";

    private static final int MSG_POWER_UP = 0;

    private Handler switchEarphoneaAndExtroverHandler = new Handler();

    public Runnable switchEarphoneaAndExtroverRunnable = new Runnable() {

        @Override
        public void run() {
            if (isFmPro == 0) {
                isFmPro = 1;
                startPowerUpFM();
            }
            if (mSwitchCounts == getSwitchCounts()) {
                mSwitchCounts = 0;
                mSearchStationDialog.cancel();
                return;
            }

            switchType = mSwitchCounts % 2;
            setFMPlayerRoute(switchType);
            mSwitchCounts++;

            switchEarphoneaAndExtroverHandler.postDelayed(switchEarphoneaAndExtroverRunnable, 2000);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        mContext = getActivity();
        getActivity().setVolumeControlStream(AudioManager.STREAM_FM);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG,"onResume");
        startRender();
        if (!mEarphonePlayButton.isEnabled()) {
            if (isFmPro == 0) {
                startPowerUpFM();
            }
            setFMPlayerRoute(0);
        }
        if (!mExtrovertedPlayButton.isEnabled()) {
            if (isFmPro == 0) {
                startPowerUpFM();
            }
            setFMPlayerRoute(1);
        }
        if (!mSwitchEarphoneaAndExtroverEditText.getText().toString().equals("")) {
            if (isFmPro == 0) {
                startPowerUpFM();
            }
            if (mSwitchCounts != 0) {
                startSwitchEarphoneAndExtroverted();
            } else {
                setFMPlayerRoute(switchType);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return initView(inflater);
    }

    @SuppressWarnings("deprecation")
    private View initView(LayoutInflater inflater) {
        View view = inflater.inflate(R.layout.earphone_fragment_main, null);
        mEarphonePlayButton = (Button) view.findViewById(R.id.earphone_play);
        mExtrovertedPlayButton = (Button) view.findViewById(R.id.extroverted_play);
        mSwitchEarphoneaAndExtroverEditText = (EditText) view
                .findViewById(R.id.earphone_and_extroverted_switch_edittext);
        mEarphonePlayButton.setOnClickListener(this);
        mExtrovertedPlayButton.setOnClickListener(this);
        mSwitchEarphoneaAndExtroverButton = (Button) view
                .findViewById(R.id.earphone_and_extroverted_switch_start_switch);
        mSwitchEarphoneaAndExtroverButton.setOnClickListener(this);
        initProgressDialog();
        return view;
    }

    public void initProgressDialog() {
        mSearchStationDialog = new ProgressDialog(getActivity());
        mSearchStationDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mSearchStationDialog.setMessage(getResources().getString(
                R.string.earphone_and_extroverted_switch_underway));
        mSearchStationDialog.setIndeterminate(false);
        mSearchStationDialog.setCancelable(false);
        mSearchStationDialog.setButton(getResources().getString(R.string.fm_cancel),
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switchEarphoneaAndExtroverHandler
                                .removeCallbacks(switchEarphoneaAndExtroverRunnable);
                        if(isFmPro !=0){
                            powerOffFM();
                        }
                        dialog.dismiss();
                    }
                });
        mSearchStationDialog.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(DialogInterface dialoge, int keyCode,
                    KeyEvent event) {
                if (KeyEvent.KEYCODE_SEARCH == keyCode || KeyEvent.KEYCODE_HOME == keyCode) {
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.earphone_play:
                mExtrovertedPlayButton.setEnabled(true);
                mEarphonePlayButton.setEnabled(false);
                setFMPlayerRoute(0);
                break;
            case R.id.extroverted_play:
                mEarphonePlayButton.setEnabled(true);
                mExtrovertedPlayButton.setEnabled(false);
                setFMPlayerRoute(1);
                break;
            case R.id.earphone_and_extroverted_switch_start_switch:
                if (mSwitchEarphoneaAndExtroverEditText.getText().toString().equals("")) {
                    Toast.makeText(
                            getActivity(),
                            getResources().getString(
                                    R.string.earphone_and_extroverted_switch_counts_string),
                            Toast.LENGTH_LONG).show();
                    return;
                }
                startSwitchEarphoneAndExtroverted();
                if (!mSearchStationDialog.isShowing()) {
                    mSearchStationDialog.show();
                }
            default:
                break;
        }
    }

    public void startSwitchEarphoneAndExtroverted() {
        switchEarphoneaAndExtroverHandler.post(switchEarphoneaAndExtroverRunnable);
    }

    public int getSwitchCounts() {
        return Integer.parseInt(mSwitchEarphoneaAndExtroverEditText.getText().toString());
    }

    public void setFMPlayerRoute(int headsetOrSpeaker) {
        Log.d(TAG,"set fm route");
        if (isFmPro==0) {
            isFmPro =1;
            startPowerUpFM();
            return;
        }
        setFMAudioPath(headsetOrSpeaker);
        FmNative.setMute(true);
        FmNative.setRds(false);
        FmNative.tune((float)106.8);
        FmNative.setRds(true);
        FmNative.setMute(false);
    }

    public void setFMAudioPath(int headsetOrSpeaker) {
        Log.d(TAG,"set fm audio path: " + headsetOrSpeaker);
        if (headsetOrSpeaker == RADIO_AUDIO_DEVICE_WIRED_HEADSET) {
            AudioSystem.setForceUse(AudioSystem.FOR_FM, AudioSystem.FORCE_NONE);
        } else {
            AudioSystem.setForceUse(AudioSystem.FOR_FM, AudioSystem.FORCE_SPEAKER);
        }
    }

    public  void startPowerUpFM() {
        new StartPowerUpThread().start();
    }

    public  void powerOnFM() {
        Log.d(TAG, "startPowerUp");
        boolean value = false;
        FmNative.setMute(true);
        value = FmNative.openDev();
        if (!value) {
            Log.d(TAG, "powerUp fail");
            isFmPro = 0;
            return;
        }
        value = FmNative.powerUp(POWER_UP_START_FREQUENCY);
        if (!value) {
            Log.e(TAG, "powerUp fail ");
            isFmPro = 0;
            return;
        }
        isFmPro = 2;
        Log.d(TAG, "sendMessage MSG_POWER_UP");
        mHandler.sendMessage(mHandler.obtainMessage(MSG_POWER_UP));
    }

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_POWER_UP:
                    powerUpComplete();
                    break;
                default:
            }
        }
    };

    private void powerUpComplete() {
        Log.d(TAG,"power up complete");
        if (!mEarphonePlayButton.isEnabled()) {
            setFMPlayerRoute(0);
        }
        if (!mExtrovertedPlayButton.isEnabled()) {
            setFMPlayerRoute(1);
        }
        if (!mSwitchEarphoneaAndExtroverEditText.getText().toString().equals("")) {
            setFMPlayerRoute(switchType);
        }
    }

    class StartPowerUpThread extends Thread {
        public void run() {
            powerOnFM();
        };
    };

    private synchronized void startRender() {
        Log.d(TAG, "startRender ");

        // need to create new audio record and audio play back track,
        // because input/output device may be changed.
        /**
         * SPRD: bug492835, FM audio route change.
         *
         * @{
         */
        AudioSystem.setDeviceConnectionState(
                AudioManager.DEVICE_OUT_FM_HEADSET,
                AudioSystem.DEVICE_STATE_AVAILABLE, "", "");
    }

    private synchronized void stopRender() {
        Log.d(TAG, "stopRender");
        /**
         * SPRD: bug492835, FM audio route change.
         *
         * @{
         */
        AudioSystem.setDeviceConnectionState(
                AudioManager.DEVICE_OUT_FM_HEADSET,
                AudioSystem.DEVICE_STATE_UNAVAILABLE, "", "");
        AudioSystem.setForceUse(AudioSystem.FOR_FM, AudioSystem.FORCE_NONE);
    }

    public void powerOffFM() {
        Log.d(TAG, "power off fm");
        try {
            while (isFmPro == 1) {
                Thread.sleep(1000);
            }
        } catch (Exception e) {
                Log.d(TAG, "" + e);
        }

        FmNative.setMute(true);
        FmNative.setRds(false);
        FmNative.powerDown(0);
        FmNative.closeDev();
        isFmPro = 0;
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG,"onPause");
        if(isFmPro !=0){
            switchEarphoneaAndExtroverHandler.removeCallbacks(switchEarphoneaAndExtroverRunnable);
            stopRender();
            powerOffFM();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
