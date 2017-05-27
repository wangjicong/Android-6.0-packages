package com.wx.hallview;

import android.app.Application;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.Log;

import com.wx.hallview.views.utils.ColorUtil;

public class HallViewApplication extends Application
        implements Thread.UncaughtExceptionHandler
{
    private HallReceiver mReceiver;

    public void onCreate()
    {
        super.onCreate();
        ColorUtil.init(getResources());
        Thread.setDefaultUncaughtExceptionHandler(this);
        mReceiver = new HallReceiver();
        IntentFilter localIntentFilter = new IntentFilter();
        localIntentFilter.addAction("android.intent.action.SCREEN_ON");
        localIntentFilter.addAction("android.intent.action.SCREEN_OFF");
        registerReceiver(mReceiver, localIntentFilter);
    }

    public void uncaughtException(Thread paramThread, Throwable paramThrowable)
    {
        Log.e("HallViewApplication", "fatal Exception happend:", paramThrowable);
        System.exit(0);
    }

	@Override
	public Resources getResources() {
		Resources res =  super.getResources();
		Configuration config=new Configuration();
		config.setToDefaults();
		res.updateConfiguration(config,res.getDisplayMetrics() );
		return res;
	}
}