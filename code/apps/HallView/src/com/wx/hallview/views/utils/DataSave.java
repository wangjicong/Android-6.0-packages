package com.wx.hallview.views.utils;

/**
 * Created by Administrator on 16-1-22.
 */

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import com.wx.hallview.bean.WeatherInfo;

public class DataSave
{
    public static String getClockStyle(Context paramContext)
    {
        return getPreferences(paramContext).getString("current_tag", "clock");
    }

    private static SharedPreferences getPreferences(Context paramContext)
    {
        return paramContext.getSharedPreferences("hall_view_data", 0);
    }

    public static String getWeather(Context paramContext)
    {
        return getPreferences(paramContext).getString("text", "");
    }

    public static WeatherInfo getWeatherInfo(Context paramContext)
    {
        SharedPreferences sp = getPreferences(paramContext);
        WeatherInfo localWeatherInfo = new WeatherInfo();
        localWeatherInfo.setCountyName(sp.getString("countyName", ""));
        localWeatherInfo.setTemp(sp.getString("temp", ""));
        localWeatherInfo.setDate(sp.getString("date", ""));
        localWeatherInfo.setText(sp.getString("text", ""));
        localWeatherInfo.setLastRefreshDate(sp.getString("lastRefreshDate", ""));
        return localWeatherInfo;
    }

    public static void saveClockStyle(Context paramContext, String paramString)
    {
        getPreferences(paramContext).edit().putString("current_tag", paramString).commit();
    }

    public static void saveWeatherInfo(Context paramContext, WeatherInfo paramWeatherInfo)
    {
        SharedPreferences.Editor prefs  = getPreferences(paramContext).edit();
        prefs.putString("countyName", paramWeatherInfo.getCountyName());
        prefs.putString("temp", paramWeatherInfo.getTemp());
        prefs.putString("date", paramWeatherInfo.getDate());
        prefs.putString("text", paramWeatherInfo.getText());
        prefs.putString("lastRefreshDate", paramWeatherInfo.getLastRefreshDate());
        prefs.commit();
    }
}