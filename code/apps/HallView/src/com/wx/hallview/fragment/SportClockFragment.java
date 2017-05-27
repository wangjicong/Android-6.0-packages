package com.wx.hallview.fragment;

import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.View;
import android.widget.TextView;
import android.widget.ImageView;

import com.wx.hallview.bean.WeatherInfo;
import com.wx.hallview.R;
import com.wx.hallview.services.WeatherService;
import com.wx.hallview.views.ButteryWithProgress;
import com.wx.hallview.views.MyQAnalogClock;
import com.wx.hallview.views.utils.DataSave;

import java.util.Calendar;

public class SportClockFragment extends BaseFragmentView implements WeatherService.WeatherLoadListener {
	private final float radius = 51.4286f;
	private final String TAG = "SportClockFragment";
    private Intent intent;
    private SportClockFragment.ButteryReciver mButteryReciver;
    private ButteryWithProgress mButteryView;
    private TextView mDayWeek;
    private WeatherService mService;
    private ServiceConnection mServiceConnection;
    private MyQAnalogClock mSportClock;
    private ImageView mWeatherIcon;
    private ImageView mWeekPosition;
    
    public SportClockFragment(Context context) {
        super(context);
        mButteryReciver = new SportClockFragment.ButteryReciver();
    }
    
    protected View onCreateView(LayoutInflater inflater, ViewGroup container) {
        View view = inflater.inflate(R.layout.sport_clock_fragment, container, false);
        mWeekPosition = (ImageView)view.findViewById(R.id.iv_sport_week);
        mWeatherIcon = (ImageView)view.findViewById(R.id.iv_sport_weather_icon);
        mDayWeek = (TextView)view.findViewById(R.id.tv_sport_day_week);
        mSportClock = (MyQAnalogClock) view.findViewById(R.id.sport_clock);
        mButteryView = (ButteryWithProgress)view.findViewById(R.id.buttery_container);
        
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
                mService.getWeatherInfo(SportClockFragment.this);
            }
        };
        intent = new Intent(getContext(), SportClockFragment.class);
        intent.setClassName("com.wx.hallview", "com.wx.hallview.services.WeatherService");
        return view;
    }
    
    protected void onAttach() {
        initWeatherIcon();
        mSportClock.startTiming();
        initDayWeek();
        getContext().bindService(intent, mServiceConnection, 1);
        getContext().registerReceiver(mButteryReciver, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
    }
    
    protected void onDetach() {
        mSportClock.pauseTiming();
        getContext().unregisterReceiver(mButteryReciver);
    }
    
    public void onScreenOff() {
        mSportClock.pauseTiming();
    }
    
    public void onScreenOn() {
        mSportClock.startTiming();
        initDayWeek();
    }
    
    class ButteryReciver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra("level", 0);
            int scale = intent.getIntExtra("scale", 100);
            int butteryRemain = (level * 100) / scale;
			Log.d(TAG, "level=" + level+ " scale="+scale+" butteryRemain="+butteryRemain);
            mButteryView.setButterRemain(butteryRemain);
        }
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
    
    private void initDayWeek() {
        String week = mSportClock.getWeek();
        String day = mSportClock.getDay();
        mDayWeek.setText(day + "-" + week + ".");
		float mFloat[] = {0.0f, 0.0f};
		
		if(mSportClock.mdayOfWeek == 0){//sunday
			mFloat[0] = radius;
		}else{
			mFloat[0] = -radius*(mSportClock.mdayOfWeek-1);
			mFloat[1] = -radius*mSportClock.mdayOfWeek;
		}
		ObjectAnimator.ofFloat(mWeekPosition, "rotation", mFloat).start();
    }
    
    public boolean needShowBackButton() {
        return false;
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
}

