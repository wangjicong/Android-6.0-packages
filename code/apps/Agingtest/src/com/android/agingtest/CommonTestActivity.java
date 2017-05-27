package com.android.agingtest;

import android.app.Activity;
import android.content.Intent;//wxh add 20161118
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.TextView;
import android.widget.Toast;

import com.android.agingtest.SubFlashlightController;//wxh add 20161118
import com.sprd.android.config.AgingTestUtils;
//import com.mediatek.media.MediaRecorderEx;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;
import android.view.WindowManager;

public class CommonTestActivity extends Activity {
	final private String TAG = "CommonTestActivity";
	final private String mRecordDc = Environment.getExternalStorageState();
	final private String mRecordSavePath = Environment.getExternalStorageDirectory() + File.separator + "test.mp3";
	private TextView mText1 = null;
	private int mNum = 0;
	private Timer mLcdTimer,mRecordTimer,mFlashTimer;//wxh add mFlashTimer 20161118
	private Vibrator mVibrator;
	private MediaPlayer mPlayer;
	private AudioManager mAudioManager;
	private MediaRecorder mRecorder;
	private Camera mCamera;
	private Message msg = null;
	private static boolean mBackFlashlight = true;//wxh add 20161118
	private SubFlashlightController mSubFlashlightController;//wxh add 20161118
	private boolean hasFrontFlashlight = false;//wxh add 20161118

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.doagingtest);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); //yanghua add
		initView();
	}

	protected void onDestroy() {
		super.onDestroy();
		destory();
	}

	private void destory(){
		if(mLcdTimer != null){
			mLcdTimer.cancel();
			mLcdTimer = null;
		}
		if(mPlayer != null){
			mPlayer.stop();
			mPlayer.release();  
			mPlayer = null;
		}
		if(mVibrator != null){
			mVibrator.cancel();
			mVibrator = null;
		}
		if(AgingTestUtils.mAgingTest[AgingTestUtils.earpieceTest]){
			mAudioManager.setMode(AudioManager.MODE_NORMAL);
		}
		recorderClose();
		if(mRecordTimer != null){
			mRecordTimer.cancel();
			mRecordTimer = null;
		}
		if(mFlashTimer != null){//wxh add begin 20161118
			mFlashTimer.cancel();
			mFlashTimer = null;
		}//wxh add end 20161118
		flashLightControl(mBackFlashlight,false);//wxh add 20161118
		//flashLightClose();//wxh delete 20161118
	}

	Handler myHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch(msg.what){
				case AgingTestUtils.lcdTest:
					mNum++;
					changeColor(mNum);
					break;
				case AgingTestUtils.micTest:
					if(mRecorder == null){
						Log.d(TAG,"startRecorder");
						startRecorder();
					}else{
						Log.d(TAG,"stopRecorderAndSave");
						stopRecorderAndSave();
					}
					break;
				case AgingTestUtils.flashlightTest://wxh add begin 20161118
					mBackFlashlight = !mBackFlashlight;
					changeFlashLight();
					break;//wxh add end 20161118
				default:break;
			}
		}
	};

	private void initView() {
		mText1 = (TextView) findViewById(R.id.test_color_text1);
		mText1.setBackgroundColor(Color.BLACK);
		//lcd
		if(AgingTestUtils.mAgingTest[AgingTestUtils.lcdTest]){
			if(mLcdTimer == null){
				mLcdTimer = new Timer();
			}
			mLcdTimer.schedule(new TimerTask() {
				public void run() {
					msg = new Message();
					msg.what = AgingTestUtils.lcdTest;
					myHandler.sendMessage(msg);
				}
			}, 1000, 1000);
		}
		//speaker\mic\earpiece
		if(AgingTestUtils.mAgingTest[AgingTestUtils.speakerTest]){//speaker
			setVolumeControlStream(AudioManager.STREAM_MUSIC);
			if(mPlayer == null){
				mPlayer = MediaPlayer.create(this, R.raw.shilian33tian);
			}
			mPlayer.setLooping(true);
			mPlayer.start();
		}else if(AgingTestUtils.mAgingTest[AgingTestUtils.micTest]){//mic
			if(mRecordTimer == null){
				mRecordTimer = new Timer();
			}
			mRecordTimer.schedule(new TimerTask() {
				public void run() {
					msg = new Message();
					msg.what = AgingTestUtils.micTest;
					myHandler.sendMessage(msg);
				}
			}, 0, 7000);
		}else if(AgingTestUtils.mAgingTest[AgingTestUtils.earpieceTest]){//earpiece
			mAudioManager = (AudioManager) this.getSystemService(AUDIO_SERVICE);
			mAudioManager.setSpeakerphoneOn(false);
			mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
			setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
			if(mPlayer == null){
				mPlayer = MediaPlayer.create(this, R.raw.shilian33tian);
			}
			mPlayer.setLooping(true);
			mPlayer.start();
		}
		//vibrator
		if(AgingTestUtils.mAgingTest[AgingTestUtils.vibratorTest]){
			if(mVibrator == null){
				mVibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
			}
			long[] pattern = {1000,1000}; 
			mVibrator.vibrate(pattern,0);
		}
		//flashlight
		if(AgingTestUtils.mAgingTest[AgingTestUtils.flashlightTest]){
			hasFrontFlashlight = getResources().getBoolean(R.bool.has_front_flashlight);//wxh add begin 20161118
			Log.d(TAG, "hasFrontFlashlight="+hasFrontFlashlight);
			if(hasFrontFlashlight){
				mSubFlashlightController = new SubFlashlightController(this);
				if(mFlashTimer == null){
					mFlashTimer = new Timer();
				}
				mFlashTimer.schedule(new TimerTask() {
					public void run() {
						msg = new Message();
						msg.what = AgingTestUtils.flashlightTest;
						myHandler.sendMessage(msg);
					}
				}, 5000, 5000);
			}
			flashLightControl(mBackFlashlight,true);//wxh add end 20161118
		}
	}

	private void changeColor(int num) {
		switch (num % 5) {
			case 0:
				mText1.setBackgroundColor(Color.RED);
				break;
			case 1:
				mText1.setBackgroundColor(Color.GREEN);
				break;
			case 2:
				mText1.setBackgroundColor(Color.BLUE);
				break;
			case 3:
				mText1.setBackgroundColor(Color.WHITE);
				break;
			default:
				mText1.setBackgroundColor(Color.BLACK);
				break;
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event){
		if (keyCode == KeyEvent.KEYCODE_BACK ){
			destory();
			finish();
			return true;
		}
		return false;
	}
	private void flashLightControl(boolean isbackFlash, boolean isOpen){//wxh add begin 20161118
		Log.d("huangff", "isbackFlash="+isbackFlash+" isOpen="+isOpen);
		String broadMessage;
		if(isbackFlash){
			if(isOpen){
				broadMessage = "LedTroch_OpenLight";
			}else{
				broadMessage = "LedTroch_CloseLight";
			}
			Intent intent = new Intent(broadMessage);
			sendBroadcast(intent);
		}else{
			if(mSubFlashlightController != null){
				mSubFlashlightController.setFlashlight(isOpen);
			}
		}
	}
	private void changeFlashLight(){
		new Thread() {
			public void run() {
				flashLightControl(!mBackFlashlight,false);
				try {
					sleep(1000);
				} catch (Exception e) {
				}
				flashLightControl(mBackFlashlight,true);
			}
		}.start();
	}//wxh add end 20161118

	private void startRecorder() {
		if (mPlayer != null) {
			mPlayer.stop();
			mPlayer.release();
			mPlayer = null;
		}
		if (!mRecordDc.equals(Environment.MEDIA_MOUNTED)) {
			Log.d(TAG,"record memory unMOUNTED");
			return;
		}
		try {
			mRecorder = new MediaRecorder();
			mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
			mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
			//MediaRecorderEx.setHDRecordMode(mRecorder, 0, false);
			mRecorder.setAudioEncoder(3);
			mRecorder.setAudioChannels(2);
			mRecorder.setAudioEncodingBitRate(128000);
			mRecorder.setAudioSamplingRate(48000);
			final File mRecordSaveFile = new File(mRecordSavePath);
			if (!mRecordSaveFile.exists()){
				mRecordSaveFile.createNewFile();
			}
			mRecorder.setOutputFile(mRecordSavePath);
			mRecorder.prepare();
			mRecorder.start();
		} catch (Exception e) {
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT);
		}
	}

	private void stopRecorderAndSave() {
		recorderClose();
		try {
			if (mPlayer == null) {
				mPlayer = new MediaPlayer();
			}
			mPlayer.setDataSource(mRecordSavePath);
			mPlayer.prepare();
			mPlayer.start();
		} catch (Exception e) {
			e.printStackTrace();
		}	
	}

	private void recorderClose(){
		if(mRecorder != null){
			mRecorder.stop();
			mRecorder.release();
			mRecorder = null;
		}
	}

	private void flashLightOpen(){
		if(mCamera == null) {
			try {
				mCamera = mCamera.open();
			} catch (RuntimeException e) {
				e.printStackTrace();
			}
		}

		if(mCamera == null) {
			Toast.makeText(this, R.string.flash_open_fail, Toast.LENGTH_SHORT).show();
			return;
		}

		Parameters params = mCamera.getParameters();
		params.setFlashMode(Parameters.FLASH_MODE_TORCH);
		mCamera.setParameters(params);
		mCamera.startPreview(); 
	}

	private void flashLightClose(){
		if(mCamera != null){
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;
		}
	}	
}
