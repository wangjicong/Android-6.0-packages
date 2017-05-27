package com.example.timeshowactivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.content.ComponentName;
import android.os.PowerManager;
import android.os.IBinder;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.os.Handler;
import android.content.SharedPreferences;
import java.util.List;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;

//import android.telephony.PhoneStateListener;//hj
import android.os.SystemProperties;

public class TimeShowReceiver extends BroadcastReceiver{
    public static final String MTK_ACTION_UNREAD_CHANGED = "com.mediatek.action.UNREAD_CHANGED";
    public static final String MTK_EXTRA_UNREAD_COMPONENT = "com.mediatek.intent.extra.UNREAD_COMPONENT";
    public static final String MTK_EXTRA_UNREAD_NUMBER = "com.mediatek.intent.extra.UNREAD_NUMBER";
    private static long  mLastReceiveEventTime;
    private static final int DELAY_TIME = 1000;
    public static int mDelayFlag = 0;
    private Handler mHandler = new Handler();
    private Context mContext = null;
    
    public static final String TAG = "TimeShowReceiver";

    //private TelephonyManager mTelephonyManager = null; //hj
   // private static boolean  WhetherSendIntent = false;

    private Runnable mDelayTimeTask = new Runnable() {
				public void run() {
					//Log.i("huangjun00","mDelayTimeTask  flag=" +mDelayFlag);
					if(mDelayFlag == 0){
						MainActivity m = MainActivity.getInstance();	
						//Log.i("huangjun00","mDelayTimeTask mDelayFlag == 0  m=" +m);
						
						if(m != null){		    	
						  m.finish();
						//m.moveTaskToBack(true);
						//final Activity activity = getActivity();
						//getActivity().onBackPressed();
						//m.onBackPressed();
						//ActivityManager am = (ActivityManager)mContext.getSystemService(Context.ACTIVITY_SERVICE);
						//am.forceStopPackage("com.example.timeshowactivity");	  
						}
					}else if(mDelayFlag == 1){
				         //  mTelephonyManager = (TelephonyManager) (mContext.getSystemService(Context.TELEPHONY_SERVICE)); //hj
				         //  mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE); //hj
						ComponentName cn = new ComponentName("com.example.timeshowactivity","com.example.timeshowactivity.MainActivity");
						Intent intent = new Intent();
						intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						intent.setComponent(cn);
						mContext.startActivity(intent);
					}
				}
    };
    
    
    private  void setTimeShowState(boolean value){					
				SharedPreferences sp = mContext.getSharedPreferences("TIME_SHOW",Context.MODE_WORLD_WRITEABLE + Context.MODE_WORLD_READABLE);
				SharedPreferences.Editor ed = sp.edit();
				ed.putBoolean("IS_SHOW", value);
				ed.commit();
	  }

    private boolean isIdle(){
				boolean isIdle = false;
				TelephonyManager tm = (TelephonyManager) mContext.getSystemService("phone");
				if(tm != null){
				    isIdle = (tm.getCallState() == TelephonyManager.CALL_STATE_IDLE);
				}
				return isIdle;
	  }

		@Override
		public void onReceive(Context arg0, Intent arg1) {
			// TODO Auto-generated method stub
			String action = arg1.getAction();
			mContext = arg0;
			
			if(action.equals("com.example.timeshowactivity.OFF")){
			    mDelayFlag = 0;
			   //WhetherSendIntent =false;
			    setTimeShowState(false);
			    Log.i(TAG,"qiuyaobo onReceive com.example.timeshowactivity.OFF");
			}else if(action.equals("com.example.timeshowactivity.ON")){
			    mDelayFlag = 1;
				//WhetherSendIntent =true;
			    setTimeShowState(true);
			//Log.i("huangjun00","TimeShowReceiver ***onReceive00com.example.timeshowactivity.ON***");
			}else if(action.equals("android.intent.action.BOOT_COMPLETED")){
				Log.d("ljz", "set hall");
				//PowerManager pm =  (PowerManager) arg0.getSystemService(Context.POWER_SERVICE);
				//pm.setHallSensor(true, 0);
			//Log.i("huangjun00","TimeShowReceiver ***onReceive.....android.intent.action.BOOT_COMPLETED***");
				return;
		    }else if (MTK_ACTION_UNREAD_CHANGED.equals(action)){
			    //Log.i("huangjun00","TimeShowReceiver ***onReceive.....MTK_ACTION_UNREAD_CHANGED***");
							boolean   lidOpen =SystemProperties.getBoolean("persist.sys.lidopen",false);
							if(lidOpen){//hal is off ,at  the same time a call is arriving,open the hal ,at hal Call interface,hang up the call ,we should display  the TimeShow 
									mDelayFlag = 1;
							}else{
									mDelayFlag = 0;
							}
							if(!isIdle()){
								return;//when we opened  call waiting function ,we hanged up one call ,the other call has been activitied,at this time we cannot update TimeShow actovity if the TimeShow actovity on Background process!
							}
					            MainActivity m = MainActivity.getInstance();
					            if(m != null){
					            m.updatetInfo();
	            }
	        }
	
		    if(!isIdle()){
						Intent updateIncallTouchUi = new Intent("UPDATE_INCALLSCREEN_TOUCHUI");
						mContext.sendBroadcast(updateIncallTouchUi);
	
	       }
		    
		     long milliseconds = SystemClock.elapsedRealtime();
		     long timeSinceLastEvent = milliseconds - mLastReceiveEventTime;
		     mLastReceiveEventTime = milliseconds;
		     mHandler.removeCallbacks(mDelayTimeTask);
			
		    //Log.i("huangjun00","TimeShowReceiver ******** flag=" +mDelayFlag + "  timeSinceLastEvent="+timeSinceLastEvent);
		    if(timeSinceLastEvent < DELAY_TIME){
						long i;
						i= DELAY_TIME - timeSinceLastEvent;
		        //Log.i("huangjun00","TimeShowReceiver ***timeSinceLastEvent < DELAY_TIME..timeSinceLastEvent=" +timeSinceLastEvent + "; DELAY_TIME="+DELAY_TIME+";i="+i);
		        mHandler.postDelayed(mDelayTimeTask, DELAY_TIME - timeSinceLastEvent);
		        return;
		    }
	
				if(mDelayFlag == 0){
						MainActivity m = MainActivity.getInstance();
						Log.i(TAG," qiuyaobo onReceive mDelayFlag == 0   m=" +m);
			
						if(m != null){
						    m.finish();
							//m.moveTaskToBack(false);
			                      //    m.onBackPressed();
							//ActivityManager am = (ActivityManager)mContext.getSystemService(Context.ACTIVITY_SERVICE);
						//	am.forceStopPackage("com.example.timeshowactivity");
							
						}
				}else if(mDelayFlag == 1){
		
		              /// mTelephonyManager = (TelephonyManager) (mContext.getSystemService(Context.TELEPHONY_SERVICE));//hj
		               // mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE); //hj
				
					ComponentName cn = new ComponentName("com.example.timeshowactivity","com.example.timeshowactivity.MainActivity");
					Intent intent = new Intent();
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					intent.setComponent(cn);
					mContext.startActivity(intent);
				}
			
		}
	
    public boolean isDialerTopActivy(){
        ActivityManager manager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningTaskInfo> runningTaskInfos = manager.getRunningTasks(1);
        String cmpNameTemp = null;

        if(null != runningTaskInfos){
                cmpNameTemp=(runningTaskInfos.get(0).topActivity).toString();
               // Log.e("huangjun00","cmpname:"+cmpNameTemp);
        }

        if(null == cmpNameTemp){
            return false;
        }

        if (cmpNameTemp.contains("com.android.dialer")){
            return true;
        }
		
        return false;
    }


/***
//hj
    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {

	    //  String number  = incomingNumber;
            Log.i("huangjun00", "TimeShowReceiver [mPhoneStateListener]state = " + state + ",incomingNumber = "
                    + incomingNumber +";WhetherSendIntent="+WhetherSendIntent);
		 String  number = incomingNumber;
		//phone_state = state;      
		if(WhetherSendIntent)
		{
	            if (state == TelephonyManager.CALL_STATE_RINGING ||state == TelephonyManager.CALL_STATE_OFFHOOK ) {
					if(incomingNumber != null ){
			          // phoneNumber_textview.setText(incomingNumber);
			        Log.i("huangjun00", "TimeShowReceiver *****DISPLAY  CALL******* state = " + state + ",incomingNumber = "
			                    + incomingNumber);			             
					ComponentName cn = new ComponentName("com.example.timeshowactivity","com.example.timeshowactivity.MainActivity");
					Intent intent = new Intent();
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					intent.setComponent(cn);
					intent.putExtra("halcall_number", incomingNumber);
					mContext.startActivity(intent);
			             WhetherSendIntent  = false;
			            Log.i("huangjun00", "TimeShowReceiver *****DISPLAY  CALL******* mContext = " + mContext);							 
					}
	            }
	            else{
			        Log.i("huangjun00", "TimeShowReceiver *****DISPLAY TIME******* state = " + state + ",incomingNumber = "
			                    + incomingNumber);
					ComponentName cn = new ComponentName("com.example.timeshowactivity","com.example.timeshowactivity.MainActivity");
					Intent intent = new Intent();
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					intent.setComponent(cn);
					mContext.startActivity(intent);
			        WhetherSendIntent  = false;
			        Log.i("huangjun00", "TimeShowReceiver *****DISPLAY TIME******* mContext = " + mContext);					 
				}   
		}
        }
    };
***/

}
