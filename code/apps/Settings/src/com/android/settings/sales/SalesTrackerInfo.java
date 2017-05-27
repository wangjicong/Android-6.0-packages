package com.android.settings.sales;

import android.content.Context;
import android.text.TextUtils;
import com.android.internal.telephony.ITelephony;
import android.os.ServiceManager;
import android.os.RemoteException;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;

import com.android.settings.R;
import com.sprd.android.config.OptConfig;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
//jxl add 20150108
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import java.util.List;
import android.os.SystemProperties;//ljb

public class SalesTrackerInfo {
    private static final String TAG = "SalesTrackerInfo";
	
	//show register window before send sms, set REGISTER_FUNCTION = false to close this function
	public static final boolean REGISTER_FUNCTION = false;
	public static final String Reg_string = "Registering your phone on Zen Server. Charges as per operator tariff plan for single SMS.";

	//show register success window after send sms, set REGISTER_SUCCESS_FUNCTION = false to close this function
	public static final boolean REGISTER_SUCCESS_FUNCTION = false;
	public static final String Reg_success_string = "Successfully registered on Spice Server";
	
	public static final String Number =OptConfig.SUNVOV_C7359_C5D_JIVI ? "5424218" : OptConfig.SUNVOV_CUSTOM_C7357_BP_ROX ? "+94778555111" :"00918588877955";
	public static final long minutes = OptConfig.SUNVOV_SUBCUSTOM_C7301_XLL_M5_FWVGA ? 120 : OptConfig.SUNVOV_C7359_C5D_JIVI ? 10 : OptConfig.SUNVOV_CUSTOM_C7357_BP_ROX ? 240 : 60;	//60 means 60 minutes
		
	private static boolean is_sim1_insert = false;
	private static boolean is_sim2_insert = false;
	private static final String  sales_model= SystemProperties.get("ro.product.model");//ljb

	/****
		Sms_Detail
	*/
	public static String Sms_Detail(Context context){
		String sms_detail = "";
		int solt_M = 0; // SIM 1
		int solt_S = 1; // SIM 2
		TelephonyManager mTelephonyManager;
		mTelephonyManager = (TelephonyManager) TelephonyManager.from(context);

		if (OptConfig.SUNVOV_CUSTOM_C7301_XLL_FWVGA){
			StringBuffer imeiBuffer = new StringBuffer();
			int sub_id0 = -1;
	    	int sub_id1 = -1;
			
			//IMEI1,IMEI2
			String IMEI1 = mTelephonyManager.getDeviceId(0);
			String IMEI2 = mTelephonyManager.getDeviceId(1);
			//String IMSI1 = mTelephonyManager.getSubscriberId(0);
			String IMSI1 = mTelephonyManager.getNetworkOperatorForPhone(0);
			String IMSI2 = mTelephonyManager.getNetworkOperatorForPhone(1);
			String ICCID1 = mTelephonyManager.getSimSerialNumber(0);
			String ICCID2 = mTelephonyManager.getSimSerialNumber(1);
			is_sim1_insert=mTelephonyManager.hasIccCard(0);       	
			is_sim2_insert=mTelephonyManager.hasIccCard(1);

			String salesPreMessage = context.getString(R.string.sales_pre_message);
			imeiBuffer.append(salesPreMessage);
			String imeiStr;
			
			//LAC,CELLID
			GsmCellLocation loc = (GsmCellLocation)mTelephonyManager.getCellLocation();
			int lac = -1;
			int cid = -1;
			if (loc != null)
			{
	            lac = loc.getLac();
	            cid = loc.getCid();
			}
			String LAC = ((lac == -1) ? "unknown" : Integer.toHexString(lac));
			String CELLID = ((cid == -1) ? "unknown" : Integer.toHexString(cid));
					
            SubscriptionManager subScriptionManager = new SubscriptionManager(context);
            List<SubscriptionInfo> subInfos = subScriptionManager.getActiveSubscriptionInfoList();

            if(null != subInfos){
        	    for (SubscriptionInfo subInfo : subInfos) {
                    int phoneId = subInfo.getSimSlotIndex();
                    int subId = subInfo.getSubscriptionId();
                    Log.d(TAG, "XLL: phoneId = " + phoneId + ", subId = " + subId);

                    if (mTelephonyManager.getSimState(phoneId) == TelephonyManager.SIM_STATE_READY && subId >= 0) {
						if(phoneId == 0){
							sub_id0 = subId;
						}else{
							sub_id1 = subId;
						}
                    }
        	    }
     	    }
     	    if(sub_id0 >=0){
     	        ICCID1 = mTelephonyManager.getSimSerialNumber(sub_id0);
     	    }
     	    if(sub_id1 >=0){
			    ICCID2 = mTelephonyManager.getSimSerialNumber(sub_id1);
			}
            if(is_sim1_insert){
                 imeiStr = " IMEI1:" + IMEI1 + " IMEI2:" + IMEI2 + " LACID:" + LAC + " CELLID:" + CELLID + " IMSI:" + IMSI1 + " " + " ICCID:" + ICCID1;
            }else if(is_sim2_insert){
                 imeiStr = " IMEI1:" + IMEI1 + " IMEI2:" + IMEI2 +" LACID:" + LAC + " CELLID:" + CELLID + "IMSI:" + IMSI2 + " " + "ICCID:"+ ICCID2;
            }else{
                 imeiStr = " IMEI1:" + IMEI1 + " IMEI2:" + IMEI2 +" LACID:" + LAC + " CELLID:" + CELLID + " IMSI:" + IMSI1 + " " + " ICCID:" + ICCID1;
            }
			imeiBuffer.append(imeiStr);

			sms_detail = imeiBuffer.toString();

			Log.d(TAG, "imeiStr:" + sms_detail);
			return sms_detail;
		}else{
			is_sim1_insert=mTelephonyManager.hasIccCard(0);       	
			is_sim2_insert=mTelephonyManager.hasIccCard(1);
			StringBuffer imeiBuffer = new StringBuffer();
			//IMEI 1
			imeiBuffer.append(sales_model);//ljb
			imeiBuffer.append(" ");
			if(is_sim1_insert){
				imeiBuffer.append(mTelephonyManager.getDeviceId(0));
			}else if(is_sim2_insert){
				imeiBuffer.append(mTelephonyManager.getDeviceId(1));
			}else{
				imeiBuffer.append(mTelephonyManager.getDeviceId(0));
			}
			
			String imeiStr = imeiBuffer.toString();
			if(OptConfig.SUNVOV_CUSTOM_C7357_BP_ROX){
			sms_detail = "VINKO C9\n"+imeiStr;
			}else{
				sms_detail = /*company+" "+*/imeiStr;
			}
			
			return sms_detail;
		}
	}
	
	//jxl add 20150107 only for S7316_XLL_S51
	public static String Sms_Detail_Sim2(Context context){
		String sms_detail = "";
		int solt_M = 0; // SIM 1
		int solt_S = 1; // SIM 2
		TelephonyManager mTelephonyManager;
		mTelephonyManager = (TelephonyManager) TelephonyManager.from(context);
		StringBuffer imeiBuffer = new StringBuffer();
		int sub_id0 = -1;
	    int sub_id1 = -1;
			
		//IMEI1,IMEI2
		String IMEI1 = mTelephonyManager.getDeviceId(0);
		String IMEI2 = mTelephonyManager.getDeviceId(1);
		String IMSI1 = mTelephonyManager.getNetworkOperatorForPhone(0);
		String IMSI2 = mTelephonyManager.getNetworkOperatorForPhone(1);
		String ICCID1 = mTelephonyManager.getSimSerialNumber(0);
		String ICCID2 = mTelephonyManager.getSimSerialNumber(1);
		is_sim1_insert=mTelephonyManager.hasIccCard(0);       	
		is_sim2_insert=mTelephonyManager.hasIccCard(1);

		String salesPreMessage = context.getString(R.string.sales_pre_message);
		imeiBuffer.append(salesPreMessage);
		String imeiStr;
		
		//LAC,CELLID
		GsmCellLocation loc = (GsmCellLocation)mTelephonyManager.getCellLocation();
		int lac = -1;
		int cid = -1;
		if (loc != null)
		{
		    lac = loc.getLac();
		    cid = loc.getCid();
		}
		String LAC = ((lac == -1) ? "unknown" : Integer.toHexString(lac));
		String CELLID = ((cid == -1) ? "unknown" : Integer.toHexString(cid));
				
					
        SubscriptionManager subScriptionManager = new SubscriptionManager(context);
        List<SubscriptionInfo> subInfos = subScriptionManager.getActiveSubscriptionInfoList();

        if(null != subInfos){
        	for (SubscriptionInfo subInfo : subInfos) {
                int phoneId = subInfo.getSimSlotIndex();
                int subId = subInfo.getSubscriptionId();
                Log.d(TAG, "XLL: phoneId = " + phoneId + ", subId = " + subId);

                if (mTelephonyManager.getSimState(phoneId) == TelephonyManager.SIM_STATE_READY && subId >= 0) {
					if(phoneId == 0){
						sub_id0 = subId;
					}else{
						sub_id1 = subId;
					}
                }
        	}
     	}
     	if(sub_id0 >=0){
     	    ICCID1 = mTelephonyManager.getSimSerialNumber(sub_id0);
     	}
     	if(sub_id1 >=0){
			 ICCID2 = mTelephonyManager.getSimSerialNumber(sub_id1);
		}
		if(is_sim2_insert){
			imeiStr = " IMEI1:" + IMEI1 + " IMEI2:" + IMEI2 +" LACID:" + LAC + " CELLID:" + CELLID + "IMSI:" + IMSI2 + " " + "ICCID:"+ ICCID2;
		}else{
			imeiStr = " IMEI1:" + IMEI1 + " IMEI2:" + IMEI2 +" LACID:" + LAC + " CELLID:" + CELLID + " IMSI:" + IMSI1 + " " + " ICCID:" + ICCID1;
		}
		imeiBuffer.append(imeiStr);
        sms_detail = imeiBuffer.toString();

		Log.d(TAG, "imeiStr2:" + sms_detail);
		return sms_detail;
	}
}
