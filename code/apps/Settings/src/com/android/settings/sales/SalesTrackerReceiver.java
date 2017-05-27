package com.android.settings.sales;


import java.util.ArrayList;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.provider.Settings;
import java.util.List;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.BufferedOutputStream;
import java.io.IOException;
import android.os.FileUtils;
import android.widget.Toast;
import android.os.SystemClock;
import android.util.Log;

import android.net.Uri;
import android.provider.Telephony.Sms;
import android.database.ContentObserver;
import android.os.Handler;


import com.android.internal.telephony.ITelephony;
import android.os.ServiceManager;
import android.os.RemoteException;
import android.telephony.TelephonyManager;

import com.android.settings.R;
import com.sprd.android.config.OptConfig;

public class SalesTrackerReceiver extends BroadcastReceiver {


    public static final String BOOT_COMPLETE = "android.intent.action.BOOT_COMPLETED";
    public static final String SEND_SMS = "com.android.sales.sendsms";
    public static final String SEND_SMS_SUCCESS = "com.android.sales.sendsuccess";
    public static final String SALES_TRACKER_TEST = "com.android.sales.trakcertest";
    public static final String SEND_TEST_SMS = "com.android.sales.sendtestsms";
    public static final String DELETE_TRACKER_FILE = "com.android.sales.deletefile";

    public static final String SEND_SIM2_SMS = "com.android.sales.sendsmsbysim2";
    public static final String SEND_TEST_SMS_SUCCESS = "com.android.sales.sendtestsmssuccess";
	
    public static final String SHOW_REGISTER = "com.android.sales.showregister";
    public static final String SHOW_REGISTER_TEST = "com.android.sales.showregistertest";
    
    
    private static final String TAG = "SalesTracker";
    private static File SALS_FLAG_FILE_D = new File("/data/sales_flag");
    private static File SALS_FLAG_FILE_P = new File("/productinfo/sales_flag");
    //private static File SALS_FLAG_FILE = new File("/data/app/sales_flag");
    private static int try_time = 0;
    private static int test_try_time = 0;

	private static boolean is_sim1_insert = false;
	private static boolean is_sim2_insert = false;
    private String MCCMNC = null;
	private String MCC = null;
	//jxl add 20150108
	private static boolean is_sim1_send_falid = false;

		
    private static Context mContext;
    private static final Uri SMS_QUERY_URI = Uri.parse("content://sms");
    private SentMMSObserver mSentMMSObserver;

    private class SentMMSObserver extends ContentObserver {
		
        public SentMMSObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange) {
		//DeleteSentSMS();
		Intent Delintent = new Intent("com.android.sales.deletesms");
		String smscontent = SalesTrackerInfo.Sms_Detail(mContext);
		Delintent.putExtra("msg_body", smscontent);
		Delintent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
		mContext.sendBroadcast(Delintent);

		UnRegisterObserver();
        }
    }

	private void RegisterObserver(){
        if (mSentMMSObserver == null) {
            mSentMMSObserver = new SentMMSObserver();
            mContext.getContentResolver().registerContentObserver(
                    SMS_QUERY_URI, true, mSentMMSObserver);
        }
	}

	private void UnRegisterObserver(){
		Log.d(TAG, "UnRegisterObserver");
        if (mSentMMSObserver != null) {
            mContext.getContentResolver().unregisterContentObserver(
                    mSentMMSObserver);
        }
         mSentMMSObserver = null;
	}

	public void deleteRegisterSms(){
		Log.d("lupei","lupei deleteRegisterSms");
		Intent Delintent = new Intent("com.android.sales.deletesms");
		String smscontent = SalesTrackerInfo.Sms_Detail(mContext);
		Delintent.putExtra("msg_body", smscontent);
		Delintent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
		mContext.sendBroadcast(Delintent);	
	}

//jiazhenl 0923
	private void removeProductinfo(File logo_choiceFile)
	{
		try {
			if(logo_choiceFile.isFile() && logo_choiceFile.exists())
			logo_choiceFile.delete();
		}
		catch (Exception e) {
			Log.e(TAG,"Exception Occured: Trying to removeProductinfo "
					+ e.toString());
		}
	}

	private void writeProductinfo(char logo_choice, File logo_choiceFile)
	{
		byte BufToWrite[] = new byte[1];

		BufToWrite[0] = (byte)logo_choice;

		try {
			if(!logo_choiceFile.exists())
			{
				logo_choiceFile.createNewFile();
			}
			FileUtils.setPermissions(logo_choiceFile, 0644, -1, -1); // -rw-r--r--
			
			try {
				FileOutputStream fos = new FileOutputStream(logo_choiceFile, false);
				BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(
				fos);
				bufferedOutputStream.write(BufToWrite, 0, BufToWrite.length);
				bufferedOutputStream.flush();
				bufferedOutputStream.close();
			} catch (Exception e) {
				Log.e(TAG,
						"Exception Occured: Trying to write logo_choice.file "
						+ e.toString());
			}
		}
		catch (IOException e) {
            e.printStackTrace();
        }
		catch(Exception e) {
            e.printStackTrace();
        }
	}

	private char readProductinfo(File fileName)
	{
		char BufToRead[] = new char[1];
		char value = 0;
 		
        try {
            InputStreamReader inputReader = null;
            if(!fileName.exists()) 
            {
				//
            }
            else
            {			
                try{
                    inputReader = new FileReader(fileName);
                    int numRead = inputReader.read(BufToRead);
                    if( numRead > 0) {
            			value =  BufToRead[0];//Integer.parseInt(String.valueOf(BufToRead));
                    }
                } catch (Exception e) {
					Log.e(TAG,
							"Exception Occured: Trying to read logo_choice.file "
							+ e.toString());
				}
                finally {
                    inputReader.close();
                }
            }
        }
		catch (IOException e) {
            e.printStackTrace();
        }
		catch(Exception e) {
            e.printStackTrace();
        }
		Log.d(TAG, "jiazhenl in readProductinfo <<<< value ="+ value);
		return value;
	}

	private boolean checkIsNeedToSend()
	{
		if (SALS_FLAG_FILE_D.exists() && !SALS_FLAG_FILE_P.exists()) {
			if('0' == readProductinfo(SALS_FLAG_FILE_D))
			{
				Log.d(TAG, "jiazhenl <<<< 2 ");							
				return true;
			}
			else{
				Log.d(TAG, "jiazhenl <<<< 3 ");
				return false;
			}
		}
		else
		{
			return false;
		}
	}
//jiazhenl 0923
	
	@Override
	public void onReceive(Context context, Intent intent) {
		String intentAction = intent.getAction();
		Log.d(TAG, "intent action :" + intentAction);
		
        if(!OptConfig.SUN_SALES_TRACKER){
		    return;
		}
		
		if(mContext == null)
		{
			mContext = context;
		}

	    int sub_id0 = -1;
	    int sub_id1 = -1;
	    TelephonyManager mTelephonyManager;
        mTelephonyManager = (TelephonyManager) TelephonyManager.from(context);
		int simcount = 0;
		is_sim1_insert=mTelephonyManager.hasIccCard(0);        
		is_sim2_insert=mTelephonyManager.hasIccCard(1);
		if(is_sim1_insert && is_sim2_insert)
		{
			simcount = 2;
		}
		else if(is_sim1_insert || is_sim2_insert)
		{
			simcount = 1;
		}
		
        SubscriptionManager subScriptionManager = new SubscriptionManager(context);
        List<SubscriptionInfo> subInfos = subScriptionManager.getActiveSubscriptionInfoList();
        if (subInfos == null) {
            //subInfos = new ArrayList<SubscriptionInfo>();
            return;
        }
        for (SubscriptionInfo subInfo : subInfos) {
            int phoneId = subInfo.getSimSlotIndex();
            int subId = subInfo.getSubscriptionId();
            Log.d(TAG, "jiazhenl: phoneId = " + phoneId + ", subId = " + subId);
            if(subId < 0) {
                return;
            }
            if (mTelephonyManager.getSimState(phoneId) == TelephonyManager.SIM_STATE_READY) {
				if(phoneId == 0)
				{
					sub_id0 = subId;
				}
				else
				{
					sub_id1 = subId;
				}
            }
        }
        Log.d(TAG, "jiazhenl: simcount = " + simcount + ", sub_id0 = " + sub_id0 + ", sub_id1 = " + sub_id1);
		if(BOOT_COMPLETE.equals(intentAction)){

			//jiazhenl 0923
			/*D0,P0==> need to send
			  D1,P1==> no need to send
			  All:Product download should send.Factory data reset should not send.
			  1.Upgrade download send,ro.salestracker.type=1,default
			  D-1,P0;D0,P1==> restore to D0,P0
			  D-1,P1;D1,P0==> restore to D1,P1

			  2.Upgrade download not send,ro.salestracker.type=2,define by yourself
			  D-1,P0==> restore to D0,P0
			  D-1,P1;D1,P0;D0,P1==> restore to D1,P1
			*/
			if (!SALS_FLAG_FILE_D.exists()) {
				if(!SALS_FLAG_FILE_P.exists())
				{
					writeProductinfo('0', SALS_FLAG_FILE_D);
					Log.d(TAG, "jiazhenl <<<< 00 ");
				}
				else if(SALS_FLAG_FILE_P.exists())
				{
					writeProductinfo('1', SALS_FLAG_FILE_D);
					Log.d(TAG, "jiazhenl <<<< 01 ");
				}
			}else if (SALS_FLAG_FILE_D.exists()) {
				if(SALS_FLAG_FILE_P.exists()){
					if('0' == readProductinfo(SALS_FLAG_FILE_D)){
						if(false){//OptConfig.SUNVOV_SALESTRACKER_TYPE_2){
							writeProductinfo('1', SALS_FLAG_FILE_D);
							Log.d(TAG, "jiazhenl <<<< 10 ");
						}else{
							removeProductinfo(SALS_FLAG_FILE_P);
							Log.d(TAG, "jiazhenl <<<< 10 ");
						}
					}
				}else if(!SALS_FLAG_FILE_P.exists() && ('1' == readProductinfo(SALS_FLAG_FILE_D))){
					writeProductinfo('1', SALS_FLAG_FILE_P);
					Log.d(TAG, "jiazhenl <<<< 11 ");
				}
			}
			//jiazhenl 0923
			
			if (false == checkIsNeedToSend()) {
				Log.d(TAG, "Sales tracker sms already send");
		    }else{
				long time = SystemClock.elapsedRealtime() + SalesTrackerInfo.minutes *60 * 1000;  //send time
				if(SalesTrackerInfo.REGISTER_FUNCTION)
				{
					AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
					Intent send_intent = new Intent(SHOW_REGISTER);
					send_intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
					PendingIntent sender = PendingIntent.getBroadcast(
					context, 0, send_intent, 0);
					am.cancel(sender);
					am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, time, sender);
					return;
				}
				SalesTrackerStartSmsTimer(context, time, AlarmManager.ELAPSED_REALTIME_WAKEUP);
				Log.d(TAG, "Sales tracker sms will send after:" + time);
			}
			return;
		}
        
		if (SEND_SMS_SUCCESS.equals(intentAction)) {
			Log.d("jiazhenl","SEND_SMS_SUCCESS getResultCode() = "+getResultCode());
			deleteRegisterSms();
			if(getResultCode()==Activity.RESULT_OK)
			{
				//Write send flag
				writeProductinfo('1', SALS_FLAG_FILE_D);
				writeProductinfo('1', SALS_FLAG_FILE_P);
				Log.d(TAG, "jiazhenl <<<< 4 ");
				if(SalesTrackerInfo.REGISTER_SUCCESS_FUNCTION)
				{
					Intent reg_intent = new Intent(context, SalesTrackerRegisterActivity.class);
					reg_intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					reg_intent.putExtra("is_after", true);
					context.startActivity(reg_intent);
				}
			}
			else
			{
				if(try_time == 0)// try to use SIM2 send
				{
					try_time++;
					long time = 30 * 1000;	// 30 seconds
					is_sim1_send_falid = true;
					SalesTrackerStartSIM2SmsTimer(context,time);
				}
				else if(try_time == 1) // try to use SIM1 send again
				{
					try_time++;
					long time = System.currentTimeMillis() + 60 * 1000;	// 2 minutes
					is_sim1_send_falid = false;
					SalesTrackerStartSmsTimer(context, time, AlarmManager.RTC_WAKEUP);
				}
				else if(try_time == 2)	// try to use SIM2 send again
				{
					try_time++;
					long time = 60 * 1000;	// 2 minutes
					is_sim1_send_falid = true;
					SalesTrackerStartSIM2SmsTimer(context,time);
				}
				else if(try_time == 3)
				{
						is_sim1_send_falid = false;
				        Log.d(TAG, "try 3 times but send failed");
				}
		        Log.d(TAG, "sent not success");
			}
			return;
		}
		
		if (SEND_TEST_SMS_SUCCESS.equals(intentAction)) {
			Log.d("jiazhenl","SEND_TEST_SMS_SUCCESS getResultCode() = "+getResultCode());
			deleteRegisterSms();
			if(getResultCode()==Activity.RESULT_OK)
			{
			        Log.d(TAG, "send test sms success");
					test_try_time = 0;
					if(SalesTrackerInfo.REGISTER_SUCCESS_FUNCTION)
					{
						Intent reg_intent = new Intent(context, SalesTrackerRegisterActivity.class);
						reg_intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						reg_intent.putExtra("is_after", true);
						context.startActivity(reg_intent);
					}
			}else{	// try to use SIM2 send test sms							
				String num = intent.getStringExtra("test_num");
				boolean stop_test = intent.getBooleanExtra("test_stop", false);
				if(stop_test = false || test_try_time > 20)
				{
					return;
				}
				String text = SalesTrackerInfo.Sms_Detail(context);
				int subid = -1; //means SIM2
				if(simcount == 2)
				{
					if(test_try_time%2 == 1)
					{
						subid = sub_id0;
					}
					else
					{
						subid = sub_id1;
					}
				}
				else if(simcount == 1)
				{
					if(sub_id0 != -1){
						subid = sub_id0;
					}
					if(sub_id1 != -1){
						subid = sub_id1;
					}
				}
				else
				{
					return;
				}
				test_try_time++;
				Intent success = new Intent(SEND_TEST_SMS_SUCCESS);
				success.putExtra("test_stop", true);
				success.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
				int count;
				//SmsManager sms = SmsManager.getDefault();  
				//ArrayList<String> texts = sms.divideMessage(text);  				
				SmsManager sms = SmsManager.getSmsManagerForSubscriptionId(subid);
				ArrayList<String> texts;
				if (true//OperatorUtils.OPEN_MARKET
					&& Settings.System.getInt(mContext.getContentResolver(),
					"SMS_ENCODE_TYPE", 1) == 3) {
					texts = divideMessageFor16Bit(text);
				} else {
					texts = sms.divideMessage(text);
				}
				count = texts.size();
				ArrayList<PendingIntent> sentIntents = new ArrayList<PendingIntent>(count);
				for (int i = 0; i < count; i++)   
				{  
					sentIntents.add(PendingIntent.getBroadcast(context, 0, success, 0));  
				} 
				try  
				{ 
                   			sms.sendMultipartTextMessage(num, null, texts, sentIntents,null);
				} catch(Exception ex)  
				{  
					Log.d(TAG,"Error in Send test SMS by SIM2"+ex.getMessage());  
				}  			
			}
			return;
		}
		
		if (SEND_SMS.equals(intentAction)) {
			is_sim1_insert=mTelephonyManager.hasIccCard(0);
			is_sim2_insert=mTelephonyManager.hasIccCard(1);
			if(is_sim1_insert){
				MCCMNC = mTelephonyManager.getSimOperator((int)sub_id0);
			}else if(is_sim2_insert){
				MCCMNC = mTelephonyManager.getSimOperator((int)sub_id1);
			}else{
			  return;	
			}
			if(MCCMNC.length() > 3)
			{
				MCC = MCCMNC.substring(0,3);
				if((MCC!= null) && !(MCC.equals("404") || MCC.equals("405") || MCC.equals("406"))){
				//return ;
				}
			}
			String num = SalesTrackerInfo.Number;
			String text = SalesTrackerInfo.Sms_Detail(context);
			RegisterObserver();

		        Log.d(TAG, "sms detail: " +text );  

			Intent success = new Intent(SEND_SMS_SUCCESS);
			success.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
			int count1;
			//SmsManager sms = SmsManager.getDefault();  

			//ArrayList<String> texts = sms.divideMessage(text);  
				int subid = -1;
				if(simcount == 2)
				{
					subid = sub_id0;
				}
				else if(simcount == 1)
				{
					if(sub_id0 != -1){
						subid = sub_id0;
					}
					if(sub_id1 != -1){
						subid = sub_id1;
					}
				}
				else
				{
					return;
				}				
				SmsManager sms = SmsManager.getSmsManagerForSubscriptionId(subid);
				ArrayList<String> texts;
				if (true//OperatorUtils.OPEN_MARKET
					&& Settings.System.getInt(mContext.getContentResolver(),
					"SMS_ENCODE_TYPE", 1) == 3) {
					texts = divideMessageFor16Bit(text);
				} else {
					texts = sms.divideMessage(text);
				}
			
	        	count1 = texts.size();
	        	ArrayList<PendingIntent> sentIntents = new ArrayList<PendingIntent>(count1);
	        	for (int i = 0; i < count1; i++)   
	        	{  
	        		sentIntents.add(PendingIntent.getBroadcast(context, 0, success, 0));  
	        	}  
			try  
			{  
				sms.sendMultipartTextMessage(num,null,texts,sentIntents,null);
		        }  
		        catch(Exception ex)  
		        {  
		        	Log.d(TAG,"Error in SendingSms"+ex.getMessage());  
		        }  
			return;
		}

		if (SEND_SIM2_SMS.equals(intentAction)) {
			is_sim1_insert=mTelephonyManager.hasIccCard(0);
			is_sim2_insert=mTelephonyManager.hasIccCard(1);
			if(is_sim1_insert){
				MCCMNC = mTelephonyManager.getSimOperator((int)sub_id0);
			}else if(is_sim2_insert){
				MCCMNC = mTelephonyManager.getSimOperator((int)sub_id1);
			}else{
			  return;
			}
			if(MCCMNC.length() > 3)
			{
				MCC = MCCMNC.substring(0,3);
				if((MCC!= null) && !(MCC.equals("404") || MCC.equals("405") || MCC.equals("406"))){
					//return ;
				}
			}
			Log.d(TAG, "try to use SIM2 to send sms");
			int subid = -1;
			if(simcount == 2)
			{
				subid = sub_id1;
			}
			else if(simcount == 1)
			{
				if(sub_id0 != -1){
					subid = sub_id0;
				}
				if(sub_id1 != -1){
					subid = sub_id1;
				}
			}
			else
			{
				return;
			}
			String num = SalesTrackerInfo.Number;
			String text = SalesTrackerInfo.Sms_Detail(context);
			
			RegisterObserver();

			Log.d(TAG, "sms detail by sim2: " +text );  

			Intent success = new Intent(SEND_SMS_SUCCESS);
			success.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
			int count1;
			//SmsManager sms = SmsManager.getDefault();
			//ArrayList<String> texts = sms.divideMessage(text);
			SmsManager sms = SmsManager.getSmsManagerForSubscriptionId(subid);
			ArrayList<String> texts;
			if (true//OperatorUtils.OPEN_MARKET
				&& Settings.System.getInt(mContext.getContentResolver(),
				"SMS_ENCODE_TYPE", 1) == 3) {
				texts = divideMessageFor16Bit(text);
			} else {
				texts = sms.divideMessage(text);
			}
			
			count1 = texts.size();
			ArrayList<PendingIntent> sentIntents = new ArrayList<PendingIntent>(count1);
			for (int i = 0; i < count1; i++)   
			{  
				sentIntents.add(PendingIntent.getBroadcast(context, 0, success, 0));  
			}  
			try  
			{  
                   			sms.sendMultipartTextMessage(num, null, texts, sentIntents,null);
			}  
			catch(Exception ex)  
			{  
				Log.d(TAG,"Error in SendingSms by SIM2"+ex.getMessage());  
			}  	
			return;
		}		
		if(SALES_TRACKER_TEST.equals(intentAction)){
			long time = 30*1000;
			String num = intent.getStringExtra("test_num");
			Log.e(TAG,"num=" + num);
			AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
			if(SalesTrackerInfo.REGISTER_FUNCTION)
			{
				Intent send_intent = new Intent(SHOW_REGISTER_TEST);
				send_intent.putExtra("test_num", num);
				send_intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
				PendingIntent sender = PendingIntent.getBroadcast(
						context, 0, send_intent, 0);
				
				am.cancel(sender);
				am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+time, sender);	
			}
			else
			{
				Intent send_intent = new Intent(SEND_TEST_SMS);
				send_intent.putExtra("test_num", num);
				send_intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
				PendingIntent sender = PendingIntent.getBroadcast(
						context, 0, send_intent, 0);
				
				am.cancel(sender);
				am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+time, sender);				
			}
			return;
		}
		if(SEND_TEST_SMS.equals(intentAction)){
			is_sim1_insert=mTelephonyManager.hasIccCard(0);
			is_sim2_insert=mTelephonyManager.hasIccCard(1);
			if(is_sim1_insert){
				MCCMNC = mTelephonyManager.getSimOperator((int)sub_id0);
			}else if(is_sim2_insert){
				MCCMNC = mTelephonyManager.getSimOperator((int)sub_id1);
			}else{
			  return;	
			}
			Log.e(TAG,"ljz MCCMNC=" + MCCMNC);
			if(MCCMNC.length() > 3)
			{
				MCC = MCCMNC.substring(0,3);
				if((MCC!= null) && !(MCC.equals("404") || MCC.equals("405") || MCC.equals("406"))){
					//return ;
				}
			}
			String num = intent.getStringExtra("test_num");
			String text = SalesTrackerInfo.Sms_Detail(context);
			RegisterObserver();

			Log.d(TAG, "sms detail: " +text );  
			
			Intent success = new Intent(SEND_TEST_SMS_SUCCESS);
			success.putExtra("test_num", num);
			success.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
			int count1;

			//SmsManager sms = SmsManager.getDefault();  
			//ArrayList<String> texts = sms.divideMessage(text);  
			int subid = -1;
			if(simcount == 2)
			{
				subid = sub_id0;
			}
			else if(simcount == 1)
			{
				if(sub_id0 != -1){
					subid = sub_id0;
				}
				if(sub_id1 != -1){
					subid = sub_id1;
				}
			}
			else
			{
				return;
			}
			Log.e(TAG,"jiazhenl in SEND_TEST_SMS subid" + subid);
			SmsManager sms = SmsManager.getSmsManagerForSubscriptionId(subid);
			ArrayList<String> texts;
			if (true//OperatorUtils.OPEN_MARKET
				&& Settings.System.getInt(mContext.getContentResolver(),
				"SMS_ENCODE_TYPE", 1) == 3) {
				texts = divideMessageFor16Bit(text);
			} else {
				texts = sms.divideMessage(text);
			}
			count1 = texts.size();
			ArrayList<PendingIntent> sentIntents = new ArrayList<PendingIntent>(count1);
			for (int i = 0; i < count1; i++)   
			{  
				sentIntents.add(PendingIntent.getBroadcast(context, 0, success, 0));  
			}  
			try  
			{  
				sms.sendMultipartTextMessage(num,null,texts,sentIntents,null);
			} catch(Exception ex)  
		        {  
		        	Log.d(TAG,"Error in SendingSms"+ex.getMessage());  
		        }  
			return;
		}
		if(DELETE_TRACKER_FILE.equals(intentAction)){
			try {
				Log.d(TAG, "jiazhenl <<<< DELETE_TRACKER_FILE intent ");			
				writeProductinfo('0', SALS_FLAG_FILE_D);
				removeProductinfo(SALS_FLAG_FILE_P);
				Toast.makeText(context, R.string.delete_success, Toast.LENGTH_SHORT).show();
			} catch (Exception e) {
					Toast.makeText(context, R.string.delete_failed, Toast.LENGTH_SHORT).show();
		            } 
			return;
		}
		
		if(SalesTrackerInfo.REGISTER_FUNCTION)
		{
			if(SHOW_REGISTER.equals(intentAction)){
				is_sim1_insert=mTelephonyManager.hasIccCard(0);
				is_sim2_insert=mTelephonyManager.hasIccCard(1);
				if(is_sim1_insert){
					MCCMNC = mTelephonyManager.getSimOperator((int)sub_id0);
				}else if(is_sim2_insert){
					MCCMNC = mTelephonyManager.getSimOperator((int)sub_id1);
				}else{
				  return;	
				}
				if(MCCMNC.length() > 3)
				{
					MCC = MCCMNC.substring(0,3);
					if((MCC!= null) && !(MCC.equals("404") || MCC.equals("405") || MCC.equals("406"))){				
						//return ;
					}
				}
				Intent reg_intent = new Intent(context, SalesTrackerRegisterActivity.class);
				reg_intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				reg_intent.putExtra("is_before", true);
				context.startActivity(reg_intent);
				return;
			}
			else if(SHOW_REGISTER_TEST.equals(intentAction))
			{
				String num = intent.getStringExtra("test_num");
				
				Intent reg_intent = new Intent(context, SalesTrackerRegisterActivity.class);
				reg_intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				reg_intent.putExtra("is_test", true);
				reg_intent.putExtra("test_num", num);
				context.startActivity(reg_intent);
			}
			
		}
	}

	private void SalesTrackerStartSmsTimer(Context context, long second, int type){
		AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		Intent send_intent = new Intent(SEND_SMS);
		send_intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
		PendingIntent sender = PendingIntent.getBroadcast(
		context, 0, send_intent, 0);
		
		am.cancel(sender);
		am.set(type, second, sender);
	}

	private void SalesTrackerStartSIM2SmsTimer(Context context, long second){
		AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		Intent send_intent = new Intent(SEND_SIM2_SMS);
		send_intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
		PendingIntent sender = PendingIntent.getBroadcast(
		context, 0, send_intent, 0);
		
		am.cancel(sender);
		am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+second, sender);
	}

	    /*
     * SPRD:Bug#281864,Support sms coding 7bit/16bit.
     * 
     * @{
     */
    private ArrayList<String> divideMessageFor16Bit(String text) {
        int msgCount;
        int limit;
        int count = text.length() * 2;
        if (count > SmsMessage.MAX_USER_DATA_BYTES) {
            msgCount = (count + (SmsMessage.MAX_USER_DATA_BYTES_WITH_HEADER - 1))
                    / SmsMessage.MAX_USER_DATA_BYTES_WITH_HEADER;
        } else {
            msgCount = 1;
        }
        if (msgCount > 1) {
            limit = SmsMessage.MAX_USER_DATA_BYTES_WITH_HEADER;
        } else {
            limit = SmsMessage.MAX_USER_DATA_BYTES;
        }
        ArrayList<String> result = new ArrayList<String>(msgCount);
        int pos = 0; // Index in code units.
        int textLen = text.length();
        while (pos < textLen) {
            int nextPos = 0; // Counts code units.

            nextPos = pos + Math.min(limit / 2, textLen - pos);

            if ((nextPos <= pos) || (nextPos > textLen)) {
                Log.e(TAG, "fragmentText failed (" + pos + " >= " + nextPos
                        + " or " + nextPos + " >= " + textLen + ")");
                break;
            }
            result.add(text.substring(pos, nextPos));
            pos = nextPos;
        }
        return result;
    }
    /*
     * @}
     */
}
