package com.wx.hallview.fragment;

/**
 * Created by Administrator on 16-1-26.
 */

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.provider.Settings.System;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.wx.hallview.bean.WeatherInfo;
import com.wx.hallview.services.WeatherService;
import com.wx.hallview.services.WeatherService.MyBinder;
import com.wx.hallview.services.WeatherService.WeatherLoadListener;
import com.wx.hallview.views.ImageViewWithUnReadNumber;
import com.wx.hallview.views.MyQAnalogClock;
import com.wx.hallview.views.utils.DataSave;
import java.io.PrintStream;
import java.util.Calendar;
import com.wx.hallview.R;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.os.Handler;


public class ClockFragment extends BaseFragmentView
        implements WeatherService.WeatherLoadListener
{
    private Intent intent;
    private MyQAnalogClock mClockView;
    private TextView mDayWeek;
    private ImageViewWithUnReadNumber mPhoneIcon;
    private WeatherService mService;
    private ServiceConnection mServiceConnection;
    private ImageViewWithUnReadNumber mSmsIcon;
    private ImageView mWeatherIcon;
    private ReceiveMms receiveMms;
    private int unReadMmsNumber = 0;
    private int unReadPhoneNumber = 0;
    private TelephonyManager mTelephonyManager;

    public ClockFragment(Context paramContext)
    {
        super(paramContext);
        mTelephonyManager = (TelephonyManager)paramContext.getSystemService(Context.TELEPHONY_SERVICE);
        mTelephonyManager.listen(mListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    private void initDayWeek()
    {
        String str1 = this.mClockView.getDay();
        String str2 = this.mClockView.getWeek();
        
        //qiuyaobo,remove weather,20160901,begin
        this.mDayWeek.setText(str1 + "-" + str2 + ".");
        //this.mDayWeek.setText(str1 + "\n" + str2 + ".");
        //qiuyaobo,remove weather,20160901,end
    }

    private void initWeatherIcon()
    {
        String str = DataSave.getWeather(getContext());
        int i = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if ((i > 19) || (i < 6))
        {
            this.mWeatherIcon.setImageResource(R.drawable.clock_night);
            return;
        }
        if (str.contains("cloudy"))
        {
            this.mWeatherIcon.setImageResource(R.drawable.clock_cloud);
            return;
        }
        if ((str.contains("sunny")) || (str.contains("clear")))
        {
            this.mWeatherIcon.setImageResource(R.drawable.clock_sunny);
            return;
        }
        if ((str.contains("snow")) || (str.contains("flurries")))
        {
            this.mWeatherIcon.setImageResource(R.drawable.clock_snow);
            return;
        }
        if (str.contains("rain"))
        {
            this.mWeatherIcon.setImageResource(R.drawable.clock_rain);
            return;
        }
        this.mWeatherIcon.setImageResource(R.drawable.clock_cloud);
    }

    public boolean needShowBackButton()
    {
        return false;
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
    
    
    public void onAttach()
    {
        this.mClockView.startTiming();
        initWeatherIcon();
        initDayWeek();
        getContext().bindService(this.intent, this.mServiceConnection, Context.BIND_AUTO_CREATE);
      //  Log.d("ClockFragment", "onAttach");

       
        this.unReadMmsNumber = getNewSmsCount();// Settings.System.getInt(getContext().getContentResolver(), "com_android_mms_mtk_unread",0);
        this.mSmsIcon.setUnReadNumber(this.unReadMmsNumber);
       // Log.d("ClockFragment", "onAttach:unReadMmsNumber" + this.unReadMmsNumber);

        this.unReadPhoneNumber = getMissCallCount();//Settings.System.getInt(getContext().getContentResolver(), "com_android_contacts_mtk_unread",0);
        this.mPhoneIcon.setUnReadNumber(this.unReadPhoneNumber);
       // Log.d("ClockFragment", "onAttach:unReadPhoneNumber" + this.unReadPhoneNumber);

        this.receiveMms = new ReceiveMms();
        IntentFilter localIntentFilter = new IntentFilter();
        localIntentFilter.addAction("com.mediatek.action.UNREAD_CHANGED");
        getContext().registerReceiver(this.receiveMms, localIntentFilter);

        return;
    }

    public View onCreateView(LayoutInflater paramLayoutInflater, ViewGroup paramViewGroup)
    {
        ViewGroup view = (ViewGroup)paramLayoutInflater.inflate(R.layout.clock_view, paramViewGroup, false);
        this.mSmsIcon = ((ImageViewWithUnReadNumber)view.findViewById(R.id.message_icon));
        this.mPhoneIcon = ((ImageViewWithUnReadNumber)view.findViewById(R.id.phone_icon));
        this.mClockView = ((MyQAnalogClock)view.findViewById(R.id.analog_clock));
        this.mDayWeek = ((TextView)view.findViewById(R.id.tv_simple_day_week));
        this.mWeatherIcon = ((ImageView)view.findViewById(R.id.iv_simple_weather_icon));
        
        //qiuyaobo,remove weather,20160901,begin
        this.mWeatherIcon.setVisibility(View.GONE);
        //qiuyaobo,remove weather,20160901,end
        
        this.mServiceConnection = new ServiceConnection()
        {
            public void onServiceConnected(ComponentName paramComponentName, IBinder paramIBinder)
            {
                ClockFragment.this.mService =  ((WeatherService.MyBinder)paramIBinder).getService();
                ClockFragment.this.mService.getWeatherInfo(ClockFragment.this);
            }

            public void onServiceDisconnected(ComponentName paramComponentName)
            {
                ClockFragment.this.mService = null;
            }
        };
        this.intent = new Intent(getContext(), ClockFragment.class);
        this.intent.setClassName("com.wx.hallview", "com.wx.hallview.services.WeatherService");
        return view;
    }

    public void onDetach()
    {
        Log.d("ClockFragment", "onDetach");
        this.mClockView.pauseTiming();
        if (this.receiveMms != null)
            getContext().unregisterReceiver(this.receiveMms);
    }

    public void onScreenOff()
    {
        this.mClockView.pauseTiming();
    }

    public void onScreenOn()
    {
        this.mClockView.startTiming();
        initDayWeek();
    }

    public void onWeatherInfoChanged(int paramInt, WeatherInfo paramWeatherInfo)
    {
        if (paramInt == 0)
        {
            initWeatherIcon();
        }
        getContext().unbindService(this.mServiceConnection);
        return;
    }

    public class ReceiveMms extends BroadcastReceiver
    {
        public ReceiveMms()
        {
        }

        public void onReceive(Context paramContext, Intent paramIntent)
        {

            ClockFragment.this.mSmsIcon.setUnReadNumber(getNewSmsCount());
            ClockFragment.this.mSmsIcon.invalidate();
            return;
        }
    }
    
    
    private PhoneStateListener mListener = new PhoneStateListener() {
        
        public void onCallStateChanged(int state, String incomingNumber) {
        	     new Handler().postDelayed(new Runnable(){   
                     public void run() {
                     	  unReadPhoneNumber = getMissCallCount();
                          mPhoneIcon.setUnReadNumber(unReadPhoneNumber);
                     }   
                 }, 2000);
        }
    };
}
