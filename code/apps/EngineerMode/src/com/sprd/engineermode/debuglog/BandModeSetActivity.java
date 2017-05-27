package com.sprd.engineermode.debuglog;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.provider.Settings;
import android.os.ServiceManager;
import android.os.RemoteException;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.TelephonyManager.RadioFeatures;
import android.util.Log;
import com.android.internal.telephony.ITelephony;

import com.sprd.engineermode.R;
import com.sprd.engineermode.debuglog.FirBandModeSetActivity.FBHandler;
import com.sprd.engineermode.telephony.TelephonyFragment;

public class BandModeSetActivity extends PreferenceActivity implements
		Preference.OnPreferenceClickListener {

	private static final String TAG = "BandModeSetActivity";
	private static final int KEY_RADIO_POWER_OFF_DOWN = 1;
	private static final int KEY_RADIO_POWER_ON_DOWN = 2;
	private static final int KEY_RADIO_SET_POWER_DOWN = 3;
	private static final int KEY_SAVE_BAND = 4;
	private static final int KEY_RADIO_SET_TIMEOUT = 5;
	private PreferenceGroup mPreGroup = null;
	private ProgressDialog mProgressDlg;
	private BandSelector mBandSelector;
	private FBHandler mFBHandler;
	private int mPhoneID = -1;
	private int mSubID = -1;
	private int mWatingStatus;
	private Handler mUiThread = new Handler();
	private TelephonyManager mTelephonyManager;
	private PhoneStateListener mPhoneStateListener;

	class FBHandler extends Handler {
		public FBHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			Log.d(TAG, " handleMessage:" + msg.what);
			switch (msg.what) {
			case KEY_RADIO_SET_POWER_DOWN:
				boolean isRadio = isRadioOn(mSubID);
				Log.d(TAG, " KEY_RADIO_SET_POWER_DOWN hasIccCard():"
						+ mTelephonyManager.hasIccCard(mPhoneID)
						+ " isRadioOn():" + isRadio);
				if (mTelephonyManager.hasIccCard(mPhoneID) && mSubID >= 0
						&& isRadio) {
					mTelephonyManager.setRadioBusy(true);
					Log.d(TAG, " power -> down");
					mWatingStatus = KEY_RADIO_POWER_OFF_DOWN;
					showProgressDialog("Restart radio...");
					mTelephonyManager.setSimStandby(mPhoneID, false);
					mFBHandler.sendEmptyMessageDelayed(KEY_RADIO_SET_TIMEOUT,
							60 * 1000);
				}
				break;
			case KEY_RADIO_POWER_OFF_DOWN:
				Log.d(TAG, " power -> down ok! -> setSimStandby(true)");
				mTelephonyManager.setSimStandby(mPhoneID, true);
				mWatingStatus = KEY_RADIO_POWER_ON_DOWN;
				break;
			case KEY_RADIO_POWER_ON_DOWN:
				Log.d(TAG, " power -> on ok!");
				mBandSelector.loadBands();
				mTelephonyManager.setRadioBusy(false);
				dismissProgressDialog();
				mWatingStatus = KEY_RADIO_SET_POWER_DOWN;
				mFBHandler.removeMessages(KEY_RADIO_SET_TIMEOUT);
				break;
			case KEY_SAVE_BAND:
				showProgressDialog("Saving band");
				mWatingStatus = KEY_RADIO_SET_POWER_DOWN;
				mBandSelector.saveBand();
				dismissProgressDialog();
				AlertDialog alertDialog = new AlertDialog.Builder(
						BandModeSetActivity.this)
						.setTitle("Band Select")
						.setMessage(mBandSelector.getSetInfo())
						.setPositiveButton(R.string.alertdialog_ok,
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										// mFBHandler.sendEmptyMessage(KEY_RADIO_SET_POWER_DOWN);
									}
								}).create();
				alertDialog.show();
				break;
			case KEY_RADIO_SET_TIMEOUT:
				Log.d(TAG, " KEY_RADIO_SET_TIMEOUT");
				mFBHandler.sendEmptyMessage(KEY_RADIO_POWER_ON_DOWN);
				break;
			}
		}
	}

	private boolean isRadioOn(int subId) {
		ITelephony telephony = ITelephony.Stub.asInterface(ServiceManager
				.getService(Context.TELEPHONY_SERVICE));
		if (telephony != null) {
			try {
				telephony
						.isRadioOnForSubscriber(subId, "com.sprd.engineermode");
				return true;
			} catch (RemoteException e) {
				Log.e(TAG, "Error calling ITelephony#isRadioForSubscriber", e);
			}
		}
		return false;
	}

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		HandlerThread ht = new HandlerThread(TAG);
		ht.start();
		mFBHandler = new FBHandler(ht.getLooper());

		setPreferenceScreen(getPreferenceManager().createPreferenceScreen(this));
		mPreGroup = getPreferenceScreen();

		mPhoneID = getIntent().getIntExtra(TelephonyFragment.KEY_PHONEID, 0);
		int[] subId = SubscriptionManager.getSubId(mPhoneID);
		if (subId != null && subId.length > 0) {
			mSubID = subId[0];
		}

		mPhoneStateListener = new PhoneStateListener(mSubID) {
			@Override
			public void onServiceStateChanged(ServiceState serviceState) {
				Log.d(TAG,
						" onServiceStateChanged serviceState.getState():"
								+ serviceState.getState() + " mIsChanging:"
								+ mWatingStatus + " isSimStandby:"
								+ mTelephonyManager.isSimStandby(mPhoneID));
				if (mWatingStatus == KEY_RADIO_POWER_OFF_DOWN
						&& serviceState.getState() == ServiceState.STATE_POWER_OFF) {
					//if (!mTelephonyManager.isSimStandby(mPhoneID)) {
						mFBHandler.sendEmptyMessage(KEY_RADIO_POWER_OFF_DOWN);
					} else if (mWatingStatus == KEY_RADIO_POWER_ON_DOWN
							&& serviceState.getState() != ServiceState.STATE_POWER_OFF) {
						mFBHandler.sendEmptyMessage(KEY_RADIO_POWER_ON_DOWN);
					}
				}
		};
		mTelephonyManager = TelephonyManager.from(this);
		mTelephonyManager.listen(mPhoneStateListener,
				PhoneStateListener.LISTEN_SERVICE_STATE);
		Log.d(TAG, "onCreate mPhoneID:" + mPhoneID + " mSubID:" + mSubID);
		mBandSelector = new BandSelector(mPhoneID, this, mUiThread);
	}

	@Override
	protected void onStart() {
		mBandSelector.initModes(mPreGroup);
		mBandSelector.loadBands();
		super.onStart();
	}

	@Override
	protected void onDestroy() {
		mTelephonyManager.listen(mPhoneStateListener,
				PhoneStateListener.LISTEN_NONE);
		super.onDestroy();
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.frequency_set, menu);
		MenuItem item = menu.findItem(R.id.frequency_set);
		if (item != null) {
			item.setVisible(true);
		}
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.frequency_set: {
			if (!mBandSelector.isCheckOneOrMore()) {
				Toast.makeText(getApplicationContext(),
						"Please check at least one every mode!",
						Toast.LENGTH_SHORT).show();
			} else {
				mFBHandler.sendEmptyMessage(KEY_SAVE_BAND);
			}
		}
			break;
		default:
			Log.i(TAG, "default");
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void showProgressDialog(final String msg) {
		mUiThread.post(new Runnable() {
			public void run() {
				mProgressDlg = ProgressDialog.show(BandModeSetActivity.this,
						msg, "Please wait...", true, false);
			}
		});
	}

	private void dismissProgressDialog() {
		mUiThread.post(new Runnable() {
			@Override
			public void run() {
				if (mProgressDlg != null) {
					mProgressDlg.dismiss();
				}
			}
		});
	}

}