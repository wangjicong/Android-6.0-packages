package com.sprd.validationtools.itemstest;

import java.util.ArrayList;
import java.util.List;

import com.sprd.validationtools.BaseActivity;
import com.sprd.validationtools.R;

import android.os.Bundle;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class SIMCardTestActivity extends BaseActivity {

	private LinearLayout container;

	private TelephonyManager telMgr;
	private int mSimReadyCount = 0;
	private int phoneCount;
	public Handler mHandler = new Handler();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sim_card_test);
		setTitle(R.string.sim_card_test_tittle);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		container = (LinearLayout) findViewById(R.id.sim_test_result_container);
		setTitle(R.string.sim_card_test_tittle);
		phoneCount = TelephonyManager.from(SIMCardTestActivity.this).getPhoneCount();
		showDevice();
		int phoneCount = TelephonyManager.from(SIMCardTestActivity.this).getPhoneCount();
		if (phoneCount == mSimReadyCount) {
			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(SIMCardTestActivity.this,
							R.string.text_pass, Toast.LENGTH_SHORT).show();
					storeRusult(true);
					finish();
				}
			}, 1000);
		}else {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(SIMCardTestActivity.this, R.string.text_fail, Toast.LENGTH_SHORT).show();
                    storeRusult(false);
                    finish();
                }
            }, 1000);
        }
		super.removeButton();
	}

	private List<String> getResultList(int simId) {
		List<String> resultList = new ArrayList<String>();
		telMgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE + simId);
		if (telMgr == null) {
			telMgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
			if (telMgr == null) {
				return null;
			}
		}
		/* SPRD:436223 Sim test wrong in sigle sim in the phone@{*/
		if (telMgr.getSimState(simId) == TelephonyManager.SIM_STATE_READY) {
			resultList.add("fine");
			mSimReadyCount++;
		} else if (telMgr.getSimState(simId) == TelephonyManager.SIM_STATE_ABSENT) {
			resultList.add("no SIM card");
		} else {
			resultList.add("locked/unknow");
		}

		if (telMgr.getSimCountryIso(simId).equals("")) {
			resultList.add("can not get country");
		} else {
			resultList.add(telMgr.getSimCountryIso());
		}

		if (telMgr.getSimOperator(simId).equals("")) {
			resultList.add("can not get operator");
		} else {
			resultList.add(telMgr.getSimOperator());
		}

		if (telMgr.getSimOperatorName().equals("")) {
			resultList.add("can not get operator name");
		} else {
			resultList.add(telMgr.getSimOperatorName());
		}

		if (telMgr.getSimSerialNumber(simId) != null) {
			resultList.add(telMgr.getSimSerialNumber());
		} else {
			resultList.add("can not get serial number");
		}

		if (telMgr.getSubscriberId(simId) != null) {
			resultList.add(telMgr.getSubscriberId());
		} else {
			resultList.add("can not get subscriber id");
		}

		if (telMgr.getDeviceId(simId) != null) {
			resultList.add(telMgr.getDeviceId());
		} else {
			resultList.add("can not get device id");
		}

		if (telMgr.getLine1Number() != null) {
			resultList.add(telMgr.getLine1Number());
		} else {
			resultList.add("can not get phone number");
		}

		if (telMgr.getPhoneType(simId) == 0) {
			resultList.add("NONE");
		} else if (telMgr.getPhoneType(simId) == 1) {
			resultList.add("GSM");
		} else if (telMgr.getPhoneType(simId) == 2) {
			resultList.add("CDMA");
		} else if (telMgr.getPhoneType(simId) == 3) {
			resultList.add("SIP");
		}
		/* @}*/

		if (telMgr.getDataState() == 0) {
			resultList.add("disconnected");
		} else if (telMgr.getDataState() == 1) {
			resultList.add("connecting");
		} else if (telMgr.getDataState() == 2) {
			resultList.add("connected");
		} else if (telMgr.getDataState() == 3) {
			resultList.add("suspended");
		}

		if (telMgr.getDataActivity() == 0) {
			resultList.add("none");
		} else if (telMgr.getDataActivity() == 1) {
			resultList.add("in");
		} else if (telMgr.getDataActivity() == 2) {
			resultList.add("out");
		} else if (telMgr.getDataActivity() == 3) {
			resultList.add("in/out");
		} else if (telMgr.getDataActivity() == 4) {
			resultList.add("dormant");
		}

		if (!telMgr.getNetworkCountryIso().equals("")) {
			resultList.add(telMgr.getNetworkCountryIso());
		} else {
			resultList.add("can not get network country");
		}

		if (!telMgr.getNetworkOperator().equals("")) {
			resultList.add(telMgr.getNetworkOperator());
		} else {
			resultList.add("can not get network operator");
		}

		if (telMgr.getNetworkType() == 0) {
			resultList.add("unknow");
		} else if (telMgr.getNetworkType() == 1) {
			resultList.add("gprs");
		} else if (telMgr.getNetworkType() == 2) {
			resultList.add("edge");
		} else if (telMgr.getNetworkType() == 3) {
			resultList.add("umts");
		} else if (telMgr.getNetworkType() == 4) {
			resultList.add("hsdpa");
		} else if (telMgr.getNetworkType() == 5) {
			resultList.add("hsupa");
		} else if (telMgr.getNetworkType() == 6) {
			resultList.add("hspa");
		} else if (telMgr.getNetworkType() == 7) {
			resultList.add("cdma");
		} else if (telMgr.getNetworkType() == 8) {
			resultList.add("evdo 0");
		} else if (telMgr.getNetworkType() == 9) {
			resultList.add("evdo a");
		} else if (telMgr.getNetworkType() == 10) {
			resultList.add("evdo b");
		} else if (telMgr.getNetworkType() == 11) {
			resultList.add("1xrtt");
		} else if (telMgr.getNetworkType() == 12) {
			resultList.add("iden");
		} else if (telMgr.getNetworkType() == 13) {
			resultList.add("lte");
		} else if (telMgr.getNetworkType() == 14) {
			resultList.add("ehrpd");
		} else if (telMgr.getNetworkType() == 15) {
			resultList.add("hspap");
		}
		return resultList;
	}

	private List<String> getKeyList() {
		List<String> keyList = new ArrayList<String>();
		keyList.add("Sim State:  ");
		keyList.add("Sim Country:  ");
		keyList.add("Sim Operator:  ");
		keyList.add("Sim Operator Name:  ");
		keyList.add("Sim Serial Number:  ");
		keyList.add("Subscriber Id:  ");
		keyList.add("Device Id:  ");
		keyList.add("Line 1 Number:  ");
		keyList.add("Phone Type:  ");
		keyList.add("Data State:  ");
		keyList.add("Data Activity:  ");
		keyList.add("Network Country:  ");
		keyList.add("Network Operator:  ");
		keyList.add("Network Type:  ");
		return keyList;
	}

	private void showDevice() {

		List<String> keyList = getKeyList();
		List<String> resultList0 = null;

		for (int i = 0; i < phoneCount; i++) {
			TextView tv = new TextView(this);
			if (i != 0) {
				tv.append("\n\n");
			}
			tv.append("Sim" + (i + 1) + " "
					+ this.getResources().getString(R.string.sim_test_result)
					+ ":\n");
			resultList0 = getResultList(i);
			for (int j = 0; j < 14; j++) {
				if (resultList0 != null)
					tv.append(keyList.get(j) + resultList0.get(j) + "\n");
				else
					tv.append(keyList.get(j) + "\n");
			}
			container.addView(tv);
		}
	}

//    @Override
//    public void onBackPressed() {
//        if (TelephonyManager.getPhoneCount() == mSimReadyCount) {
//            storeRusult(true);
//        } else {
//            storeRusult(false);
//        }
//        finish();
//    }
}
