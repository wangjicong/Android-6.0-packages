/**
 * Copyright (C) 2010,2013 Thundersoft Corporation
 * All rights Reserved
 */

package com.ucamera.ucomm.sns;

import java.util.Locale;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore.Images.Media;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;

/*
 * get the text description from google's api according to gps info
 *
 *  1. get gps from photo
 *  2. if no gps got, get it from GsmCellLocation
 *  3. if no gps got, get it from LocationProvider of network
 *  4. if no gps got, get it from LocationProvider of gps
 *  5. use google's api to get text description for gps.
 */
public class LocationHelper {
    final String TAG = "TextLocation";
    // err code to return
    public final int ERR_NO_NETWORK = 1;
    public final int ERR_NO_LOCATIONPROVIDER = 2;
    public final int ERR_RESULT_OF_GOOGLEAPI = 3;

    // caller's context
    private Context mContext;

    // location manager and listener
    private LocationListener mLL;
    private LocationManager mLM;

    // use to getLastKnownLocation before listener got update
    private Location mLastLocation;
    // use to get gps from Gsm
    private TelephonyManager tm;

    // the result values
    String mLatitude;
    String mLongitude;
    String mTextLocation;

    // the mLatitude and mLongitude is up to time
    boolean mHasGps;

    // use to callback the result or error
    TextLocationCallBack mListener;

    // for sequence of actions
    private Handler mHandler;
    final int MSG_REQUEST_UPDATE_NETWORK = 1;
    final int MSG_REQUEST_UPDATE_GPS = 2;
    final int MSG_UNREGISTER_LM = 3;
    final int MSG_GET_LOCATION_FROM_GPS = 4;
    final int MSG_CALLBACK_RESULT = 5;
    final int MSG_CALLBACK_ERROR = 6;

    // caller should pass TextLocationCallBack in
    public interface TextLocationCallBack{
        public void obtainLocation(String textString, String latitude, String longitude);
        public void opError(int errCode);
    }

    // constructor
    public LocationHelper(Context context){
        mContext = context;
        mHasGps = false;
        if (mHandler == null) {
            mHandler = new MyWorkHandler();
        }
    }

    /*
     * pass params to obtain the text location
     * @param Uri photo: use the exif gps info of this photo
     * @param TextLocationCallBack listener: callback for result
     */
    public void getTextLocation(Uri photo, TextLocationCallBack listener){
        mListener = listener;
        if (photo != null){
            mHasGps = getGpsForPhoto(photo);
        }

        if (!mHasGps){
            getGps();
        }
        else {
            mHandler.sendEmptyMessage(MSG_GET_LOCATION_FROM_GPS);
        }
    }

    // get gps info from GsmCellLocation
    private void getGpsForCell(){
        new Thread(){
            public void run(){
                try{
                    GsmCellLocation gsmcelllocation = (GsmCellLocation)tm.getCellLocation();
                    int cellid = gsmcelllocation.getCid();
                    int gsmloccode = gsmcelllocation.getLac();
                    int j = Integer.valueOf(tm.getNetworkOperator().substring(0, 3)).intValue();
                    int l = Integer.valueOf(tm.getNetworkOperator().substring(3, 5)).intValue();
                    JSONObject jsonobject = new JSONObject();
                    jsonobject.put("version", "1.1.0");
                    jsonobject.put("host", "maps.google.com");
                    jsonobject.put("request_address", true);
                    JSONArray jsonarray = new JSONArray();
                    JSONObject jsonobject1 = new JSONObject();
                    jsonobject1.put("cell_id", cellid);
                    jsonobject1.put("location_area_code", gsmloccode);
                    jsonobject1.put("mobile_country_code", j);
                    jsonobject1.put("mobile_network_code", l);
                    jsonarray.put(jsonobject1);
                    jsonobject.put("cell_towers", jsonarray);
                    HttpPost httppost = new HttpPost("http://www.google.com/loc/json");
                    httppost.setEntity(new StringEntity(jsonobject.toString()));

                    jsonobject = new JSONObject(EntityUtils.toString((new DefaultHttpClient()).execute(httppost).getEntity()));

                    mLatitude = jsonobject.getJSONObject("location").getString("latitude");
                    mLongitude = jsonobject.getJSONObject("location").getString("longitude");
                    mHasGps = true;
                }
                catch (Exception e){
                    Log.d(TAG, "getGpsForCell from cell occures error!");
                    e.printStackTrace();
                }

                if (!mHasGps){
                    getGpsFornormal();
                }
                else{
                    mHandler.sendEmptyMessage(MSG_GET_LOCATION_FROM_GPS);
                }
            }
        }.start();
    }

    // get gps info from LOCATION_SERVICE
    private void getGpsFornormal(){
        mLM = (LocationManager)mContext.getSystemService(Context.LOCATION_SERVICE);
        mLL = new LocationListener() {
            public void onLocationChanged(Location location){
                if(location != null && !mHasGps){
                    mLastLocation = location;
                    mHasGps = true;
                    mLatitude = String.valueOf(mLastLocation.getLatitude());
                    mLongitude = String.valueOf(mLastLocation.getLongitude());
                    mHandler.sendEmptyMessage(MSG_GET_LOCATION_FROM_GPS);
                }
            }

            public void onProviderDisabled(String s){
            }

            public void onProviderEnabled(String s){
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }
        };
        mHandler.sendEmptyMessage(MSG_REQUEST_UPDATE_NETWORK);
    }

    // use google's api translate the gps info to text
    // the request is like this:
    // the api reference is : http://code.google.com/intl/zh-CN/apis/maps/documentation/geocoding/
    // http://maps.googleapis.com/maps/api/geocode/json?latlng=39.583734,116.330748&sensor=true&language=zh-cn
    private void getLocation() {
        new Thread(){
            public void run(){
                if (!hasNet()){
                    mHandler.sendMessage(mHandler.obtainMessage(MSG_CALLBACK_ERROR, ERR_NO_NETWORK, 0,null));
                    return;
                }

                try {
                    StringBuilder httpUrl = new StringBuilder("http://maps.googleapis.com/maps/api/geocode/json?");
                    httpUrl.append("latlng=").append(mLatitude).append(",").append(mLongitude);
                    httpUrl.append("&sensor=true");
                    String language = getLanguage();
                    if (language != null) {
                        httpUrl.append("&language=").append(language);
                    }
                    HttpGet httpGet = new HttpGet(httpUrl.toString());
                    BasicHttpParams basichttpparams = new BasicHttpParams();
                    HttpConnectionParams.setConnectionTimeout(basichttpparams, 40000);
                    HttpConnectionParams.setSoTimeout(basichttpparams, 40000);
                    httpGet.setParams(basichttpparams);
                    JSONObject jsonobject = new JSONObject(EntityUtils.toString((new DefaultHttpClient()).execute((org.apache.http.client.methods.HttpUriRequest)httpGet).getEntity()));

                    if ("OK".equals(jsonobject.getString("status"))){
                        mTextLocation = ((JSONObject)(jsonobject.getJSONArray("results").get(0))).getString("formatted_address");
                        mHandler.sendEmptyMessage(MSG_CALLBACK_RESULT);
                    }
                    else{
                        mHandler.sendMessage(mHandler.obtainMessage(MSG_CALLBACK_ERROR, ERR_RESULT_OF_GOOGLEAPI, 0,null));
                    }

                }catch(Exception e){
                    Log.e(TAG,"get text location failed");
                    e.printStackTrace();
                    mHandler.sendMessage(mHandler.obtainMessage(MSG_CALLBACK_ERROR, ERR_RESULT_OF_GOOGLEAPI, 0,null));
                }
            }
        }.start();
    }

    // get language spec to construct the request uri to google api
    private String getLanguage(){
        Locale locale = Locale.getDefault();
        if (locale.equals(Locale.CHINA) || locale.equals(Locale.CHINESE)){
            return "zh-CN";
        }
        else if (locale.equals(Locale.TAIWAN) || locale.equals(Locale.TRADITIONAL_CHINESE)){
            return "zh-TW";
        }
        else if (locale.equals(Locale.JAPAN) || locale.equals(Locale.JAPANESE)){
            return "ja";
        }
        return null;
    }

    // get gps from Gsm, if not then get from LOCATION_SERVICE
    private void getGps(){
        tm = (TelephonyManager)mContext.getSystemService(Context.TELEPHONY_SERVICE);
        android.telephony.CellLocation celllocation = tm.getCellLocation();
        if(tm.getSimState() != TelephonyManager.SIM_STATE_READY
            || !(celllocation instanceof GsmCellLocation)
            || !hasNet()){
            getGpsFornormal();
        }
        else{
            getGpsForCell();
        }
    }

    // part copy from ImageEditControlActivity
    // get gps info from photo's exif
    private boolean getGpsForPhoto(Uri imageUri){
        if (imageUri == null) {
            return false;
        }

        String scheme = imageUri.getScheme();
        String imagePath = null;
        if("content".equals(scheme)) {
            imagePath = getDefaultPathAccordUri(imageUri);
        } else if("file".equals(scheme)) {
            imagePath = imageUri.getPath();
        }
        if (imagePath == null) {
            return false;
        }

        try {
            ExifInterface exifInterface = new ExifInterface(imagePath);
            float[] output = new float[2];
            if (exifInterface.getLatLong(output)){
                mLatitude = String.valueOf(output[0]);
                mLongitude = String.valueOf(output[1]);
                return true;
            }
        } catch(Exception e) {
            Log.e(TAG, "getGpsForPhoto(): exifInterface parser " + imagePath + " exception.");
        }
        return false;
    }

    // copy from ImageEditControlActivity
    // get path from uri
    private String getDefaultPathAccordUri(Uri uri) {
        String strPath = null;
        final String[] IMAGE_PROJECTION = new String[] {Media.DATA};
        Cursor cr = mContext.getContentResolver().query(uri, IMAGE_PROJECTION, null, null, null);
        if(cr != null && cr.getCount() > 0) {
            if(cr.isBeforeFirst()) {
                cr.moveToFirst();
                strPath = cr.getString(cr.getColumnIndex(Media.DATA));
            }
            cr.close();
        }

        return strPath;
    }

    // the handler
    private class MyWorkHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            int delay;
            switch (msg.what) {

            case MSG_REQUEST_UPDATE_NETWORK:
                delay = 1;
                if(mLM.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
                    Location location = mLM.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    if (location != null){
                        mLastLocation = location;
                        mLatitude = String.valueOf(mLastLocation.getLatitude());
                        mLongitude = String.valueOf(mLastLocation.getLongitude());
                        mHandler.sendEmptyMessage(MSG_GET_LOCATION_FROM_GPS);
                    }

                    mLM.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mLL);
                    delay = 10000;
                }
                sendEmptyMessageDelayed(MSG_REQUEST_UPDATE_GPS, delay);
                break;

            case MSG_REQUEST_UPDATE_GPS:
                mLM.removeUpdates(mLL);
                delay = 1;

                if(!mHasGps){
                    if(mLM.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                        Location location = mLM.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        if (location != null){
                            mLastLocation = location;
                            mLatitude = String.valueOf(mLastLocation.getLatitude());
                            mLongitude = String.valueOf(mLastLocation.getLongitude());
                            mHandler.sendEmptyMessage(MSG_GET_LOCATION_FROM_GPS);
                        }
                        mLM.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLL);
                        delay = 10000;
                    }
                }
                sendEmptyMessageDelayed(MSG_UNREGISTER_LM, delay);
                break;

            case MSG_UNREGISTER_LM:
                if(mLM != null){
                    mLM.removeUpdates(mLL);
                }
                if(!mHasGps && mLastLocation == null && mListener != null){
                    mListener.opError(ERR_NO_LOCATIONPROVIDER);
                }
                break;
            case MSG_GET_LOCATION_FROM_GPS:
                getLocation();
                break;
            case MSG_CALLBACK_RESULT:
                if (mListener != null){
                    mListener.obtainLocation(mTextLocation, mLatitude, mLongitude);
                }
                break;
            case MSG_CALLBACK_ERROR:
                if (mListener != null){
                    mListener.opError(msg.arg1);
                }
                break;
            default:
                break;
            }
        }
    }

    // tool to get net
    public boolean hasNet(){
        NetworkInfo networkinfo = ((ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if(networkinfo != null && networkinfo.isAvailable()){
            return true;
        }
        return false;
    }
}
