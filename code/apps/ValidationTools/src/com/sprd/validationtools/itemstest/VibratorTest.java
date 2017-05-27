/* Sunvov:lzz 20150618 add for NewTest start @{ */
package com.sprd.validationtools.itemstest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.media.AudioSystem;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.sprd.validationtools.BaseActivity;
import com.sprd.validationtools.R;

public class VibratorTest extends BaseActivity {

    TextView mContent;
    private int backupMode = 0;
    private List<String> mFilePaths;
    private File mFile;
    MediaPlayer mPlayer = null;
    private Vibrator mVibrator = null;
    private static final long V_TIME = 2000;
    private static final long DIALOG_TIME = 3000;
    private static final String DEFAULT_AUDIO = "Kuma.ogg";
    private boolean isSearchFinished = false;
    private Runnable mR = new Runnable() {
        public void run() {
            if (mPlayer != null) {
                //showResultDialog(getString(R.string.melody_play_info));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mContent = new TextView(this);
        mContent.setGravity(Gravity.CENTER);
        mContent.setTextSize(25);
        setContentView(mContent);
        setTitle(R.string.vibrator_test);
        mFilePaths = new ArrayList<String>();
        mPlayer = new MediaPlayer();
        mVibrator = (Vibrator) getSystemService(this.VIBRATOR_SERVICE);

        mContent.setText("Vibrator testing...");
        new Thread() {
            public void run() {
                if (checkSDCard()) {
                    mFile = Environment.getExternalStoragePath();
                    toSearchFiles(mFile);

                    if (mFilePaths.size() != 0) {
                        mHandler.sendEmptyMessage(SEARCH_FINISHED);
                        return;
                    }
                }

                File firstAudio = new File("/system/media/audio/ringtones", DEFAULT_AUDIO);
                if (firstAudio.exists()) {
                    mFilePaths.add(firstAudio.getPath());
                } else {
                    mFile = new File("/system/media/audio/ringtones");
                    toSearchFiles(mFile);
                }

                mHandler.sendEmptyMessage(SEARCH_FINISHED);
            }
        }.start();
    }

    private final int SEARCH_FINISHED = 0;
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SEARCH_FINISHED:
                    isSearchFinished = true;
                    doPlay();
                    mHandler.postDelayed(mR, DIALOG_TIME);
                    break;
            }
        }
    };

    private void doPlay() {

        int audioNumber = getRandom(mFilePaths.size());
        if (mPlayer == null) {
            return;
        }
        try {
            mPlayer.setDataSource(mFilePaths.get(audioNumber));
            mPlayer.prepare();
        } catch (IllegalArgumentException e) {
            /*SPRD: fix bug350197 setDataSource fail due to crash @{*/
//            mPlayer = null;
            /* @}*/
            e.printStackTrace();
        } catch (IllegalStateException e) {
//            mPlayer = null;
            e.printStackTrace();
        } catch (IOException e) {
//            mPlayer = null;
            e.printStackTrace();
        }
        //mPlayer.start();
        AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        if (AudioSystem.DEVICE_STATE_AVAILABLE == AudioSystem.getDeviceConnectionState(
                AudioManager.DEVICE_OUT_EARPIECE, "")) {
            audioManager.setMode(AudioManager.MODE_IN_CALL);
        }
        mPlayer.setVolume(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        is_need_vibrate =true;
        //sunvov:dlj for Sleep three seconds after the vibration start 150815
        mVibratorHandler.sendEmptyMessageDelayed(1, 0);
        //sunvov:dlj for Sleep three seconds after the vibration end 150815
        //mContent.setText(getResources().getText(R.string.melody_play_tag)
                //+ mFilePaths.get(audioNumber));
    }

    @Override
    protected void onResume() {
        super.onResume();
        AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        backupMode = audioManager.getMode();
        if (mPlayer == null) {
            mPlayer = new MediaPlayer();
        }
        if (isSearchFinished) {
            doPlay();
            mHandler.postDelayed(mR, DIALOG_TIME);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        is_need_vibrate =false;
        if (mPlayer == null) {
            return;
        }
        mPlayer.stop();
        mPlayer.release();
        mPlayer = null;
        if (mVibrator != null) {
            mVibrator.cancel();
        }
        AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(backupMode);
    }

    private boolean checkSDCard() {
        boolean hasSDCard = false;
        hasSDCard = Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED);
        return hasSDCard;
    }

    public void toSearchFiles(File file) {
        File[] files = file.listFiles();
        if (files == null) {
            return;
        }
        for (File tf : files) {
            if (tf.isDirectory()) {
                toSearchFiles(tf);
            } else {
                try {
                    if (tf.getName().indexOf(".mp3") > -1) {
                        mFilePaths.add(tf.getPath());
                    }
                    if (tf.getName().indexOf(".ogg") > -1) {
                        mFilePaths.add(tf.getPath());
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "pathError", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private int getRandom(int max) {
        double random = Math.random();
        int result = (int) Math.floor(random * max);
        return result;
    }

    @Override
    public void onDestroy() {
        mHandler.removeCallbacks(mR);
        super.onDestroy();
    }

    private static final int DELAY_TIME =3000;
    private static final int MAX_V_COUNT =100000;
    private   boolean is_need_vibrate =false;
    private  int mVibratorCount =0;
	    
    Handler mVibratorHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (is_need_vibrate && mVibratorCount<MAX_V_COUNT){
                mVibratorCount++;
                mVibrator.vibrate(V_TIME);
                mVibratorHandler.sendEmptyMessageDelayed(1, DELAY_TIME);
            }
        }
    };
	
}
/* Sunvov:lzz 20150618 add for NewTest end @} */