
package com.sprd.validationtools.itemstest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.media.AudioSystem;
import android.app.Activity;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.WindowManager;
import android.view.View;
import android.widget.Toast;
import android.widget.RadioButton;
import android.provider.Settings;

import com.sprd.validationtools.BaseActivity;
import com.sprd.validationtools.IATUtils;
import com.sprd.validationtools.R;
import android.os.SystemProperties;

import com.sprd.android.config.OptConfig;//qiuyaobo,20160727

public class PhoneLoopBackTest extends BaseActivity {
    private static final String TAG = "PhoneLoopBackTest";
    public byte mPLBTestFlag[] = new byte[1];
    public Handler mUihandler = new Handler();
    private RadioButton mRadioSpeaker = null;
    private RadioButton mRadioReceiver = null;
    private static final int LOOPBACK_NONE = 0;
    private static final int LOOPBACK_SPEAKER = 1;
    private static final int LOOPBACK_RECEIVER = 2;
    private int mCurLoopback = LOOPBACK_NONE;
    private int mBackupRingerMode = AudioManager.RINGER_MODE_NORMAL;

    private Object mLock = new Object();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.micphone_test);
        setTitle(R.string.phone_loopback_test);
        mRadioSpeaker = (RadioButton) findViewById(R.id.radio_speaker);
        mRadioReceiver = (RadioButton) findViewById(R.id.radio_earpiece);

        mRadioSpeaker.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                switchLoopback(LOOPBACK_SPEAKER);
            }
        });
        mRadioReceiver.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                switchLoopback(LOOPBACK_RECEIVER);
            }
        });
        if (AudioSystem.DEVICE_STATE_AVAILABLE != AudioSystem.getDeviceConnectionState(
                AudioManager.DEVICE_OUT_EARPIECE, "")) {
            mRadioReceiver.setVisibility(View.GONE);
        }
        
    }

    @Override
    protected void onResume() {
        super.onResume();
        startMmiAudio(mCurLoopback);

    }

    @Override
    protected void onPause() {
        rollbackMmiAudio(mCurLoopback);
        super.onPause();
    }

    private void switchLoopback(int loopbackType) {
        Log.i("PhoneLoopBackTest",
                "=== create thread to execute PhoneLoopBackTest switch command! ===");
        mRadioReceiver.setEnabled(false);
        mRadioSpeaker.setEnabled(false);

        if (loopbackType == LOOPBACK_RECEIVER) {
            new Thread() {
                public void run() {
                    String result = IATUtils.sendATCmd("AT+SSAM=0", "atchannel0");
                 //   String result = IATUtils.sendAtCmd("AT+SSAM=0");
                 //   setInMac(LOOPBACK_RECEIVER);
                    if (result.contains(IATUtils.AT_OK)) {
                        mUihandler.post(new Runnable() {
                            public void run() {
                                mRadioSpeaker.setEnabled(true);
                                mCurLoopback = LOOPBACK_RECEIVER;
                                mRadioReceiver.setChecked(true);
                                Toast.makeText(PhoneLoopBackTest.this,
                                        "Switch To Receiver LoopBack Success!",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        mUihandler.post(new Runnable() {
                            public void run() {
                                Toast.makeText(PhoneLoopBackTest.this,
                                        "Switch To Receiver LoopBack Fail!",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }.start();
        } else {
            new Thread() {
                public void run() {
                    String result = IATUtils.sendATCmd("AT+SSAM=1", "atchannel0");
                   // String result = IATUtils.sendAtCmd("AT+SSAM=1");
                 //   setInMac(LOOPBACK_SPEAKER);
                    if (result.contains(IATUtils.AT_OK)) {
                        mUihandler.post(new Runnable() {
                            public void run() {
                                mRadioReceiver.setEnabled(true);
                                mCurLoopback = LOOPBACK_SPEAKER;
                                mRadioSpeaker.setChecked(true);
                                Toast.makeText(PhoneLoopBackTest.this,
                                        "Switch To Speaker LoopBack Success!",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        mUihandler.post(new Runnable() {
                            public void run() {
                                Toast.makeText(PhoneLoopBackTest.this,
                                        "Switch To Speaker LoopBack Fail!",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }.start();
        }
    }

    private boolean setInMac(int loopbackType) {
        String result = null;
        if (loopbackType == LOOPBACK_SPEAKER) {
            //result = IATUtils.sendATCmd("AT+SPVLOOP=4,,,,,,2,1", "atchannel0");
            result = IATUtils.sendAtCmd("AT+SPVLOOP=4,,,,,,2,1");
        } else {
            //result = IATUtils.sendATCmd("AT+SPVLOOP=4,,,,,,2,2", "atchannel0");
            result = IATUtils.sendAtCmd("AT+SPVLOOP=4,,,,,,2,2");
        }

        //if (result != null && result.length() >= 0 && result.contains(IATUtils.AT_OK)) {
        if (result != null && result.length() >= 0 && IATUtils.AT_OK.contains(result)) {
            return true;
        }
        return false;
    }

    private void startMmiAudio(int loopbackType) {
        Log.i("PhoneLoopBackTest",
                "=== create thread to execute PhoneLoopBackTest start command! ===");
        mRadioReceiver.setEnabled(false);
        mRadioSpeaker.setEnabled(false);
        if (loopbackType == LOOPBACK_RECEIVER) {
            new Thread() {
                public void run() {
                    synchronized(mLock) {
                    try {
                        sleep(1500);
                    } catch (Exception e) {

                    }
                    String result = IATUtils.sendATCmd("AT+SPVLOOP=1,0,8,2,3,0", "atchannel0");
                //    String result = IATUtils.sendAtCmd("AT+SPVLOOP=1,0,8,2,3,0");
                  //  setInMac(LOOPBACK_RECEIVER);
                    if (result.contains(IATUtils.AT_OK)) {
                        mUihandler.post(new Runnable() {
                            public void run() {
                                mRadioSpeaker.setEnabled(true);
                                mCurLoopback = LOOPBACK_RECEIVER;
                                mRadioReceiver.setChecked(true);
                                Toast.makeText(PhoneLoopBackTest.this, "Receiver LoopBack Opened!",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        mUihandler.post(new Runnable() {
                            public void run() {
                                Toast.makeText(PhoneLoopBackTest.this,
                                        "Receiver LoopBack Init Fail!",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } 
                    }
            }.start();
        } else {
            new Thread() {
                public void run() {
                    try {
                        sleep(1500);
                    } catch (Exception e) {

                    }
                    String result = IATUtils.sendATCmd("AT+SPVLOOP=1,1,8,2,3,0", "atchannel0");
                   // String result = IATUtils.sendAtCmd("AT+SPVLOOP=1,1,8,2,3,0");
                  //  setInMac(LOOPBACK_SPEAKER);
                    if (result.contains(IATUtils.AT_OK)) {
                        mUihandler.post(new Runnable() {
                            public void run() {
                                mRadioReceiver.setEnabled(true);
                                mCurLoopback = LOOPBACK_SPEAKER;
                                mRadioSpeaker.setChecked(true);
                                Toast.makeText(PhoneLoopBackTest.this, "Speaker LoopBack Opened!",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        mUihandler.post(new Runnable() {
                            public void run() {
                                Toast.makeText(PhoneLoopBackTest.this,
                                        "Speaker LoopBack Init Fail!",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }.start();
        }
    }

    private void rollbackMmiAudio(int loopbackType) {
        Log.i("PhoneLoopBackTest",
                "=== create thread to execute PhoneLoopBackTest stop command! ===");
        if (loopbackType == LOOPBACK_RECEIVER) {
            new Thread() {
                public void run() {
                    //String result = IATUtils.sendATCmd("AT+SPVLOOP=0,0,8,2,3,0", "atchannel0");
                    //jiazhenl 20150828 add for Bug36890 begin
                    try {
                        sleep(1500);
                    } catch (Exception e) {

                    }
                    //jiazhenl 20150828 add for Bug36890 end
                    String result = IATUtils.sendATCmd("AT+SPVLOOP=0,0,8,2,3,0", "atchannel0");
                 //   String result = IATUtils.sendAtCmd("AT+SPVLOOP=0,0,8,2,3,0");
                    Log.d(TAG, result);
                }
            }.start();
        } else {
            new Thread() {
                public void run() {
                    //String result = IATUtils.sendATCmd("AT+SPVLOOP=0,1,8,2,3,0", "atchannel0");
                    //jiazhenl 20150828 add for Bug36890 begin
                    try {
                        sleep(1500);
                    } catch (Exception e) {

                    }
                    //jiazhenl 20150828 add for Bug36890 end
                    String result = IATUtils.sendATCmd("AT+SPVLOOP=0,1,8,2,3,0", "atchannel0");
                 //   String result = IATUtils.sendAtCmd("AT+SPVLOOP=0,1,8,2,3,0");
                    Log.d(TAG, result);
                }
            }.start();
        }
    }
@Override
    public void onDestroy() {
        super.onDestroy();
    }
//    @Override
//    public void onBackPressed() {
//        showResultDialog(getString(R.string.phone_loopback_result_check));
//    }
}
