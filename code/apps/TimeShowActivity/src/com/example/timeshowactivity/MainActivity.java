package com.example.timeshowactivity;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.app.Activity;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;  
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.content.SharedPreferences;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.LinearLayout;
//import com.mediatek.common.featureoption.FeatureOption;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.view.animation.LinearInterpolator;

import android.telephony.PhoneStateListener;

import android.view.View.DragShadowBuilder;
import android.content.ClipData;
import android.content.ClipDescription;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View.OnDragListener;
import android.view.View.OnTouchListener;
import java.lang.reflect.Method;
import com.android.internal.telephony.ITelephony;
import android.os.SystemProperties;
import android.view.Gravity;
import android.widget.RelativeLayout;

//hj
//import android.telecom.TelecomManager;
//import android.provider.ContactsContract;
//import android.provider.ContactsContract.CommonDataKinds.Email;
//import android.provider.ContactsContract.CommonDataKinds.Phone;
//import android.provider.ContactsContract.Contacts;
//import android.provider.ContactsContract.Data;
//import android.provider.ContactsContract.Directory;
//import android.provider.ContactsContract.SearchSnippets;
//hj

public class MainActivity extends Activity {
    private ImageView mHourH;
    private ImageView mHourL;
    private ImageView mMinH;
    private ImageView mMinL;
    private ImageView mTimeFormate;

    private LinearLayout timeLayout;
    private ImageView mmiss_call_imageview;
    private ImageView munread_message_imageview;	
    private ImageView mhour_min_sepretor;

    /**
    private ImageView mDateFirst;
    private ImageView mDateSecond;
    private ImageView mDateThird;
    private ImageView mDateForth;
    private ImageView mWeek;
    **/
	  private static MainActivity m = null;
	  private KeyguardLock mKeyguardLock ;
	
    private View mDigitalClock, mClockFrame;
    private AnalogClock mAnalogClock;
    private String mDateFormat;
    private ImageView mImgSecondHand = null;
    private TextView miss_call_textview, unread_message_textview;	
    private int unread_message_num = 0, miss_call_num = 0;

    //private  static String number =null;
    //private  static String hal_number =null; 
    //private  static String hal_name =null;
    //private static  int  phone_state = 0;
    //private RelativeLayout answer_new_panel;
    //private ImageView drap_handle,decline,answer;
    //private static final int DECLINE_X=120;
    //private static final int ANSWER_X=370;
    //private TextView phoneName_textview,phoneNumber_textview;
    //private TelephonyManager mTelephonyManager = null;

		public static MainActivity getInstance(){
				return m;
		}	
		
		private BroadcastReceiver mUpdateTimeReceiver = new BroadcastReceiver(){
	
			@Override
			public void onReceive(Context context, Intent intent) {
					/***
		        if (FeatureOption.SUNVOV_HALL_LEATHER_ANALOG_CLOCK) {
		            if (FeatureOption.SUNVOV_HALL_LEATHER_HUAWEI_ANALOG_CLOCK) {
				            updateTimeAndDate();
		            } else {
				            updateAnalogClockDate(mDateFormat, mClockFrame);
		            }
		        } else {
		        **/
				        updateTimeAndDate();
		        //}	
			}	
	  };

		@Override
		protected void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			m = this;
      LayoutInflater inflater =
              (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

      mClockFrame = inflater.inflate(R.layout.activity_main, null);
      setContentView(mClockFrame);
      mDigitalClock = mClockFrame.findViewById(R.id.digital_clock);
      LinearLayout dateLayout = (LinearLayout)mDigitalClock.findViewById(R.id.date_setting);

      timeLayout = (LinearLayout)mDigitalClock.findViewById(R.id.time_setting);
	
      /**
      if (FeatureOption.SUNVOV_HALL_LEATHER_HUAWEI_ANALOG_CLOCK) {
      		dateLayout.setVisibility(View.GONE);
      }
      **/
      mAnalogClock = (AnalogClock)mClockFrame.findViewById(R.id.analog_clock);
      mImgSecondHand = (ImageView)mClockFrame.findViewById(R.id.second_hand);
			//setContentView(R.layout.activity_main);
			miss_call_textview = (TextView) findViewById(R.id.miss_call_textview);
			unread_message_textview = (TextView) findViewById(R.id.unread_message_textview);
	
			mHourH = (ImageView) findViewById(R.id.hour_high);
			mHourL = (ImageView) findViewById(R.id.hour_low);
			mMinH = (ImageView) findViewById(R.id.min_high);
			mMinL = (ImageView) findViewById(R.id.min_low);
			mTimeFormate = (ImageView) findViewById(R.id.time_formate);
			//mDateFirst = (ImageView) findViewById(R.id.date_first);
			//mDateSecond = (ImageView) findViewById(R.id.date_second);
			//mDateThird = (ImageView) findViewById(R.id.date_third);
			//mDateForth = (ImageView) findViewById(R.id.date_forth);
			//mWeek = (ImageView) findViewById(R.id.week);	
	
			//mMinL = (ImageView) findViewById(R.id.min_low);
			//mTimeFormate = (ImageView) findViewById(R.id.time_formate);
	
			mmiss_call_imageview = (ImageView) findViewById(R.id.miss_call_imageview);
			munread_message_imageview = (ImageView) findViewById(R.id.unread_message_imageview);
			mhour_min_sepretor = (ImageView) findViewById(R.id.hour_min_sepretor);
	
			//phoneName_textview = (TextView) findViewById(R.id.phoneName);
			//phoneNumber_textview = (TextView) findViewById(R.id.phoneNumber);
	        //answer_new_panel=(RelativeLayout) findViewById(R.id.answer_new_panel);
	        //decline=(ImageView) findViewById(R.id.decline);
	        //answer=(ImageView) findViewById(R.id.answer);	
	        //drap_handle=(ImageView) findViewById(R.id.drap_handle);
	        //drap_handle.setOnTouchListener(drap_handle_ontouchlistener);
	        //mPhoto.setOnTouchListener(photo_ontouchlistener);
			 
			int flags = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
			getWindow().addFlags(flags);
			
		//	KeyguardManager keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
		//	mKeyguardLock = keyguardManager.newKeyguardLock("TimeShowActivity");
		//	mKeyguardLock.disableKeyguard();
			
		//	setTimeShowState(true);  
		//	Intent intent = new Intent(this, TimeShowService.class);
	 //		startService(intent);
	
	       //Log.i("huangjun00","MainActivity  onCreate()  m =" +m);
	/*	
	  //      mTelephonyManager = (TelephonyManager) (getSystemService(Context.TELEPHONY_SERVICE));
	 //       mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
			Intent intent = getIntent();
			if(intent != null){
				hal_number = (String) intent.getExtra("halcall_number");
				hal_name = (String) intent.getExtra("halcall_name");
			}
			//hal_name  = getContactNameByPhoneNumber(this,hal_number);//"182 2192 6886"
			Log.i("huangjun00","MainActivity  onCreate hal_name =" +hal_name + ";hal_number="+hal_number);
			//TelephonyManager tm = (TelephonyManager) mContext.getSystemService("phone");
			TelephonyManager tm = (TelephonyManager) this.getSystemService("phone");
			Log.i("huangjun00","MainActivity  onCreate tm =" +tm);
			if(tm != null){
				int callState=tm.getCallState() ;		
				Log.i("huangjun00","MainActivity  onCreate callState =" +callState);
	
		if(callState == TelephonyManager.CALL_STATE_RINGING ){
			    Log.i("huangjun00","MainActivity  onCreate CALL_STATE_RINGING ");
			    // Device call state: Ringing. A new call arrived and is
			    //   ringing or waiting. In the latter case, another call is
			    // already active. 
			    
	        	   timeLayout.setVisibility(View.GONE);
	                mHourH.setVisibility(View.GONE);
	                mHourL.setVisibility(View.GONE);
	                mMinH.setVisibility(View.GONE);
	                mMinL.setVisibility(View.GONE);
	                mTimeFormate.setVisibility(View.GONE);
	                mhour_min_sepretor.setVisibility(View.GONE);
	                miss_call_textview.setVisibility(View.GONE);
	                unread_message_textview.setVisibility(View.GONE);
	                mmiss_call_imageview.setVisibility(View.GONE);
	                munread_message_imageview.setVisibility(View.GONE);
	                mhour_min_sepretor.setVisibility(View.GONE);
			   //phoneNumber_textview.setText(number);
	                phoneName_textview.setVisibility(View.VISIBLE);
	                phoneNumber_textview.setVisibility(View.VISIBLE);	
			   //phoneNumber_textview.setText("18221926886");		   
			   //phoneName_textview.setText("abcdefghijklmnopq");
			   phoneNumber_textview.setText(hal_number);
			   phoneName_textview.setText(hal_name);
			   phoneName_textview.setSelected(true);
	                answer_new_panel.setVisibility(View.VISIBLE);  
			}
		else if  (callState == TelephonyManager.CALL_STATE_OFFHOOK ){
			      Log.i("huangjun00","MainActivity  onCreate CALL_STATE_OFFHOOK ");
			    //  Device call state: Off-hook. At least one call exists
			    //   that is dialing, active, or on hold, and no calls are ringing
			    //   or waiting. 	
	
	        	   timeLayout.setVisibility(View.GONE);
	                mHourH.setVisibility(View.GONE);
	                mHourL.setVisibility(View.GONE);
	                mMinH.setVisibility(View.GONE);
	                mMinL.setVisibility(View.GONE);
	                mTimeFormate.setVisibility(View.GONE);
	                mhour_min_sepretor.setVisibility(View.GONE);
	                miss_call_textview.setVisibility(View.GONE);
	                unread_message_textview.setVisibility(View.GONE);
	                mmiss_call_imageview.setVisibility(View.GONE);
	                munread_message_imageview.setVisibility(View.GONE);
	                mhour_min_sepretor.setVisibility(View.GONE);		
			   //phoneNumber_textview.setText(number);
	                phoneName_textview.setVisibility(View.VISIBLE);
	                phoneNumber_textview.setVisibility(View.VISIBLE);	
			   //phoneNumber_textview.setText("18221926886");		   
			   //phoneName_textview.setText("abcdefghijklmnopq");
			   phoneNumber_textview.setText(hal_number);
			   phoneName_textview.setText(hal_name);
			   phoneName_textview.setSelected(true);
	                answer_new_panel.setVisibility(View.VISIBLE);	
			   answer.setVisibility(View.INVISIBLE);
			}
		else{
		      Log.i("wangxing1","MainActivity  onCreate CALL_STATE_IDLE ");
	             //Device call state: No activity.
	                phoneName_textview.setVisibility(View.GONE);
	                phoneNumber_textview.setVisibility(View.GONE);
	                 answer_new_panel.setVisibility(View.GONE);
	     		}
		}
	*/
			registerUpdateTimeReceiver();
		}

		public void setMissCall(int num) {
			miss_call_num = num;
			miss_call_textview.setText("" + miss_call_num);
		}
	
		public void setUnReadMessage(int num) {
			unread_message_num = num;
			unread_message_textview.setText("" + unread_message_num);
		}	
	
 		public void updatetInfo() {
			Cursor csr = getContentResolver().query(CallLog.Calls.CONTENT_URI, new String[] { Calls.TYPE }, " type=? and new=?", new String[] { Calls.MISSED_TYPE + "", "1" }, "date desc");
			if (csr != null) {
				try {
					miss_call_num = csr.getCount();
				} finally {
					csr.close();
				}
			}

			csr = getContentResolver().query(Uri.parse("content://sms"), null, "type = 1 and read = 0", null, null);
			if (csr != null) {
				try {
					unread_message_num = csr.getCount();
				} finally {
					csr.close();
				}
			}
			miss_call_textview.setText("" + miss_call_num);
			unread_message_textview.setText("" + unread_message_num);
		}

		private void registerUpdateTimeReceiver(){
		    IntentFilter intentFilter = new IntentFilter();
			intentFilter.addAction("android.intent.action.TIME_SET");
			intentFilter.addAction("android.intent.action.TIME_TICK");
			intentFilter.addAction("android.intent.action.TIMEZONE_CHANGED");
			intentFilter.addAction("android.intent.action.DATE_CHANGED");
			registerReceiver(mUpdateTimeReceiver, intentFilter);
		} 
		
		private void unRegisterUpdateTimeReceiver(){
		   unregisterReceiver(mUpdateTimeReceiver);	
		}
	
	  public  void setTimeShowState(boolean value){				
			SharedPreferences sp = getSharedPreferences("TIME_SHOW",Context.MODE_WORLD_WRITEABLE + Context.MODE_WORLD_READABLE);
			SharedPreferences.Editor ed = sp.edit();
			ed.putBoolean("IS_SHOW", value);
			ed.commit();
		}

		@Override
		protected void onResume() {
			// TODO Auto-generated method stub
			super.onResume();
					/****
	        if (FeatureOption.SUNVOV_HALL_LEATHER_ANALOG_CLOCK) {
	            if (FeatureOption.SUNVOV_HALL_LEATHER_HUAWEI_ANALOG_CLOCK) {
			            mDigitalClock.setVisibility(View.VISIBLE);
			            mAnalogClock.setVisibility(View.VISIBLE);
			            mAnalogClock.enableHourMinute(false);
			            mAnalogClock.enableSeconds(false);
			            secondHandFlash();
			            updateTimeAndDate();
	            } else {
			            mDigitalClock.setVisibility(View.GONE);
			            mAnalogClock.setVisibility(View.VISIBLE);
			            mDateFormat = getString(R.string.abbrev_wday_month_day_no_year);
			            updateAnalogClockDate(mDateFormat, mClockFrame);
	            }
	        } else {
	        **/
	      //Log.i("huangjun00","MainActivity  onResume" );
	
	/*			
			Intent intent = getIntent();
			if(intent != null){
				hal_number = (String) intent.getExtra("halcall_number");
				}
			TelephonyManager tm = (TelephonyManager) this.getSystemService("phone");
		      Log.i("huangjun00","MainActivity  onCreate tm =" +tm);
			        if(tm != null){
			            int callState=tm.getCallState() ;		
		      Log.i("huangjun00","MainActivity  onCreate callState =" +callState);
	
	if(callState == TelephonyManager.CALL_STATE_RINGING ){
		      Log.i("wangxing1","MainActivity  onCreate CALL_STATE_RINGING ");
	        	   timeLayout.setVisibility(View.GONE);
	                mHourH.setVisibility(View.GONE);
	                mHourL.setVisibility(View.GONE);
	                mMinH.setVisibility(View.GONE);
	                mMinL.setVisibility(View.GONE);
	                mTimeFormate.setVisibility(View.GONE);
	                mhour_min_sepretor.setVisibility(View.GONE);
	                miss_call_textview.setVisibility(View.GONE);
	                unread_message_textview.setVisibility(View.GONE);
	                mmiss_call_imageview.setVisibility(View.GONE);
	                munread_message_imageview.setVisibility(View.GONE);
	                mhour_min_sepretor.setVisibility(View.GONE);	
			 // phoneNumber_textview.setText(number);
	                phoneName_textview.setVisibility(View.VISIBLE);
	                phoneNumber_textview.setVisibility(View.VISIBLE);
			   phoneNumber_textview.setText(hal_number);
			   phoneName_textview.setText(hal_name);
			   phoneName_textview.setSelected(true);
	                answer_new_panel.setVisibility(View.VISIBLE);	  
			}
		else if  (callState == TelephonyManager.CALL_STATE_OFFHOOK ){
		      Log.i("huangjun00","MainActivity  onCreate CALL_STATE_OFFHOOK ");
	        	   timeLayout.setVisibility(View.GONE);
	                mHourH.setVisibility(View.GONE);
	                mHourL.setVisibility(View.GONE);
	                mMinH.setVisibility(View.GONE);
	                mMinL.setVisibility(View.GONE);
	                mTimeFormate.setVisibility(View.GONE);
	                mhour_min_sepretor.setVisibility(View.GONE);
	                miss_call_textview.setVisibility(View.GONE);
	                unread_message_textview.setVisibility(View.GONE);
	                mmiss_call_imageview.setVisibility(View.GONE);
	                munread_message_imageview.setVisibility(View.GONE);
	                mhour_min_sepretor.setVisibility(View.GONE);	
			   //phoneNumber_textview.setText(number);
	                phoneName_textview.setVisibility(View.VISIBLE);
	                phoneNumber_textview.setVisibility(View.VISIBLE);	
			   phoneNumber_textview.setText(hal_number);
			   phoneName_textview.setText(hal_name);
			   phoneName_textview.setSelected(true);
	                answer_new_panel.setVisibility(View.VISIBLE);	
			   answer.setVisibility(View.INVISIBLE);
			}
		else{
			Log.i("huangjun00","MainActivity  onCreate CALL_STATE_IDLE ");
			phoneName_textview.setVisibility(View.GONE);
			phoneNumber_textview.setVisibility(View.GONE);
			answer_new_panel.setVisibility(View.GONE);	
		   }
	}
	*/			
	            mDigitalClock.setVisibility(View.VISIBLE);
	            mAnalogClock.setVisibility(View.GONE);
	            updateTimeAndDate();
	            updatetInfo();
		}


		@Override
		protected void onPause() {
			// TODO Auto-generated method stub
			super.onPause();
		}


		@Override
		protected void onDestroy() {
		// TODO Auto-generated method stub
			
		//	Intent intent = new Intent(this, TimeShowService.class);
		//	stopService(intent);
		    unRegisterUpdateTimeReceiver();
		//	LService.mIsActive = false;
	
		//  setTimeShowState(false);
		//	mKeyguardLock.reenableKeyguard();
	
			super.onDestroy();
		}

		public  void updateTimeAndDate(){
			
			int[] arrayOfInt1 = { R.drawable.time_number_00,
					R.drawable.time_number_01, R.drawable.time_number_02,
					R.drawable.time_number_03, R.drawable.time_number_04,
					R.drawable.time_number_05, R.drawable.time_number_06,
					R.drawable.time_number_07, R.drawable.time_number_08,
					R.drawable.time_number_09 };
			/**
			int[] arrayOfInt2 = { R.drawable.date_number_00,
					R.drawable.date_number_01, R.drawable.date_number_02,
					R.drawable.date_number_03, R.drawable.date_number_04,
					R.drawable.date_number_05, R.drawable.date_number_06,
					R.drawable.date_number_07, R.drawable.date_number_08,
					R.drawable.date_number_09 };
			int[] arrayOfInt3 = { R.drawable.week_sun, R.drawable.week_mon,
					R.drawable.week_tue, R.drawable.week_wed, R.drawable.week_thur,
					R.drawable.week_fri, R.drawable.week_sat };
			int[] arrayOfInt4 = { R.drawable.zh_week_sun, R.drawable.zh_week_mon,
					R.drawable.zh_week_tue, R.drawable.zh_week_wed,
					R.drawable.zh_week_thur, R.drawable.zh_week_fri,
					R.drawable.zh_week_sat };
			**/
			
			boolean is_24_hour_format = false;
			boolean is_chinese_language = false;
			int time = -1;
			int date = -1;
			int mon = -1;
			int image = -1;
			Calendar calendar = Calendar.getInstance();
			Date localDate = new Date();
			int hour = localDate.getHours();
			int min = localDate.getMinutes();
			int day = calendar.get(Calendar.DATE);
			int month = calendar.get(Calendar.MONTH) + 1;
			int week = calendar.get(Calendar.DAY_OF_WEEK) - 1;
			
			String str1 = Locale.getDefault().getLanguage();
			if((str1 != null) && (str1.equals("zh")) ){
				is_chinese_language = true;
			}
			
			is_24_hour_format = DateFormat.is24HourFormat(this);
			
			//time
			if(is_24_hour_format){
				mTimeFormate.setVisibility(View.GONE);
				time = hour / 10;
				image = arrayOfInt1[time];
				mHourH.setImageResource(image);
				time = hour % 10;
				image = arrayOfInt1[time];
				mHourL.setImageResource(image);	
			}else{
				mTimeFormate.setVisibility(View.VISIBLE);
				if(hour < 12){
					if(is_chinese_language){
						mTimeFormate.setImageResource(R.drawable.zh_am);
					}else{
						mTimeFormate.setImageResource(R.drawable.am);
					}
				}else{
					if(is_chinese_language){
						mTimeFormate.setImageResource(R.drawable.zh_pm);
					}else{
						mTimeFormate.setImageResource(R.drawable.pm);
					}
				}
				
				if ((12 > hour))// AM
				{
					if ((1 > hour))// 00:00-1:00
					{
						hour += 12;
					}
				} else// PM
				{
					if ((12 <= hour) && (13 > hour))// 12:00-13:00
					{
					} else {
						hour = hour - 12;
					}
				}
				time = hour / 10;
				image = arrayOfInt1[time];
				mHourH.setImageResource(image);
				time = hour % 10;
				image = arrayOfInt1[time];
				mHourL.setImageResource(image);		
			}
			
			time = min / 10;
			image = arrayOfInt1[time];
			mMinH.setImageResource(image);
			time = min % 10;
			image = arrayOfInt1[time];
			mMinL.setImageResource(image);
		    
			//date
			ContentResolver cv = getContentResolver();
			String strDateFormate = Settings.System.getString(cv, Settings.System.DATE_FORMAT);
			/**
			if(strDateFormate != null && strDateFormate.equals("dd-MM-yyyy")){
				mon = month / 10;
				image = arrayOfInt2[mon];
				mDateThird.setImageResource(image);
				mon = month % 10;
				image = arrayOfInt2[mon];
				mDateForth.setImageResource(image);
				date = day / 10;
				image = arrayOfInt2[date];
				mDateFirst.setImageResource(image);
				date = day % 10;
				image = arrayOfInt2[date];
				mDateSecond.setImageResource(image);				
			}else{
				mon = month / 10;
				image = arrayOfInt2[mon];
				mDateFirst.setImageResource(image);
				mon = month % 10;
				image = arrayOfInt2[mon];
				mDateSecond.setImageResource(image);
				date = day / 10;
				image = arrayOfInt2[date];
				mDateThird.setImageResource(image);
				date = day % 10;
				image = arrayOfInt2[date];
				mDateForth.setImageResource(image);		
			}
			
			//week
			if(is_chinese_language){
			    image = arrayOfInt4[week];
			}else{
				image = arrayOfInt3[week];
			}
				
			mWeek.setImageResource(image);
			**/
		}

    public static void updateAnalogClockDate(String dateFormat, View clock) {
        //if (FeatureOption.SUNVOV_D918_JX || FeatureOption.SUNVOV_D930_JX){
        //    return;
        //}    	
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());

        if (dateFormat != null) {
            CharSequence newDate = DateFormat.format(dateFormat, cal);
            Calendar calendar = Calendar.getInstance();
            int day = calendar.get(Calendar.DATE);
            TextView dateDisplay;
            dateDisplay = (TextView) clock.findViewById(R.id.analog_clock_date);
            if (dateDisplay != null) {
                dateDisplay.setVisibility(View.VISIBLE);
                //if (FeatureOption.SUNVOV_CUSTOM_D958_BP){
                //    dateDisplay.setText(String.valueOf(day));
                //} else {
                    dateDisplay.setText(newDate);
                //}
            }
        }
    }

    private void secondHandFlash() {
    		Date localDate = new Date();
    		int second = localDate.getSeconds();
    		float angle = second / 60.0f * 360.0f;
        final Animation rotationAnim = new RotateAnimation( angle, angle + 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotationAnim.setDuration(60 * 1000);
        rotationAnim.setRepeatCount(Animation.INFINITE);
        rotationAnim.setInterpolator(new LinearInterpolator());
        mImgSecondHand.setVisibility(View.VISIBLE);
        mImgSecondHand.startAnimation(rotationAnim);
    }

/*
    TelecomManager getTelecommService() {
      //  return (TelecomManager) mContext.getSystemService(Context.TELECOM_SERVICE);
        return (TelecomManager) this.getSystemService(Context.TELECOM_SERVICE);		
    }

    public  void endPhone() {
			Log.d("huangjun00", "endPhone()..................... " );		
			TelecomManager telecomManager = getTelecommService();
			Log.d("huangjun00", "endPhone telecomManager=" + telecomManager );					
			if (telecomManager != null) {	
				telecomManager.endCall();
				Log.d("huangjun00", "endPhone telecomManager.endCall" );
			   }
			}

    public  void answerPhone() {
	            Log.d("huangjun00", "answerPhone().................." );	
                    TelecomManager telecomManager = getTelecommService();
    	             Log.d("huangjun00", "answerPhone telecomManager=" + telecomManager );
					
                    if (telecomManager != null) {
    	              Log.d("huangjun00", "answerPhone telecomManageacceptRingingCall()   .isRinging()=" + telecomManager.isRinging() );
						
                        if (telecomManager.isRinging()) {
                         //   Log.i(TAG, "interceptKeyBeforeQueueing:"
                         //         + " CALL key-down while ringing: Answer the call!");
							                        telecomManager.acceptRingingCall();
                        	}
    	        }
    	}


    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {

	    //  String number  = incomingNumber;	
            Log.i("wangxing1", "[onCallStateChanged]state = " + state + ",incomingNumber = "
                    + incomingNumber);
		number = incomingNumber;
		phone_state = state;      
			
            if (state == TelephonyManager.CALL_STATE_RINGING ||state == TelephonyManager.CALL_STATE_OFFHOOK ) {
				
	      Log.i("wangxing1","MainActivity  onCallStateChanged 000 state =" +state+";number="+number);
                mHourH.setVisibility(View.GONE);
                mHourL.setVisibility(View.GONE);
                mMinH.setVisibility(View.GONE);
                mMinL.setVisibility(View.GONE);
                mTimeFormate.setVisibility(View.GONE);
                mhour_min_sepretor.setVisibility(View.GONE);
                miss_call_textview.setVisibility(View.GONE);
                unread_message_textview.setVisibility(View.GONE);
                mmiss_call_imageview.setVisibility(View.GONE);
                munread_message_imageview.setVisibility(View.GONE);
                mhour_min_sepretor.setVisibility(View.GONE);
				
		 // phoneNumber_textview.setText(number);
		   phoneNumber_textview.setText("18221926886");		   
		   phoneName_textview.setText("abcdefghijklmnopq");
		   phoneName_textview.setSelected(true);
 
            }
		else{
	      Log.i("wangxing1","MainActivity  onCallStateChanged 111 state =" +state+";number="+number);			
                phoneName_textview.setVisibility(View.GONE);
                phoneNumber_textview.setVisibility(View.GONE);

                 answer_new_panel.setVisibility(View.GONE);	

		}	
		
			
        }
    };

     public static String getContactNameByPhoneNumber(Context context, String address) {
         String[] projection = { ContactsContract.PhoneLookup.DISPLAY_NAME,
                 ContactsContract.CommonDataKinds.Phone.NUMBER };
 
         Cursor cursor = context.getContentResolver().query(
                 ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                 projection, // Which columns to return.
                 ContactsContract.CommonDataKinds.Phone.NUMBER + " = '"
                         + address + "'", // WHERE clause.  //modified this line to  {// null} can print all ContactsContract content
                 null, // WHERE clause value substitution
                 null); // Sort order.
            Log.i("huangjun11", "getContactNameByPhoneNumber  cursor = " + cursor +";address ="+address);
         if (cursor == null) {
            Log.i("huangjun11", "getContactNameByPhoneNumber  cursor ==null "); 
            return null;
         }
            Log.i("huangjun11", "getContactNameByPhoneNumber  cursor.getCount() = " + cursor.getCount());
         for (int i = 0; i < cursor.getCount(); i++) {
             cursor.moveToPosition(i);
 
             // 
            int nameFieldColumnIndex = cursor
                    .getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME);
                 String name = cursor.getString(nameFieldColumnIndex);
                 String  number = cursor.getString(1);
	           Log.i("huangjun11", "getContactNameByPhoneNumber  nameFieldColumnIndex= " + nameFieldColumnIndex +";name ="+name + ";number="+number);
             return name;
         }
         return null;
     }

    private OnTouchListener drap_handle_ontouchlistener=new OnTouchListener() {

		int mOriginalX;	
		int mOriginalY ;
		int lastX;
		int initL, initT, initB, initR;
		int l, b, r, t;
		int screenWidth = 480, screenHeight = 854;

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			int x = (int) event.getRawX();
			int y = (int) event.getRawY();

			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				Log.d("lzz", "ontouchlistener ACTION_DOWN x=" + x + " y=" + y);
				mOriginalX = (int) event.getRawX();//Being touch event original X coordinate of the touch position				
				mOriginalY = (int) event.getRawY();
				lastX=mOriginalX;

				initL = v.getLeft();
				initT = v.getTop();
				initR = initL + v.getWidth();
				initB = initT + v.getHeight();

				break;
			case MotionEvent.ACTION_MOVE:
				Log.d("lzz", "ontouchlistener ACTION_MOVE x=" + x + " y=" + y);
				int dx = (int) event.getRawX() - lastX;
				l = v.getLeft() + dx;
				b = initB;
				r = v.getRight() + dx;
				t = initT;
				// The following determine whether to move beyond the screen
				if (l < 0) {
					l = 20;
					r = l + v.getWidth();
				}
				if (r > screenWidth) {
					r = screenWidth-20;
					l = r - v.getWidth();
				}
				v.layout(l, initT, r, b);
				lastX = (int) event.getRawX();
				v.postInvalidate();
				
				if (x<DECLINE_X){
					Log.d("lzz", "ontouchlistener x< DECLINE_X  ........... x=" + x );
					endPhone();

						{
        	   					   timeLayout.setVisibility(View.VISIBLE);							
					                mHourH.setVisibility(View.VISIBLE);
					                mHourL.setVisibility(View.VISIBLE);
					                mMinH.setVisibility(View.VISIBLE);
					                mMinL.setVisibility(View.VISIBLE);
					                mTimeFormate.setVisibility(View.VISIBLE);
					                mhour_min_sepretor.setVisibility(View.VISIBLE);
					                miss_call_textview.setVisibility(View.VISIBLE);
					                unread_message_textview.setVisibility(View.VISIBLE);
					                mmiss_call_imageview.setVisibility(View.VISIBLE);
					                munread_message_imageview.setVisibility(View.VISIBLE);
					                mhour_min_sepretor.setVisibility(View.VISIBLE);
							 //  phoneNumber_textview.setText("18221926886");		   
							 //  phoneName_textview.setText("abcdefghijklmnopq");
							 //  phoneName_textview.setSelected(true);
					             //   answer_new_panel.setVisibility(View.VISIBLE);		
					                phoneName_textview.setVisibility(View.GONE);
					                phoneNumber_textview.setVisibility(View.GONE);
					                 answer_new_panel.setVisibility(View.GONE);	
						}
				}else if (x>ANSWER_X){
						Log.d("lzz", "ontouchlistener x>ANSWER_X.............. x=" + x );
		
					answerPhone();
					answer.setVisibility(View.INVISIBLE);
				}
				break;

			case MotionEvent.ACTION_UP:
				v.layout(initL, initT, initR, initB);
				lastX = mOriginalX;
				v.postInvalidate();
				break;
			default:
				break;
			}
			return true;
		}
	};	

    private OnTouchListener photo_ontouchlistener=new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            Log.d("photo_ontouchlistener", " lzz  MotionEvent.ACTION_DOWN=" + MotionEvent.ACTION_DOWN+" event.getAction()="+event.getAction());
            return true;
        }
    };	

    public void updateCallCard(boolean lidOpen,boolean isIncoming) {
    
        if (answer_new_panel==null || answer==null || drap_handle==null){
            return;
        }
		
        if(lidOpen){    
			
            if (answer_new_panel.getVisibility()==View.VISIBLE){
                return;
            }
			
            int height = getResources().getDimensionPixelSize(R.dimen.timeshow_answer_new_panel_height);
            answer_new_panel.setVisibility(View.VISIBLE);
            RelativeLayout.LayoutParams params =  (RelativeLayout.LayoutParams)answer_new_panel.getLayoutParams();
            params.height = height;
            answer_new_panel.setLayoutParams(params);
            answer.setVisibility(isIncoming?View.VISIBLE:View.INVISIBLE);
            drap_handle.setOnTouchListener(drap_handle_ontouchlistener);
        }else{
        
            if (answer_new_panel.getVisibility()==View.INVISIBLE){
                return;
            }
      
            int height = 0;
            RelativeLayout.LayoutParams params =  (RelativeLayout.LayoutParams)answer_new_panel.getLayoutParams();
            params.height = height;
            answer_new_panel.setLayoutParams(params);
            answer_new_panel.setVisibility(View.INVISIBLE);
        }		
    }
*/
	
}
