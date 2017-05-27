package com.android.agingtest;

import android.app.Activity;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.Timer;
import java.util.TimerTask;
import android.view.WindowManager;

public class DoCameraTest extends Activity {
	final private String TAG = "DoCameraTest";
	final static int cameraCount = Camera.getNumberOfCameras();
	private static boolean mBackCamera = true;
	private Camera mCamera;
	private Timer mChangeTimer;
	private SurfaceView surfaceView;
	private SurfaceHolder surfaceHolder;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.cameratest);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); //yanghua add
		surfaceView = (SurfaceView) findViewById(R.id.camera_view);
		surfaceHolder = surfaceView.getHolder();
		surfaceHolder.addCallback(surfaceCallback);
		surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		if(cameraCount > 1){
			if(mChangeTimer == null){
				mChangeTimer = new Timer();
			}
			mChangeTimer.schedule(new TimerTask() {
				public void run() {
					Message msg = new Message();
					msg.what = 0;
					myHandler.sendMessage(msg);
				}
			}, 7000, 7000);
		}
	}

	protected void onDestroy() {
		super.onDestroy();
		destory();
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

	Handler myHandler = new Handler() {
		public void handleMessage(Message msg) {
			if(msg.what == 0){
				mBackCamera = !mBackCamera;
				changeCamera();
			}
		}
	};

	private void destory(){
		if(mChangeTimer != null){
			mChangeTimer.cancel();
			mChangeTimer = null;
		}
		if (mCamera != null) {
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;
		}
	}

	SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {
		public void surfaceCreated(SurfaceHolder holder) {
			CameraInfo cameraInfo = new Camera.CameraInfo();
			for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
				Camera.getCameraInfo(camIdx, cameraInfo);
				if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
					try {
						mCamera = Camera.open(camIdx);
						mCamera.setPreviewDisplay(holder);
						mCamera.startPreview();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
		}            
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		if (mCamera != null) {
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;
		}
		}
	};

	private void changeCamera(){
		CameraInfo cameraInfo = new CameraInfo();
		int camIdx = 0;

		for(camIdx=0; camIdx < cameraCount; camIdx++) {
			Camera.getCameraInfo(camIdx, cameraInfo);
			if(mBackCamera) {
				//change to back camera
				if(cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
					break;
				}
			} else {//change to front camera
				if(cameraInfo.facing  == Camera.CameraInfo.CAMERA_FACING_FRONT) {
					break;
				}
			}
		}
		Log.d(TAG,"mBackCamera="+mBackCamera+ " camIdx="+camIdx);
		if(camIdx < cameraCount){
			if (mCamera != null) {
				mCamera.stopPreview();
				mCamera.release();
				mCamera = null;
			}
			mCamera = Camera.open(camIdx);
			try {
				mCamera.setPreviewDisplay(surfaceHolder);
			} catch (Exception e) {
				e.printStackTrace();
			}
			mCamera.startPreview();
		}
	}
}
