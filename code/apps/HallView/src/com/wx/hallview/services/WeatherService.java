package com.wx.hallview.services;

/**
 * Created by Administrator on 16-1-27.
 */
import android.app.Service;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import com.wx.hallview.bean.WeatherInfo;
import com.wx.hallview.views.utils.DataSave;
import com.wx.hallview.views.utils.HttpUtil;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class WeatherService extends Service implements LocationListener
{
    private ConnectivityManager mConnectivityManager;
    private boolean mIsRunning = false;
    private List<WeatherLoadListener> mListeners;
    private LocationManager mLocationManager;
    private SimpleDateFormat mPFormat = new SimpleDateFormat("E, dd MMM yyyy hh:mm a", Locale.US);
    private long mTimeDiff = -1L;
    private WeatherInfo mWeatherInfo;

    private void onWeatherLoaded(WeatherInfo weatherInfo, int result)
    {
        Log.d("WeatherService", "weather:onWeatherLoaded");
        synchronized(mListeners) {
            Log.d("WeatherService", "weather:no lock");
            for(WeatherService.WeatherLoadListener listener : mListeners) {
                Log.d("WeatherService", "weather:return");
                listener.onWeatherInfoChanged(result, weatherInfo);
            }
            mListeners.clear();
            mIsRunning = false;
        }
    }

    private void onWeatherLoadedFailed(int result)
    {
        onWeatherLoaded(null, result);
    }

    private void startLoading(WeatherLoadListener listener)
    {
        if(!mIsRunning) {
            mIsRunning = true;
            int result = -1;
            if(mConnectivityManager != null) {
                NetworkInfo networkInfo = mConnectivityManager.getActiveNetworkInfo();
                if((networkInfo != null) && (networkInfo.isConnected())) {
                    if(networkInfo.getState() == NetworkInfo.State.CONNECTED) {
                        if(mLocationManager.isProviderEnabled("network")) {
                        	result = 0;
                        } else {
                        	  Log.d("WeatherService", "weather:please check location setting");
                            result = 5;
                        }
                    } else {
                    	  Log.d("WeatherService", "weather:current network is not available");
                        result = 4;
                    }
                } else {
                	  Log.d("WeatherService", "weather:no network connection");
                    result = 3;
                }
            } else {
            	  Log.d("WeatherService", "weather:no network service");
                result = 2;
            }
            if(result == 0) {
                Log.d("WeatherService", "weather:requestLocation");
                mLocationManager.requestLocationUpdates("network", 1000, 0.0f, this);
            } else {
            	  onWeatherLoadedFailed(result);
            }
        } else {
            Log.d("WeatherService", "weather: warning weather info is loading.");
        }
    }

    public void getWeatherInfo(WeatherLoadListener paramWeatherLoadListener)
    {
        try
        {
            this.mWeatherInfo = DataSave.getWeatherInfo(this);
            Date localDate = new Date();
            Object localObject = this.mWeatherInfo.getLastRefreshDate();
            if (TextUtils.isEmpty((CharSequence)localObject))
            {
                getWeatherInfo(paramWeatherLoadListener, true);
                return;
            }
            localObject = this.mPFormat.parse((String)localObject);
            this.mTimeDiff = (localDate.getTime() - ((Date)localObject).getTime());
            this.mTimeDiff = (this.mTimeDiff % 86400000L / 3600000L);
            Log.d("WeatherService", "weather:mTimeDiff=" + this.mTimeDiff);
            if (this.mTimeDiff >= 3L)
            {
                Log.d("WeatherService", "weather:need update");
                getWeatherInfo(paramWeatherLoadListener, true);
            } else {
                Log.d("WeatherService", "weather:needn't update");
                getWeatherInfo(paramWeatherLoadListener, false);
            }
        }catch (ParseException e)
        {
            e.printStackTrace();
        }
    }

    public void getWeatherInfo(WeatherService.WeatherLoadListener listener, boolean forceLoad) {
        if(!forceLoad) {
            Log.d("WeatherService", "weather:!isNeedUpdate");
            if(mWeatherInfo == null) {
                mWeatherInfo = DataSave.getWeatherInfo(this);
            }
            listener.onWeatherInfoChanged(0, mWeatherInfo);
            return;
        }
        synchronized(mListeners) {
            Log.d("WeatherService", "weather:isNeedUpdate");
            if(mListeners.contains(listener)) {
                Log.d("WeatherService", "mListeners.contains(listener)");
            } else {
                mListeners.add(listener);
                startLoading(listener);
            }
        }
    }

    public IBinder onBind(Intent paramIntent)
    {
        return new MyBinder();
    }

    public void onCreate()
    {
        Log.d("WeatherService", "weather:service created");
        mListeners = new ArrayList();
        mLocationManager = (LocationManager)getSystemService("location");
        mConnectivityManager = (ConnectivityManager)getSystemService("connectivity");
    }

    public void onDestroy()
    {
        Log.d("WeatherService", "weather:Service onDestroy");
        super.onDestroy();
    }

    public void onLocationChanged(Location location)
    {
        Log.d("WeatherService", "weather:location success,begin get address info");
        new WeatherService.GetWeatherInfoAsyncTask().execute(new Location[] {location});
        mLocationManager.removeUpdates(this);
    }

    public void onProviderDisabled(String str) {
        Log.d("WeatherService", "weather:onProviderDisabled");
        onWeatherLoadedFailed(5);
        mLocationManager.removeUpdates(this);
    }

    public void onProviderEnabled(String str) {
        Log.d("WeatherService", "weather:onProviderEnabled");
    }
    
    public void onStatusChanged(String str, int index, Bundle bundle) {
        Log.d("WeatherService", "weather:onStatusChanged");
    }

    private class GetWeatherInfoAsyncTask extends AsyncTask<Location, Void, WeatherInfo>
    {

        protected WeatherInfo doInBackground(Location[] args) {
            Location location = args[0];
            WeatherInfo weatherInfo = null;
            String countyName = null;
            try {
                Geocoder geocoder = new Geocoder(WeatherService.this);
                Log.d("WeatherService", "weather:in");
                List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                Address address = (Address)addresses.get(0);
                Log.d("WeatherService", "weather:get");
                countyName = address.getSubAdminArea();
                if(TextUtils.isEmpty(countyName)) {
                    countyName = address.getLocality();
                    if(TextUtils.isEmpty(countyName)) {
                        countyName = address.getAdminArea();
                    }
                }
            } catch(IOException e) {
                e.printStackTrace();
            } finally {
                WeatherService.this.mLocationManager.removeUpdates(WeatherService.this);
            }
            if(TextUtils.isEmpty(countyName)) {
                Log.d("WeatherService", "weather:get address info failed,can not continue");
                return null;
            }
            try {
                Log.d("WeatherService", "weather:get address info success,begin get weather info");
                String getJsonUrl = String.format(Locale.US, "https://query.yahooapis.com/v1/public/yql?q=%stext=\"%s\")&format=json", new Object[] {URLEncoder.encode("select * from weather.forecast where woeid in (select woeid from geo.places(1) where ", "UTF-8"), countyName});
                Log.d("WeatherService", "weather:" + getJsonUrl);
                String json = HttpUtil.getJsonByUrl(getJsonUrl);
                Log.d("WeatherService", "weather" + json);
                if(!TextUtils.isEmpty(json)) {
                    weatherInfo = HttpUtil.getWeatherInfoByJson(json, countyName);
                }
                Log.d("WeatherService", "weather:" + mWeatherInfo);
            } catch(UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            return weatherInfo;
        }

        protected void onPostExecute(WeatherInfo result) {
            if(result != null) {
                String lastRefreshDate = mPFormat.format(new Date());
                result.setLastRefreshDate(lastRefreshDate);
                Log.d("WeatherService", "weather:get weather info success");
                DataSave.saveWeatherInfo(WeatherService.this, result);
                WeatherService.this.onWeatherLoaded(result, 0);
            } else {
                Log.d("WeatherService", "weather:get weather info failed");
                WeatherService.this.onWeatherLoaded(result, 1);
            }
        }
    }

    public class MyBinder extends Binder
    {
        public MyBinder()
        {
        }

        public WeatherService getService()
        {
            return WeatherService.this;
        }
    }

    public static abstract interface WeatherLoadListener
    {
        public abstract void onWeatherInfoChanged(int paramInt, WeatherInfo paramWeatherInfo);
    }
}
