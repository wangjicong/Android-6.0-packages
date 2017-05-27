package com.android.agingtest;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.view.KeyEvent;
import android.widget.TextView;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.util.Log;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.os.SystemClock;
import android.view.WindowManager;

public class DoWifiTest extends Activity {
	final private static int TIMEOUT = 19;
	final private static int WIFI_STATE = 0;
	final private static int WIFI_LIST = 1;
	final private static int WIFI_CONNECTED = 2;
	final private static int WIFI_CONNECTING = 3;
	final private static int WIFI_FAILED = 4;
	final private static int WIFI_NOTFOUND_OPENAP = 5;
	final private static int WIFI_ONOFF = 6;

	private Timer mWifiTimer;
	private TextView mTvInfo,mTvResult,mTvCon,mTvResInfo;
	private HandlerThread mWifiThread = new HandlerThread("wifiThread");
	private WiFiTools mWifiTools;
	private Handler wifiHandler;
	private boolean mWifiOn = true;
	private boolean mFlag = false;
	private boolean mListFlag = false;
	private boolean mWifiScan = false;
	private Context mContext;
	private int mCount = 0;
	private String mNetWorkName = "";
	private static WifiManager mWifiManager;
	
	private static String TAG = "DoWifiTest";
	private WifiScanReceiver mWifiScanReceiver = null;
	List<ScanResult> mWifiList = null;
	private static final int DELAY_TIME = 15000;
	private StartScanThread mStartScanThread = null;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.wifi_test);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); //yanghua add
		mTvInfo = (TextView) findViewById(R.id.wifi_state_id);
		mTvResult = (TextView) findViewById(R.id.wifi_result_id);
		mTvCon = (TextView) findViewById(R.id.wifi_con_id);
		mTvResInfo = (TextView) findViewById(R.id.wifi_resinfo_id);
		((TextView) findViewById(R.id.wifi_interval)).setText(getString(R.string.test_interval, getString(R.string.wifi_name)));
		mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		mContext = this;
		mWifiTools = new WiFiTools();
		mWifiThread.start();
		wifiHandler = new Handler(mWifiThread.getLooper());
		mWifiTools.openWifi();
		wifiHandler.post(wifirunnable);

		if(mWifiTimer == null){
			mWifiTimer = new Timer();
		}
		mWifiTimer.schedule(new TimerTask() {
			public void run() {
				Message msg = new Message();
				msg.what = WIFI_ONOFF;
				uiHandler.sendMessage(msg);
			}
		}, 20000, 20000);
		
        mWifiScanReceiver = new WifiScanReceiver();
        String filterFlag2 = WifiManager.SCAN_RESULTS_AVAILABLE_ACTION;
        IntentFilter filter2 = new IntentFilter(filterFlag2);
        mContext.registerReceiver(mWifiScanReceiver, filter2);	
        
	}

	protected void onDestroy() {
		super.onDestroy();
		destory();
		
        // release wifi scan receiver
        if (mWifiScanReceiver != null) {
            mContext.unregisterReceiver(mWifiScanReceiver);
            mWifiScanReceiver = null;
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

	Handler uiHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case WIFI_STATE:
					mTvInfo.setText(mWifiTools.GetState());
					mTvResult.setText(R.string.WiFi_scaning);
					break;
				case WIFI_LIST:
					mTvResult.setText(mNetWorkName);
					break;
				case WIFI_CONNECTED:
					mTvResInfo.setText(mWifiManager.getConnectionInfo().toString());
					mTvCon.setText(R.string.WiFi_success);
					break;
				case WIFI_CONNECTING:
					mTvCon.setText(R.string.WiFi_connecting);
					break;
				case WIFI_FAILED:
					mTvCon.setText(R.string.WiFi_failed);
					break;
				case WIFI_NOTFOUND_OPENAP:
					mTvCon.setText(R.string.WiFi_notfound_openap);
					break;
				case WIFI_ONOFF:
					mWifiOn = !mWifiOn;
					changeWifiOnoff(mWifiOn);
					if(!mWifiOn){
						mTvInfo.setText(R.string.WiFi_info_close);
						mTvResult.setText("");
						mTvCon.setText("");
					}
					break;
				default:
					break;
			}
		}
	};

	private void destory(){
		if(mWifiTimer != null){
			mWifiTimer.cancel();
			mWifiTimer = null;
		}
		changeWifiOnoff(false);
	}

	Runnable wifirunnable = new Runnable() {
		@Override
		public void run() {
			if (mCount >= TIMEOUT) {
				uiHandler.sendEmptyMessage(WIFI_FAILED);
				changeWifiOnoff(false);
			}
			if (mFlag == false) {
				boolean res = StartWifi();
				if (!res && mListFlag) {
					uiHandler.sendEmptyMessage(WIFI_NOTFOUND_OPENAP);
					wifiHandler.removeCallbacks(this);
					return;
				} else if (res) {
					mFlag = true;
					uiHandler.sendEmptyMessage(WIFI_CONNECTING);
				}
				wifiHandler.postDelayed(this, 3000);
			} else {
				if (mWifiTools.IsConnection()) {
					uiHandler.sendEmptyMessage(WIFI_CONNECTED);
				} else {
					wifiHandler.postDelayed(this, 3000);
				}
			}
			mCount++;
		}
	};

	private void changeWifiOnoff(boolean mWifiOn){
		if(mWifiOn){
			mCount = 0;
			mWifiTools.openWifi();
			wifiHandler.post(wifirunnable);
		}else{
			mWifiTools.closeWifi();
			wifiHandler.removeCallbacks(wifirunnable);
		}
	}

	private boolean StartWifi() {
		//List<ScanResult> mWifiList = null;
		uiHandler.sendEmptyMessage(WIFI_STATE);
		//mWifiList = mWifiTools.scanWifi();
		mNetWorkName = "";
		if (mWifiList == null || mWifiList.size() <= 0) {
			return false;
		}
		if (mWifiList.size() > 0) {
			for (int i = 0; i < mWifiList.size(); i++) {
				ScanResult sr = mWifiList.get(i);
				mNetWorkName += sr.SSID + "\n";
			}
			uiHandler.sendEmptyMessage(WIFI_LIST);
			for (int j = 0; j < mWifiList.size(); j++) {
				ScanResult sr = mWifiList.get(j);
				if (sr.capabilities.equals("[WPS]") || sr.capabilities.equals("")) {
					if (mWifiTools.addWifiConfig(mWifiList, sr, "")) {
						return true;
					}
				}
			}
			mListFlag = true;
			return false;
		}
		return false;
	};

	public class WiFiTools {
		private static final int WIFI_STATE_DISABLING = 0;
		private static final int WIFI_STATE_DISABLED = 1;
		private static final int WIFI_STATE_ENABLING = 2;
		private static final int WIFI_STATE_ENABLED = 3;
		private String info = "";

		public WiFiTools() {
			mWifiManager.startScan();
		}

		public String GetState() {
			int state = mWifiManager.getWifiState();
			switch (state) {
				case WIFI_STATE_DISABLING:
					info = mContext.getString(R.string.WiFi_info_closeing);
					break;
				case WIFI_STATE_DISABLED:
					info = mContext.getString(R.string.WiFi_info_close);
					break;
				case WIFI_STATE_ENABLING:
					info = mContext.getString(R.string.WiFi_info_opening);
					break;
				case WIFI_STATE_ENABLED:
					info = mContext.getString(R.string.WiFi_info_open);
					break;
				default:
					info = mContext.getString(R.string.WiFi_info_unknown);
					break;
			}
			return info;
		}

		public boolean openWifi() {
			boolean wifistate = true;
			if (!mWifiManager.isWifiEnabled()) {
				wifistate = mWifiManager.setWifiEnabled(true);
			}
			
			//mWifiManager.startScan();
      if (mWifiManager != null) {
          mStartScanThread = new StartScanThread();
          mStartScanThread.start();
      }			
			
			return wifistate;
		}

		public void closeWifi() {
			if (mWifiManager.isWifiEnabled()) {
				mWifiManager.setWifiEnabled(false);
				mWifiManager.stopWifi();
			}
		}

		public boolean addWifiConfig(List<ScanResult> wifiList, ScanResult srt, String pwd) {
			WifiConfiguration wc = new WifiConfiguration();
			wc.SSID = "\"" + srt.SSID + "\"";
			wc.allowedKeyManagement.set(KeyMgmt.NONE);
			wc.status = WifiConfiguration.Status.ENABLED;
			wc.networkId = mWifiManager.addNetwork(wc);
			return mWifiManager.enableNetwork(wc.networkId, true);
		}

		public List<ScanResult> scanWifi() {
			return mWifiManager.getScanResults();
		}

		public Boolean IsConnection() {
			ConnectivityManager connec = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
			if (connec.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED) {
				return true;
			}
			return false;
		}
	}
	
    private class WifiScanReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                //List<ScanResult> wifiScanResultList = mWifiManager.getScanResults();
                
								mWifiList = mWifiManager.getScanResults();
								
								/*
                if (wifiScanResultList != null
                        && wifiScanResultList.size() != mLastCount) {
                    wifiDeviceListChange(wifiScanResultList);

                    mLastCount = wifiScanResultList.size();
                }
                */
            }
        }
    }

    class StartScanThread extends Thread {
        @Override
        public void run() {
            try {
                // wait until other actions finish.
                SystemClock.sleep(DELAY_TIME);
                mWifiManager.startScan();
            } catch (Exception e) {
                // do nothing
            }
        }
    }    	
}
