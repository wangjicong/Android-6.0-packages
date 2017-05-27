package com.wx.hallview.fragment;

/**
 * Created by Administrator on 16-1-23.
 */
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.content.ComponentName;
import com.wx.hallview.views.ImageViewWithUnReadNumber;
import android.content.ContentResolver;
import android.util.Log;
import com.wx.hallview.services.WeatherService;
import android.widget.TextView;
import android.os.Handler;
import android.os.IBinder;

import android.content.ServiceConnection;
import android.widget.ImageView;
import com.wx.hallview.views.utils.DataSave;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import android.content.IntentFilter;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.View;
import com.wx.hallview.bean.WeatherInfo;
import java.io.PrintStream;
import com.wx.hallview.R;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

public class NumberClockFragment extends BaseFragmentView implements WeatherService.WeatherLoadListener {
    private Intent intent;
    private TextView mAmPm;
    private TextView mDayWeek;
    private TextView mNumberTime;
    private ImageViewWithUnReadNumber mPhoneIcon;
    private TextView mPointSecond;
    private WeatherService mService;
    private ServiceConnection mServiceConnection;
    private ImageViewWithUnReadNumber mSmsIcon;
    private ImageView mWeatherIcon;
    private NumberClockFragment.ReceiveMms receiveMms;
    private String timeMode;
    private Handler mHandler = new Handler();
    private boolean mTiming = false;
    
    private int unReadMmsNumber = 0;
    private int unReadPhoneNumber = 0;
    private TelephonyManager mTelephonyManager;
    
    private Runnable mRunnable = new Runnable() {
        
        public void run() {
            long startRun = System.currentTimeMillis();
            Calendar calendar = Calendar.getInstance();
            Date date = new Date();
            calendar.setTime(date);
            String week = String.format("%ta", new Object[] {date});
            String day = String.format("%td", date);
            
			      //qiuyaobo,remove weather,20160906,begin
			      mDayWeek.setText(day + "-" + week + ".");
			      //mDayWeek.setText(day + "\n" + week + ".");
			      //qiuyaobo,remove weather,20160906,end            
            
            mNumberTime.setText(String.format("%02d:%02d", new Object[] { Integer.valueOf(calendar.get(10)), Integer.valueOf(calendar.get(12)) }));
            if((calendar.get(13) % 2) == 1) {
                mPointSecond.setVisibility(View.VISIBLE);
            } else {
                mPointSecond.setVisibility(View.INVISIBLE);
            }
            String ampm = String.format("%tp", new Object[] {date});
            mAmPm.setText(ampm.toUpperCase());
            if("24".equals(timeMode)) {
                mNumberTime.setText(String.format("%02d %02d", new Object[] { Integer.valueOf(calendar.get(11)), Integer.valueOf(calendar.get(12)) }));
            } else {
            	 /* Kalyy Bug 52140 {*/
                if(Integer.valueOf(calendar.get(10)) == 0){
                    mNumberTime.setText("12 "+String.format("%02d", new Object[] { Integer.valueOf(calendar.get(12)) }));
                }else if(Integer.valueOf(calendar.get(10))<10){
                    mNumberTime.setText("  "+String.format("%d %02d", new Object[] { Integer.valueOf(calendar.get(10)), Integer.valueOf(calendar.get(12)) }));
                }else{
                    mNumberTime.setText(String.format("%2d %02d", new Object[] { Integer.valueOf(calendar.get(10)), Integer.valueOf(calendar.get(12)) }));
                }
                /* Kalyy Bug 52140 }*/
            }
            long endRun = System.currentTimeMillis();
            long useTime = endRun - startRun;
            mHandler.postDelayed(this, (1000 - useTime));
        }
    };
 
    protected View onCreateView(LayoutInflater inflater, ViewGroup container) {
        View view = inflater.inflate(R.layout.number_clock_fragment, container, false);
        mDayWeek = (TextView)view.findViewById(R.id.tv_day_week);
        mAmPm = (TextView)view.findViewById(R.id.tv_am_pm);
        mWeatherIcon = (ImageView)view.findViewById(R.id.iv_weather_icon);
        mNumberTime = (TextView)view.findViewById(R.id.tv_number_time);
        mPointSecond = (TextView)view.findViewById(R.id.tv_point_second);
        mPhoneIcon = (ImageViewWithUnReadNumber)view.findViewById(R.id.number_phone_icon);
        mSmsIcon = (ImageViewWithUnReadNumber)view.findViewById(R.id.number_message_icon);
        
        //qiuyaobo,remove weather,20160906,begin
        mWeatherIcon.setVisibility(View.GONE);
        //qiuyaobo,remove weather,20160906,end        
        
        mServiceConnection = new ServiceConnection() {
            
            public void onServiceDisconnected(ComponentName arg0) {
                mService = null;
            }
            
            public void onServiceConnected(ComponentName cpn, IBinder binder) {
                System.out.println("weather:service connected");
                mService = ((WeatherService.MyBinder)binder).getService();
                mService.getWeatherInfo(NumberClockFragment.this);
            }
        };
        intent = new Intent(getContext(), NumberClockFragment.class);
        intent.setClassName("com.wx.hallview", "com.wx.hallview.services.WeatherService");
        return view;
    } 
    
    public void onWeatherInfoChanged(int result, WeatherInfo mWeatherInfo) {
        if(result == 0) {
            initWeatherIcon();
            System.out.println("xuehui:update weather info");
        } else {
            System.out.println("xuehui:update weather info failed: " + result);
        }
        getContext().unbindService(mServiceConnection);
    }
    
    public NumberClockFragment(Context context) {
        super(context);
        mTelephonyManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        mTelephonyManager.listen(mListener, PhoneStateListener.LISTEN_CALL_STATE);
    }
    
    private void start() {
        if(!mTiming) {
            mTiming = true;
            mHandler.post(mRunnable);
        }
    }
    
    private void pause() {
        if(mTiming) {
            mTiming = false;
            mHandler.removeCallbacks(mRunnable);
        }
    }
    
    public void onScreenOff() {
        super.onScreenOff();
        pause();
    }
    
    public void onScreenOn() {
        super.onScreenOn();
        start();
    }
    
    private int getNewSmsCount() {  
        int result = 0;  
        Cursor csr = getContext().getContentResolver().query(Uri.parse("content://sms"), null,
                "type = 1 and read = 0", null, null);  
        if (csr != null) {  
            result = csr.getCount();  
            csr.close();  
        }  
        return result;  
    } 
    
    private int getMissCallCount() {  
        int result = 0;  
        Cursor cursor = getContext().getContentResolver().query(CallLog.Calls.CONTENT_URI, new String[] {  
                Calls.TYPE  
            }, " type=? and new=?", new String[] {  
                    Calls.MISSED_TYPE + "", "1"  
            }, "date desc");  
      
        if (cursor != null) {  
            result = cursor.getCount();  
            cursor.close();  
        }  
        return result;  
    }
    
    
    protected void onAttach() {
        initWeatherIcon();
        getContext().bindService(intent, mServiceConnection, 1);
        timeMode = Settings.System.getString(getContext().getContentResolver(), "time_12_24");
        mHandler.post(mRunnable);
       // try {
            unReadMmsNumber = getNewSmsCount();//Settings.System.getInt(getContext().getContentResolver(), "com_android_mms_mtk_unread");
            mSmsIcon.setUnReadNumber(unReadMmsNumber);
       //     Log.d("NumberClockFragment", "onAttach:unReadMmsNumber" + unReadMmsNumber);
      //  } catch(Settings.SettingNotFoundException e1) {
      //      Log.d("NumberClockFragment", "onAttach:sms:SettingNotFoundException");
      //      mSmsIcon.setUnReadNumber(unReadMmsNumber);
      //      e1.printStackTrace();
      //  }
      //  try {
            unReadPhoneNumber = getMissCallCount();//Settings.System.getInt(getContext().getContentResolver(), "com_android_contacts_mtk_unread");
            mPhoneIcon.setUnReadNumber(unReadPhoneNumber);
        //    Log.d("NumberClockFragment", "onAttach:unReadPhoneNumber" + unReadPhoneNumber);
        //} catch(Settings.SettingNotFoundException e) {
        //    Log.d("NumberClockFragment", "onAttach:phone:SettingNotFoundException");
        //    mPhoneIcon.setUnReadNumber(unReadPhoneNumber);
        //    e.printStackTrace();
        //}
        receiveMms = new NumberClockFragment.ReceiveMms();
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.mediatek.action.UNREAD_CHANGED");
        getContext().registerReceiver(receiveMms, filter);
        start();
    }
    
    private void initWeatherIcon() {
        String weather = DataSave.getWeather(getContext());
        int currentHour = Calendar.getInstance().get(11);
        if((currentHour > 18) || (currentHour < 6)) {
            mWeatherIcon.setImageResource(R.drawable.clock_night);
            return;
        }
        if(weather.contains("cloudy")) {
            mWeatherIcon.setImageResource(R.drawable.clock_cloud);
            return;
        }
        if((weather.contains("sunny")) || (weather.contains("clear"))) {
            mWeatherIcon.setImageResource(R.drawable.clock_sunny);
            return;
        }
        if((weather.contains("snow")) || (weather.contains("flurries"))) {
            mWeatherIcon.setImageResource(R.drawable.clock_snow);
            return;
        }
        if(weather.contains("rain")) {
            mWeatherIcon.setImageResource(R.drawable.clock_rain);
            return;
        }
        mWeatherIcon.setImageResource(R.drawable.clock_cloud);
    }
    
    protected void onDetach() {
        super.onDetach();
        pause();
        Log.d("NumberClockFragment", "onDetach");
        if(receiveMms != null) {
            getContext().unregisterReceiver(receiveMms);
        }
    }
    
    public boolean needShowBackButton() {
        return false;
    }
    

    
    public class ReceiveMms extends BroadcastReceiver {
	    public void onReceive(Context paramContext, Intent paramIntent)
	    {
	    //  try
	    //  {
	        NumberClockFragment.this.mSmsIcon.setUnReadNumber(getNewSmsCount());
	        NumberClockFragment.this.mSmsIcon.invalidate();
	        return;
	     // }catch (Settings.SettingNotFoundException e)
	     // {
	     //   Log.d("NumberClockFragment", "onReceive:SettingNotFoundException");
	     //   NumberClockFragment.this.mSmsIcon.setUnReadNumber(0);
	     //   e.printStackTrace();
	     //   return;
	     // }
	     // finally
	     // {
	     //       NumberClockFragment.this.mSmsIcon.invalidate();
	     // }
	    }
    }
    
    private PhoneStateListener mListener = new PhoneStateListener() {
        
        public void onCallStateChanged(int state, String incomingNumber) {
        	     mHandler.postDelayed(new Runnable(){   
                     public void run() {
                     	  unReadPhoneNumber = getMissCallCount();
                          mPhoneIcon.setUnReadNumber(unReadPhoneNumber);
                     }   
                }, 2000); 
                 
        }
    };
}
