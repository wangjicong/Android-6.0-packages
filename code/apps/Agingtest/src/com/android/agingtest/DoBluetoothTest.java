package com.android.agingtest;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.view.KeyEvent;
import android.widget.TextView;

import com.sprd.android.config.AgingTestUtils;

import java.util.Timer;
import java.util.TimerTask;
import android.view.WindowManager;

public class DoBluetoothTest extends Activity {
	private TextView mTvInfo,mTvResult,mTvCon;
	private HandlerThread mBlueThread = new HandlerThread("blueThread");
	private Handler mBlueHandler;
	private BluetoothAdapter mAdapter = null;
	private Timer mBlueTimer;
	private Message msg = null;
	private boolean mBlueFlag = false;
	private boolean mBlueOn = true;
	private String mNameList = "";

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.bluetooth_test);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); //yanghua add
		((TextView) findViewById(R.id.bluetooth_interval)).setText(getString(R.string.test_interval,
			getString(R.string.bluetooth_name)));
		mTvInfo = (TextView) findViewById(R.id.ble_state_id);
		mTvResult = (TextView) findViewById(R.id.ble_result_id);
		mTvCon = (TextView) findViewById(R.id.ble_con_id);
		mAdapter = BluetoothAdapter.getDefaultAdapter();
		mBlueThread.start();
		mBlueHandler = new Handler(mBlueThread.getLooper());
		mBlueHandler.post(blueRunnable);
		mTvInfo.setText(R.string.Bluetooth_opening);
		if(mBlueTimer == null){
			mBlueTimer = new Timer();
		}
		mBlueTimer.schedule(new TimerTask() {
			public void run() {
				msg = new Message();
				msg.what = AgingTestUtils.bluetoothTest;
				mHandler.sendMessage(msg);
			}
		}, 20000, 20000);
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

	private void destory(){
		if(mBlueTimer != null){
			mBlueTimer.cancel();
			mBlueTimer = null;
		}
		changeBlueOnoff(false);
	}

	Runnable blueRunnable = new Runnable() {
		@Override
		public void run() {
			init();
		}
	};

	private void init() {
		mAdapter.enable();
		if (mAdapter.isEnabled()) {
			msg = new Message();
			msg.what = 0;
			mHandler.sendMessage(msg);
		} else {
			mBlueHandler.postDelayed(blueRunnable, 3000);
		}
	}

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if(msg.what == AgingTestUtils.bluetoothTest){
				mBlueOn = !mBlueOn;
				changeBlueOnoff(mBlueOn);
				if(!mBlueOn){
					mTvInfo.setText(R.string.Bluetooth_close);
					mTvResult.setText("");
					mTvCon.setText("");
				}
			}else if(msg.what == 0){
				mTvInfo.setText(R.string.Bluetooth_open);
				mTvResult.setText(R.string.Bluetooth_scaning);
				mBlueHandler.removeCallbacks(blueRunnable);
				IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
				registerReceiver(mReceiver, filter);
				IntentFilter filter_finished = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
				registerReceiver(mReceiver, filter_finished);
				mBlueFlag = true;
				mAdapter.startDiscovery();
			}
		}
	};

	BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				mNameList += device.getName() + "--" + getString(R.string.Bluetooth_mac) + device.getAddress() + "\n";
				mTvResult.setText(mNameList);
			} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
				mTvCon.setText(R.string.Bluetooth_scan_success);
			}
		}
	};

	private void changeBlueOnoff(boolean mBlueOn){
		if(mBlueOn){
			mBlueFlag = false;
			mBlueHandler.post(blueRunnable);
		}else{
			mBlueHandler.removeCallbacks(blueRunnable);
			if(mBlueFlag){
				unregisterReceiver(mReceiver);
				mBlueFlag = false;
			}
			mAdapter.cancelDiscovery();
			mAdapter.disable();
		}
	}
}
